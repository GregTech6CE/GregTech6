package gregtech.content.recipe;

import gregtech.api.addon.recipe.MachineRecipeDocuments;
import gregtech.api.material.GTValues;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.CraftingItemCompositions;
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
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
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

//? if >=1.21.1 {
/*    @GameTest(template = "test_empty", batch = "material_reload", timeoutTicks = 200)
    public static void machineDatapackPriorityRequiresAnExactLong(GameTestHelper helper) {
        String json = """
                {"type":"gt6ce:machine","map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"item":"minecraft:gravel"}],
                 "eu_per_tick":16,"duration":20,"priority":%s}
                """;
        var ops = helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var codec = net.minecraft.world.item.crafting.Recipe.CODEC;
        var recipe = codec.parse(ops, com.google.gson.JsonParser.parseString(json.formatted("9223372036854775807.0")))
                .getOrThrow();
        helper.assertTrue(recipe instanceof MachineDatapackRecipe machine
                        && machine.addition().priority() == Long.MAX_VALUE,
                "The largest signed priority is preserved through the recipe codec");
        for (String invalid : List.of("9007199254740992.5", "-9223372036854775809")) {
            var result = codec.parse(ops, com.google.gson.JsonParser.parseString(json.formatted(invalid)));
            helper.assertTrue(result.error().map(error -> error.message().contains("priority")).orElse(false),
                    "The recipe codec rejects invalid priority " + invalid);
        }
        helper.succeed();
    }

*///?}
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
        var mold = item("extruder_shape_plate");
        var moldBeforeFailure = ItemMaterialRegistry.base(mold).orElseThrow(() -> new IllegalStateException("extruder shape composition is missing"));
        try {
            var failed = MachineRecipeOverlay.apply(null, server.registryAccess());
            helper.assertTrue(failed.failed(), "invalid overlay fails through the real recovery path");
            helper.assertTrue(ItemMaterialForms.aliases() == tagAliases, "failed overlay restores tagged form aliases");
            helper.assertTrue(ItemMaterialRegistry.base(mold).orElseThrow().equals(moldBeforeFailure),
                    "failed overlay restores recipe-derived compositions");
            helper.assertTrue(MaterialPublication.current() == current, "failed overlay preserves the prior stamp");
            helper.assertTrue(MachineRecipeMaps.Bath.recipeRows.size() == MachineRecipeMaps.Bath.baselineSize(),
                    "failure restores the Bath baseline instead of a partial overlay");
        } finally {
            var restored = MachineRecipeOverlay.apply(server.getRecipeManager(), server.registryAccess());
            if (restored.failed()) throw new IllegalStateException("could not restore overlay after failure probe");
            MachineRecipeOverlay.publishFurnace(server.getRecipeManager(), server.registryAccess());
            MaterialPublication.commit(MachineRecipeOverlay.lastPublication());
        }
        helper.assertTrue(SharedSpecialProcessRows.owned().size() == owned, "successful overlay restores owned rows");
        assertFurnaceMarkers(helper);
        assertCraftingCompositions(helper);
        assertProductionCompositions(helper);
        helper.succeed();
    }

    private static void assertCraftingCompositions(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var manager = server.getRecipeManager();
        var access = server.registryAccess();
        assertComposition(helper, "press_bronze", "Bronze", GTValues.U * 27 / 2);
        assertComposition(helper, "extruder_shape_plate", "TungstenCarbide", GTValues.U * 2);
        assertComposition(helper, "low_heat_extruder_shape_plate", "Steel", GTValues.U * 2);
        assertComposition(helper, "press_bullet_casing_shape_small", "Steel", GTValues.U * 5 + 2 * GTValues.U9);
        var shredder = craftingRecovery(item("low_heat_extruder_shape_plate")).orElseThrow();
        helper.assertFalse(shredder.isCatalystInput(0), "shredding consumes a production mold");

        var blank = item("extruder_shape_empty");
        var shaped = item("extruder_shape_plate");
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", "extruder_shapes/extruder_shape_empty");
        var output = new ItemStack(blank, 2);
        var inputs = NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.IRON_INGOT));
        var filtered = new ArrayList<>(manager.getRecipes());
//? if >=1.21.1 {
        /*var working = new RecipeManager(access);
        filtered.removeIf(recipe -> recipe.id().equals(id));
        var replacement = new net.minecraft.world.item.crafting.RecipeHolder<>(id,
                new ShapelessRecipe("", CraftingBookCategory.MISC, output, inputs));
*///?} else {
        var working = new RecipeManager();
        filtered.removeIf(recipe -> recipe.getId().equals(id));
        var replacement = new ShapelessRecipe(id, "", CraftingBookCategory.MISC, output, inputs);
//?}
        try {
            working.replaceRecipes(filtered);
            CraftingItemCompositions.rebuild(working, access);
            VanillaRecoveryRecipes.rebuildCraftingRows();
            helper.assertTrue(ItemMaterialRegistry.base(blank).isEmpty()
                    && ItemMaterialRegistry.base(shaped).isEmpty(), "removed recipe invalidates both mold compositions");
            helper.assertTrue(craftingRecovery(shaped).isEmpty(), "removed recipe withdraws the old shredder row");
            filtered.add(replacement);
            working.replaceRecipes(filtered);
            CraftingItemCompositions.rebuild(working, access);
            VanillaRecoveryRecipes.rebuildCraftingRows();
            assertComposition(helper, "extruder_shape_empty", "Iron", GTValues.U2);
            assertComposition(helper, "extruder_shape_plate", "Iron", GTValues.U2);
            var updated = craftingRecovery(shaped).orElseThrow();
            long recovered = 0;
            for (var dust : updated.itemOutputs) {
                var composition = ItemMaterialRegistry.get(dust).orElseThrow();
                helper.assertTrue(composition.material().resolve() == GTMaterialRegistry.get("Iron").resolve(),
                        "updated shredder output is iron");
                recovered += composition.amount() * dust.getCount();
            }
            helper.assertTrue(recovered == GTValues.U2, "updated shredder output follows the divided material amount");
        } finally {
            CraftingItemCompositions.rebuild(manager, access);
            VanillaRecoveryRecipes.rebuildCraftingRows();
        }
    }

    private static java.util.Optional<gregtech.engine.recipe.Recipe> craftingRecovery(Item item) {
        return VanillaRecoveryRecipes.craftingRows().stream()
                .filter(row -> row.itemInputs[0].is(item))
                .findFirst();
    }

    private static void assertComposition(GameTestHelper helper, String path, String material, long amount) {
        var stack = new ItemStack(item(path));
        var data = ItemMaterialRegistry.get(stack).orElseThrow();
        helper.assertTrue(data.components().size() == 1
                && data.material().resolve() == GTMaterialRegistry.get(material).resolve()
                && data.amount() == amount, path + " has its expected crafting composition: " + data.components());
        helper.assertTrue(ItemMaterialRegistry.canMelt(stack), path + " is admitted to crucible recovery");
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
    }

    private static void assertProductionCompositions(GameTestHelper helper) {
        var cable = new ItemStack(item("cable_04_copper"));
        assertAmount(helper, cable, "Copper", 2 * GTValues.U);
        assertAmount(helper, cable, "Rubber", 2 * GTValues.U);
        assertAmount(helper, new ItemStack(item("cable_01_redalloy")), "Rubber", GTValues.U);
        assertAmount(helper, new ItemStack(item("clay_plate_mold")), "Clay", 5 * GTValues.U);
        assertAmount(helper, new ItemStack(item("fluid_measuring_pot")), "Ceramic", 4 * GTValues.U);
        assertAmount(helper, new ItemStack(item("circuit_plate")), "SiliconDioxide", GTValues.U);
        assertAmount(helper, new ItemStack(item("crystal_circuit_diamond")), "Diamond", GTValues.U);
        assertAmount(helper, new ItemStack(item("crystal_circuit_emerald")), "Emerald", GTValues.U);
        assertAmount(helper, new ItemStack(item("flux_laser_lv")), "Lead", 8 * GTValues.U);
        helper.assertTrue(ItemMaterialRegistry.base(item("flux_laser_lv")).orElseThrow().components().size() > 1,
                "the flux laser includes its CO2 laser");
        for (String path : List.of("aneutronic_fusion_power_cell_helium_3", "universal_suit_helmet",
                "usb3_hdd", "portable_scanner", "robot_arm_wrench_tip", "long_dist_pipe_tungsten")) {
            helper.assertTrue(ItemMaterialRegistry.base(item(path)).isPresent(), path + " has derived composition");
        }
        var spray = new ItemStack(item("spray_paint_red_2"));
        helper.assertTrue(ItemMaterialRegistry.get(spray).orElseThrow().components().equals(
                ItemMaterialRegistry.base(item("empty_spray_can")).orElseThrow().components()),
                "spray variants retain only the empty can's composition");
        var tape = gregtech.content.item.behavior.BehaviorDuctTape.consumable(1);
        var used = tape.prepared(tape.full());
        assertAmount(helper, tape.full(), "Plastic", 3 * GTValues.U / 4);
        assertAmount(helper, tape.spent(used, tape.uses() / 2), "Plastic", 3 * GTValues.U / 8);
        helper.assertFalse(ItemMaterialRegistry.canRecover(tape.used()), "a spent tape roll has no recoverable material");
        assertTapeRepack(helper, tape);
        assertLaminatorReplacement(helper, cable);
        assertFilterCoverCompositions(helper);
    }

    /**
     * The filter covers keep the composition of their real crafting recipes: their 1×1 self-loop reset
     * recipes must not replace it with an empty one.
     */
    private static void assertFilterCoverCompositions(GameTestHelper helper) {
        for (String path : List.of("item_filter", "fluid_filter", "item_retriever_cover")) {
            var composition = ItemMaterialRegistry.get(new ItemStack(item(path)));
            helper.assertTrue(
                    composition.isPresent() && !composition.get().components().isEmpty(),
                    path + " lost its crafting composition to its reset recipe");
        }
    }

    private static void assertAmount(GameTestHelper helper, ItemStack stack, String material, long expected) {
        var data = ItemMaterialRegistry.get(stack).orElseThrow(() -> new IllegalStateException("missing composition for " + stack));
        long amount = data.components().stream()
                .filter(component -> component.material() == GTMaterialRegistry.get(material).resolve())
                .mapToLong(component -> component.amount()).sum();
        helper.assertTrue(amount == expected, stack + " has " + expected + " units of " + material + ", got " + amount);
    }

    private static void assertTapeRepack(GameTestHelper helper,
            gregtech.content.item.behavior.ItemBehaviors.Consumable tape) {
        var level = helper.getLevel();
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", "catalog_repairs/duct_tape_repack");
        var entry = level.getRecipeManager().byKey(id).orElseThrow();
        var used = tape.spent(tape.prepared(tape.full()), tape.uses() * 7 / 8);
//? if >=1.21.1 {
        /*var recipe = (TapeRepackRecipe) entry.value();
        var grid = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, java.util.Collections.nCopies(4, used));
*///?} else {
        var recipe = (TapeRepackRecipe) entry;
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                return ItemStack.EMPTY;
            }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 2, 2);
        for (int slot = 0; slot < 4; slot++) grid.setItem(slot, used.copy());
//?}
        helper.assertTrue(recipe.matches(grid, level), "four used rolls can be repacked");
        var result = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(tape.isUsed(result) && tape.remaining(result) == tape.uses() / 2,
                "repacking four eighth-rolls returns a half-used roll");
        assertAmount(helper, result, "Plastic", 3 * GTValues.U / 8);
    }

    private static void assertLaminatorReplacement(GameTestHelper helper, ItemStack cable) {
        var server = helper.getLevel().getServer();
        var manager = server.getRecipeManager();
        var access = server.registryAccess();
        String map = MachineRecipeMaps.Laminator.internalName;
        var removal = MachineRecipeDocuments.removal(Map.of(
                "map", map, "item_outputs", List.of(Map.of("item", "gt6ce:cable_04_copper"))));
        var addition = MachineRecipeDocuments.addition(Map.of(
                "map", map,
                "item_inputs", List.of(Map.of("item", "gt6ce:wire_04_copper"),
                        Map.of("item", "gt6ce:plate_rubber", "count", 3)),
                "item_outputs", List.of(Map.of("item", "gt6ce:cable_04_copper")),
                "eu_per_tick", 16, "duration", 32));
        var addId = ResourceLocation.fromNamespaceAndPath("gt6ce", "test/cable_add");
        var removeId = ResourceLocation.fromNamespaceAndPath("gt6ce", "test/cable_remove");
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
        try {
            working.replaceRecipes(recipes);
            var overlay = MachineRecipeOverlay.apply(working, access);
            helper.assertTrue(!overlay.failed() && overlay.removed() > 0 && overlay.added() == 1,
                    "the cable's laminator route was replaced");
            assertAmount(helper, cable, "Copper", 2 * GTValues.U);
            assertAmount(helper, cable, "Rubber", 3 * GTValues.U);
        } finally {
            var restored = MachineRecipeOverlay.apply(manager, access);
            if (restored.failed()) throw new IllegalStateException("could not restore cable overlay");
            MachineRecipeOverlay.publishFurnace(manager, access);
            MaterialPublication.commit(MachineRecipeOverlay.lastPublication());
        }
        assertAmount(helper, cable, "Rubber", 2 * GTValues.U);
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
