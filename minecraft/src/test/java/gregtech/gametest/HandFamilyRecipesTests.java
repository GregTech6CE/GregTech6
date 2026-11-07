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
    public static void autoclaveConsumesTheDeclaredFormCount(GameTestHelper helper) {
        // NetherQuartz is CRYSTALLISABLE and carries the whole gem ladder.
        GTMaterial quartz = GTMaterialRegistry.get("NetherQuartz").resolve();
        ItemStack tiny = stack(MaterialPrefix.dustSmall, "NetherQuartz", 32);
        FluidStack steam = GTFluids.stack("Steam", Math.multiplyExact(819200, quartz.getToolQuality() + 1));
        helper.assertTrue(steam != null && !steam.isEmpty(), "steam is registered");
        Recipe row = MachineRecipeMaps.Autoclave.findRecipe(
                List.of(tiny), List.of(steam), false,
                MachineRecipeMaps.Autoclave.mInputItemsCount, MachineRecipeMaps.Autoclave.mOutputItemsCount);
        helper.assertTrue(row != null, "32 small quartz dusts grow a legendary gem");
        helper.assertTrue(row.mInputs[0].getCount() == 32,
                "the rule rebuilds the batch count from material units, not from one representative item");
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

    private static Recipe find(RecipeMap map, List<ItemStack> items) {
        return map.findRecipe(items, List.of(), false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static ItemStack stack(MaterialPrefix prefix, String materialName, int count) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return ItemStack.EMPTY;
        return GTItems.getStack(prefix, material.resolve(), count);
    }
}
