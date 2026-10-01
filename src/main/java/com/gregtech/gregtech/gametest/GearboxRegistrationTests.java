package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.loaders.Loader_HandToolCraftingRecipes;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTGearboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original GT6 kinetic() grade table and its two craftable gearbox families. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class GearboxRegistrationTests {
    @GameTest(template = "test_empty")
    public static void originalGearboxAndTransformerGradesAreRegistered(GameTestHelper helper) {
        GTMaterial[] materials = {WoodMaterials.WoodTreated, Materials.Bronze, Materials.Brass,
                Materials.ArsenicCopper, Materials.ArsenicBronze, Materials.Steel,
                Materials.Titanium, Materials.Tungstensteel, Materials.Iridium,
                Materials.TitaniumIridium, Materials.Trinitanium,
                Materials.Trinaquadalloy, Materials.Adamantium};
        long[] gearboxSpeed = {16, 64, 64, 64, 64, 256, 1024, 4096, 16384,
                65536, 262144, 1048576, 4194304};
        long[] transformerInput = {8, 32, 32, 32, 32, 128, 512, 2048, 8192,
                32768, 131072, 524288, 2097152};
        helper.assertTrue(GTGearboxes.allGearboxes().size() == 13
                        && GTGearboxes.allTransformers().size() == 13,
                "all original 13 custom gearboxes and 13 transformer gearboxes exist");
        for (int tier = 0; tier < materials.length; tier++) {
            GTMaterial material = materials[tier];
            var gearbox = GTGearboxes.allGearboxes().stream().map(entry -> entry.get())
                    .filter(block -> block.spec().material() == material).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing custom gearbox " + material.getName()));
            var transformer = GTGearboxes.allTransformers().stream().map(entry -> entry.get())
                    .filter(block -> block.spec().material() == material).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing transformer gearbox " + material.getName()));
            helper.assertTrue(gearbox.spec().maxSpeed() == gearboxSpeed[tier],
                    "GT6 gearbox RU speed rating: " + material.getName());
            helper.assertTrue(transformer.spec().inputRate() == transformerInput[tier]
                            && transformer.spec().outputRate() == transformerInput[tier] / 4,
                    "GT6 transformer 1:4 RU speed/power rating: " + material.getName());
            helper.assertTrue(gearbox.asItem().getDefaultInstance().getMaxStackSize() == 16
                            && transformer.asItem().getDefaultInstance().getMaxStackSize() == 16,
                    "original stack limit for " + material.getName());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void everyOriginalGearboxHasResolvedSurvivalRecipe(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        long generated = Loader_HandToolCraftingRecipes.registeredIds().stream()
                .filter(id -> id.startsWith("gregtech:hand/gearbox/")
                        || id.startsWith("gregtech:hand/rotation_transformer/"))
                .count();
        helper.assertTrue(generated == 26, "all 26 original gearbox patterns are generated");
        for (var entry : GTGearboxes.allGearboxes())
            checkRecipe(helper, manager, "gearbox/" + entry.get().spec().id(), entry.get().asItem());
        for (var entry : GTGearboxes.allTransformers())
            checkRecipe(helper, manager, "rotation_transformer/" + entry.get().spec().id(), entry.get().asItem());
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void woodenGearboxUsesOriginalSawAndTreatedWoodParts(GameTestHelper helper) {
        var id = GregTech.id("hand/gearbox/gearbox_wood");
        var recipe = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        var saw = GTToolItem.create(GTToolType.SAW, Materials.Steel, GTMaterialRegistry.get("Wood"));
        var softHammer = GTToolItem.create(GTToolType.SOFT_HAMMER, Materials.Steel,
                GTMaterialRegistry.get("Wood"));
        helper.assertTrue(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
                        && shaped.getWidth() == 3 && shaped.getHeight() == 3
                        && shaped.getIngredients().get(1).test(saw)
                        && !shaped.getIngredients().get(1).test(softHammer),
                "GT6 Wooden Custom Gearbox PsP uses a saw, not a soft hammer");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void everyGradeCreatesItsRegisteredBlockEntity(GameTestHelper helper) {
        BlockPos place = new BlockPos(1, 1, 1);
        for (var entry : GTGearboxes.allGearboxes()) {
            helper.setBlock(place, entry.get());
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(place))
                            instanceof com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity,
                    "real gearbox block entity: " + entry.getId());
        }
        for (var entry : GTGearboxes.allTransformers()) {
            helper.setBlock(place, entry.get());
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(place))
                            instanceof com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity,
                    "real transformer block entity: " + entry.getId());
        }
        helper.succeed();
    }

    private static void checkRecipe(GameTestHelper helper,
                                    net.minecraft.world.item.crafting.RecipeManager manager,
                                    String path, Item expected) {
        var id = GregTech.id("hand/" + path);
        var recipe = manager.byKey(id).orElseThrow(() -> new IllegalStateException("Missing " + id));
        helper.assertTrue(recipe instanceof CraftingRecipe
                        && recipe.getResultItem(helper.getLevel().registryAccess()).is(expected),
                "crafts the matching gearbox: " + id);
        for (var ingredient : recipe.getIngredients()) if (!ingredient.isEmpty())
            helper.assertTrue(ingredient.getItems().length > 0,
                    "obtainable recipe ingredient: " + id);
    }
}
