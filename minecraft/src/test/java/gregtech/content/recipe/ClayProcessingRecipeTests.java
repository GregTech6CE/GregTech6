package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.RecipeMap;
import gregtech.platform.Mc;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ClayProcessingRecipeTests {
    private ClayProcessingRecipeTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void clayBallsAndBlocksKeepTheirDustAndBrickRecipes(GameTestHelper helper) {
        assertGrinding(helper, new ItemStack(Items.CLAY_BALL), "Clay", 1);
        assertGrinding(helper, new ItemStack(Items.CLAY), "Clay", 4);
        for (String[] variant : new String[][] {
            {"brown", "ClayBrown"},
            {"red", "ClayRed"},
            {"yellow", "Bentonite"},
            {"blue", "Palygorskite"},
            {"white", "Kaolinite"}
        }) {
            var ball = new ItemStack(Mc.item(Mc.gregtechId(variant[0] + "_clay")));
            var block = new ItemStack(Mc.item(Mc.gregtechId("clay_" + variant[0])));
            assertGrinding(helper, ball, variant[1], 1);
            assertGrinding(helper, block, variant[1], 4);
            var furnace = MachineRecipeMaps.Furnace.findRecipe(List.of(ball), List.of(), true, 1, 1);
            helper.assertTrue(
                    furnace != null
                            && furnace.itemOutputs.length == 1
                            && furnace.itemOutputs[0].is(Items.BRICK)
                            && furnace.itemOutputs[0].getCount() == 1,
                    variant[0] + " clay ball must fire into one brick");
            assertCooking(helper, RecipeType.SMELTING, ball);
            assertCooking(helper, RecipeType.BLASTING, ball);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void clayDustsShareCraftingAndMixerInputs(GameTestHelper helper) {
        var fireBricks = crafting(helper, "survival_repairs/fire_bricks");
        var plantPot = crafting(helper, "survival_repairs/plant_pot");
        var forgeTag = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "dusts/clay"));
        var ownTag = ItemTags.create(Mc.gregtechId("dust/clay"));
        var redstone = dust("Redstone", MaterialPrefix.dust, 1);
        var basalz = dust("Basalz", MaterialPrefix.dustTiny, 1);
        var obsidian = dust("Obsidian", MaterialPrefix.dust, 1);
        var petrotheum = dust("Petrotheum", MaterialPrefix.dust, 2);
        int[] claySlots = {0, 2, 6, 8};
        for (String name : List.of("Clay", "ClayBrown", "ClayRed", "Bentonite", "Palygorskite", "Kaolinite")) {
            var clay = dust(name, MaterialPrefix.dust, 1);
            helper.assertTrue(!clay.isEmpty() && clay.is(forgeTag) && clay.is(ownTag),
                    name + " dust must share ordinary clay crafting tags");
//? if >=1.21.1 {
            /*helper.assertTrue(clay.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "dusts/clay"))),
                    name + " dust must share the NeoForge clay tag");
*///?}
            for (int slot : claySlots)
                helper.assertTrue(fireBricks.getIngredients().get(slot).test(clay),
                        name + " dust must craft fire bricks");
            helper.assertTrue(plantPot.getIngredients().get(5).test(clay),
                    name + " dust must craft a plant pot");
            var mixed = MachineRecipeMaps.Mixer.findRecipe(
                    List.of(redstone, basalz, clay, obsidian), List.of(), true, 4, 1);
            helper.assertTrue(mixed != null && mixed.itemOutputs.length == 1
                            && mixed.itemOutputs[0].is(petrotheum.getItem())
                            && mixed.itemOutputs[0].getCount() == 2,
                    name + " dust must mix into two Petrotheum dust");
            var small = dust(name, MaterialPrefix.dustSmall, 1);
            helper.assertTrue(!small.is(forgeTag) && !small.is(ownTag)
                            && !fireBricks.getIngredients().get(0).test(small),
                    "a small pile of " + name + " must not pay for a whole clay dust");
        }
        var ceramic = dust("Ceramic", MaterialPrefix.dust, 1);
        helper.assertTrue(!ceramic.is(forgeTag) && !fireBricks.getIngredients().get(0).test(ceramic),
                "fired ceramic must not substitute for clay dust");
        helper.succeed();
    }

    private static CraftingRecipe crafting(GameTestHelper helper, String path) {
        var entry = helper.getLevel().getRecipeManager().byKey(Mc.gregtechId(path));
        helper.assertTrue(entry.isPresent(), "missing crafting recipe " + path);
//? if >=1.21.1 {
        /*return (CraftingRecipe) entry.orElseThrow().value();
*///?} else {
        return (CraftingRecipe) entry.orElseThrow();
//?}
    }

    private static ItemStack dust(String material, MaterialPrefix prefix, int count) {
        return GTItems.getStack(prefix, GTMaterialRegistry.get(material), count);
    }

    private static void assertGrinding(GameTestHelper helper, ItemStack input, String material, int count) {
        var expected = GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get(material), count);
        assertGrinding(helper, MachineRecipeMaps.Mortar, input, expected, count);
        assertGrinding(helper, MachineRecipeMaps.Shredder, input, expected, count);
    }

    private static void assertGrinding(
            GameTestHelper helper, RecipeMap map, ItemStack input, ItemStack expected, int count) {
        var recipe = map.findRecipe(List.of(input), List.of(), true, 1, 1);
        helper.assertTrue(
                !input.isEmpty()
                        && !expected.isEmpty()
                        && recipe != null
                        && recipe.itemInputs.length == 1
                        && recipe.itemInputs[0].getCount() == 1
                        && recipe.itemOutputs.length == 1
                        && recipe.itemOutputs[0].is(expected.getItem())
                        && recipe.itemOutputs[0].getCount() == count
                        && recipe.energyPerTick == 16
                        && recipe.durationTicks == 16L * count,
                map.internalName + " must grind " + Mc.itemId(input.getItem()) + " into " + count
                        + " matching clay dust at 16 EU/t in " + 16L * count + " ticks");
    }

    private static void assertCooking(
            GameTestHelper helper, RecipeType<? extends AbstractCookingRecipe> type, ItemStack input) {
        var level = helper.getLevel();
        boolean found = level.getRecipeManager().getAllRecipesFor(type).stream().anyMatch(entry -> {
//? if >=1.21.1 {
            /*var recipe = entry.value();
*///?} else {
            var recipe = entry;
//?}
            var output = recipe.getResultItem(level.registryAccess());
            return recipe.getIngredients().size() == 1
                    && recipe.getIngredients().get(0).test(input)
                    && output.is(Items.BRICK)
                    && output.getCount() == 1;
        });
        helper.assertTrue(found, type + " must publish " + Mc.itemId(input.getItem()) + " -> brick");
    }
}
