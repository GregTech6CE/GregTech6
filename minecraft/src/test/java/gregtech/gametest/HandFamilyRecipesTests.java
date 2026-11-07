package gregtech.gametest;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Material-unit rows of the hand-written machining families resolve from one offered stack. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class HandFamilyRecipesTests {
    private static final String BATCH = "recipe_map_registration";

    private HandFamilyRecipesTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void sharpeningRemainsKeepTheirOriginalChance(GameTestHelper helper) {
        // Vibranium carries both GEMS and STICKS, so the gem -> stick grinding row exists with
        // a pulverized remainder (U in, U/2 out).
        ItemStack gem = stack(MaterialPrefix.gem, "Vibranium", 1);
        Recipe row = find(MachineRecipeMaps.Sharpening, List.of(gem));
        helper.assertTrue(row != null, "a gem grinds into a stick");
        helper.assertTrue(row.mOutputs.length == 2, "the row keeps its pulverized remainder");
        helper.assertTrue(row.getOutputChance(1) == 7500, "GT6 chances(10000, 7500): remainder is 75%");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void crusherLadderResolvesFromTheFormStack(GameTestHelper helper) {
        ItemStack gem = stack(MaterialPrefix.gem, "Diamond", 1);
        Recipe row = find(MachineRecipeMaps.Crusher, List.of(gem));
        helper.assertTrue(row != null, "diamond gem is crushed by a dynamic material-unit rule");
        helper.assertTrue(row.mOutputs.length == 1 && row.mOutputs[0].getCount() == 2,
                "one gem produces two flawed gems");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void autoclaveCountedBatchResolves(GameTestHelper helper) {
        // Source row Loader_Recipes_Handlers:441: dustSmall 32 + ST.tag(5) -> gemLegendary,
        // steam 819200 scaled by toolQuality+1.
        GTMaterial quartz = GTMaterialRegistry.get("CertusQuartz").resolve();
        ItemStack tiny = stack(MaterialPrefix.dustSmall, "CertusQuartz", 32);
        ItemStack tag = new ItemStack(gregtech.content.registry.GTTechnological.selectorTag(5));
        int expectedSteam = Math.multiplyExact(819200, quartz.getToolQuality() + 1);
        FluidStack steam = GTFluids.stack("Steam", expectedSteam);
        helper.assertTrue(steam != null && !steam.isEmpty(), "steam is registered");
        Recipe row = MachineRecipeMaps.Autoclave.findRecipe(
                List.of(tiny, tag), List.of(steam), false,
                MachineRecipeMaps.Autoclave.mInputItemsCount, MachineRecipeMaps.Autoclave.mOutputItemsCount);
        helper.assertTrue(row != null,
                "32 small quartz dusts with circuit 5 grow a legendary gem");
        helper.assertTrue(row.mInputs[0].getCount() == 32,
                "the counted row consumes the declared 32-item batch");
        helper.assertTrue(hasFluidInput(row, steam),
                "steam is scaled by toolQuality+1 (RecipeMapHandlerPrefix:218)");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void welderCasingRowExists(GameTestHelper helper) {
        ItemStack casing = stack(MaterialPrefix.itemCasing, "Steel", 2);
        ItemStack tag = new ItemStack(gregtech.content.registry.GTTechnological.selectorTag(2));
        Recipe row = find(MachineRecipeMaps.Welder, List.of(casing, tag));
        helper.assertTrue(row != null, "two steel casings weld back into a plate");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void formConversionRebuildsTheCountedBatch(GameTestHelper helper) {
        ItemStack nuggets = stack(MaterialPrefix.nugget, "Iron", 9);
        ItemStack tag = new ItemStack(gregtech.content.registry.GTTechnological.selectorTag(9));
        Recipe row = find(MachineRecipeMaps.Boxinator, List.of(nuggets, tag));
        helper.assertTrue(row != null,
                "nine nuggets and circuit 9 pack into an ingot");
        helper.assertTrue(row.mInputs[0].getCount() == 9,
                "the material-unit rule keeps the nine-item batch");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void machineCasingConsumesSixPlates(GameTestHelper helper) {
        ItemStack plates = stack(MaterialPrefix.plate, "Steel", 6);
        ItemStack rods = stack(MaterialPrefix.stickLong, "Steel", 2);
        Recipe row = find(MachineRecipeMaps.Welder, List.of(plates, rods));
        helper.assertTrue(row != null, "six steel plates and two long rods weld a machine casing");
        helper.assertTrue(row.mInputs[0].getCount() == 6, "the casing rule keeps the plate batch");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void pipeWeldConsumesCurvedPlates(GameTestHelper helper) {
        ItemStack curved = stack(MaterialPrefix.plateCurved, "Steel", 3);
        ItemStack tag = new ItemStack(gregtech.content.registry.GTTechnological.selectorTag(3));
        Recipe row = find(MachineRecipeMaps.Welder, List.of(curved, tag));
        helper.assertTrue(row != null, "three curved steel plates weld a medium pipe");
        helper.succeed();
    }

    private static boolean hasFluidInput(Recipe row, FluidStack expected) {
        for (FluidStack input : row.mFluidInputs)
            if (input.getFluid() == expected.getFluid() && input.getAmount() == expected.getAmount())
                return true;
        return false;
    }

    private static Recipe find(RecipeMap map, List<ItemStack> items) {
        return map.findRecipe(items, List.of(), false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static ItemStack stack(MaterialPrefix prefix, String materialName, int count) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return ItemStack.EMPTY;
        return GTItems.getStack(prefix, material.resolve(), count);
    }
}
