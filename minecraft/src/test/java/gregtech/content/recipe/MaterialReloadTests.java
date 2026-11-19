package gregtech.content.recipe;

import gregtech.api.addon.recipe.MachineRecipeDocuments;
import gregtech.api.material.GTValues;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.generated.WoodMaterials;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialForm;
import gregtech.engine.material.ItemMaterialForms;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialMass;
import gregtech.engine.material.MaterialPublication;
import gregtech.engine.sensor.PhysicalItemMass;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MaterialReloadTests {
    private MaterialReloadTests() {}

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void bedrockFlowerHasItsDeclaredPhysicalMass(GameTestHelper helper) {
        var sagebrush = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "flower_sagebrush"));
        var composition = ItemMaterialRegistry.base(sagebrush).orElseThrow();
        helper.assertTrue(composition.components().size() == 1, "sagebrush has one material component");
        var component = composition.components().get(0);
        helper.assertTrue(
                component.material() == WoodMaterials.Acacia && component.amount() == GTValues.U,
                "sagebrush contains one unit of Acacia");
        double expected = 2 * MaterialMass.kilograms(WoodMaterials.Acacia, GTValues.U);
        helper.assertTrue(
                expected > 0 && PhysicalItemMass.kilograms(new ItemStack(sagebrush, 2)) == expected,
                "the weight sensor uses the declared material for each flower");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_reload", timeoutTicks = 6000)
    public static void twoRealReloadsPreserveOwnedRecipes(GameTestHelper helper) {
        assertAddonProcessTags(helper);
        var server = helper.getLevel().getServer();
        var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        int applications = MachineRecipeOverlay.applications();
        int owned = SharedSpecialProcessRows.owned().size();
        int bath = MachineRecipeMaps.Bath.recipeRows.size();
        var previous = MaterialPublication.current();
        var nativeForms = ItemMaterialForms.nativeAliases();
        var done = new AtomicBoolean();
        var failure = new AtomicReference<Throwable>();
        server.reloadResources(packs).thenCompose(ignored -> server.reloadResources(packs))
                .whenComplete((ignored, error) -> { failure.set(error); done.set(true); });
        await(helper, applications, owned, bath, previous, nativeForms, done, failure);
    }

    private static void await(GameTestHelper helper, int applications, int owned, int bath,
            MaterialPublication.Stamp previous, Map<ItemMaterialForm, List<Item>> nativeForms, AtomicBoolean done, AtomicReference<Throwable> failure) {
        if (!done.get()) {
            helper.runAfterDelay(1, () -> await(helper, applications, owned, bath, previous, nativeForms, done, failure));
            return;
        }
        if (failure.get() != null) { helper.fail("resource reload failed: " + failure.get()); return; }
        assertAddonProcessTags(helper);
        helper.assertTrue(ItemMaterialForms.nativeAliases() == nativeForms, "reloads reuse the native form index");
        helper.assertTrue(MachineRecipeOverlay.applications() >= applications + 2, "both real overlays applied");
        helper.assertTrue(SharedSpecialProcessRows.owned().size() == owned, "special-process rows do not accumulate");
        helper.assertTrue(MachineRecipeMaps.Bath.recipeRows.size() == bath, "Bath recipe count remains stable");
        var current = MaterialPublication.current();
        helper.assertTrue(current.revision() > previous.revision(), "successful reload advances publication");
        helper.assertTrue(current.recipeRevision() == MachineRecipeOverlay.generation(), "stamp names the committed overlay");
        helper.assertTrue(current.compositionRevision() == ItemMaterialRegistry.compositionRevision(),
                "stamp names the composition used by the overlay");
        var server = helper.getLevel().getServer();
        var tagAliases = ItemMaterialForms.aliases();
        try {
            var failed = MachineRecipeOverlay.apply(null);
            helper.assertTrue(failed.failed(), "invalid overlay fails through the real recovery path");
            helper.assertTrue(ItemMaterialForms.aliases() == tagAliases, "failed overlay restores tagged form aliases");
            helper.assertTrue(MaterialPublication.current() == current, "failed overlay preserves the prior stamp");
            helper.assertTrue(MachineRecipeMaps.Bath.recipeRows.size() == MachineRecipeMaps.Bath.baselineSize(),
                    "failure restores the Bath baseline instead of a partial overlay");
        } finally {
            var restored = MachineRecipeOverlay.apply(server.getRecipeManager());
            if (restored.failed()) throw new IllegalStateException("could not restore overlay after failure probe");
            MachineRecipeOverlay.publishFurnace(server.getRecipeManager(), server.registryAccess());
            MaterialPublication.commit(MachineRecipeOverlay.lastPublication());
        }
        helper.assertTrue(SharedSpecialProcessRows.owned().size() == owned, "successful overlay restores owned rows");
        assertFurnaceMarkers(helper);
        helper.succeed();
    }

    private static void assertFurnaceMarkers(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var manager = server.getRecipeManager();
        var access = server.registryAccess();
        var map = MachineRecipeMaps.Furnace;
        var removal = MachineRecipeDocuments.removal(Map.of(
                "map", map.internalName,
                "item_inputs", List.of(Map.of("item", "minecraft:red_sand")),
                "item_outputs", List.of(Map.of("item", "minecraft:glass"))));
        var addition = MachineRecipeDocuments.addition(Map.of(
                "map", map.internalName,
                "item_inputs", List.of(Map.of("item", "minecraft:red_sand")),
                "item_outputs", List.of(Map.of("item", "minecraft:gold_nugget")),
                "eu_per_tick", 16, "duration", 32));
        var addId = ResourceLocation.fromNamespaceAndPath("gt6ce", "test/furnace_add");
        var removeId = ResourceLocation.fromNamespaceAndPath("gt6ce", "test/furnace_remove");
        var recipes = new ArrayList<>(manager.getRecipes());
//? if >=1.21.1 {
        /*recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(addId, MachineDatapackRecipe.addition(addition)));
        recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(removeId, MachineDatapackRecipe.removal(removal)));
        var working = new RecipeManager(access);
*///?} else {
        recipes.add(MachineDatapackRecipe.addition(addId, addition));
        recipes.add(MachineDatapackRecipe.removal(removeId, removal));
        var working = new RecipeManager();
//?}
        helper.assertTrue(redSandOutputs(Items.GLASS) > 0, "the vanilla glass input is imported into the GT furnace");
        try {
            for (int pass = 0; pass < 2; pass++) {
                working.replaceRecipes(recipes);
                MachineRecipeOverlay.publishFurnace(working, access);
                helper.assertTrue(redSandOutputs(Items.GLASS) == 0, "the removal sees the imported vanilla glass row");
                helper.assertTrue(redSandOutputs(Items.GOLD_NUGGET) == 1, "one datapack replacement survives publication");
            }
        } finally {
            working.replaceRecipes(manager.getRecipes());
            MachineRecipeOverlay.publishFurnace(working, access);
        }
        helper.assertTrue(redSandOutputs(Items.GLASS) > 0, "the vanilla mirror returns without the marker");
        helper.assertTrue(redSandOutputs(Items.GOLD_NUGGET) == 0, "the old marker addition is gone");
    }

    private static long redSandOutputs(net.minecraft.world.item.Item output) {
        return MachineRecipeMaps.Furnace.recipeRows.stream().filter(row -> row.enabled && !row.fakeRecipe
                && row.itemInputs.length == 1 && row.itemInputs[0].is(Items.RED_SAND)
                && row.itemOutputs.length == 1 && row.itemOutputs[0].is(output)).count();
    }

    private static void assertAddonProcessTags(GameTestHelper helper) {
        var target = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("gt6ce_form_test", "alias_probe/target"));
        var reagentId = ResourceLocation.fromNamespaceAndPath("gt6ce_form_test", "alias_probe/reagent");
        var reagent = TagKey.create(Registries.FLUID, reagentId);
        var gold = GTMaterialRegistry.get("Gold");
        helper.assertTrue(GTItems.getStack(MaterialPrefix.crushedPurified, gold).is(target),
                "the addon namespace publishes the declared purified ore target");
        helper.assertFalse(GTItems.getStack(MaterialPrefix.crushedPurifiedTiny, gold).is(target),
                "the addon target tag excludes forms absent from the rule");
        helper.assertTrue(Fluids.LAVA.defaultFluidState().is(reagent),
                "the liquid reagent tag resolves the registered vanilla lava");
        helper.assertFalse(Fluids.FLOWING_LAVA.defaultFluidState().is(reagent),
                "the reagent tag uses the published lava registry identity");
        helper.assertTrue(!BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, reagentId))
                        .iterator().hasNext(),
                "a fluid-only reagent does not produce an item tag");
    }
}
