package gregtech.gametest;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** {@link RecipeInputs} assigns overlapping item inputs by availability, not by slot order. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class RecipeInputAllocationTests {
    private static final String BATCH = "recipe_input_allocation";

    private RecipeInputAllocationTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void aDrainedSpecificSlotIsReassigned(GameTestHelper helper) {
        var single = new ItemStack(Items.IRON_INGOT, 1);
        var doubled = new ItemStack(Items.IRON_INGOT, 2);
        // The first requirement accepts both slots; the guarded second one only the single stack.
        var recipe = new Recipe(
                        new ItemStack[] {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)},
                        new ItemStack[] {new ItemStack(Items.GOLD_INGOT)},
                        null, null, null, null, 20, 16, 0)
                .withItemInputGuard(1, stack -> stack.getCount() == 1);
        var remaining = RecipeInputs.consume(recipe, List.of(single, doubled), List.of(), 1);
        helper.assertTrue(remaining != null, "the guarded input must keep the slot only it accepts");
        helper.assertTrue(
                remaining.items().get(1).getCount() == 1, "the broad input must spend the doubled stack");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void aReassignedItemDemandStillChecksTheFluidInput(GameTestHelper helper) {
        var single = new ItemStack(Items.IRON_INGOT, 1);
        var doubled = new ItemStack(Items.IRON_INGOT, 2);
        // Same drained-slot shape as above, now with a consumable fluid input.
        var recipe = new Recipe(
                        new ItemStack[] {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)},
                        new ItemStack[] {new ItemStack(Items.GOLD_INGOT)},
                        null, null, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 20, 16, 0)
                .withItemInputGuard(1, stack -> stack.getCount() == 1);
        helper.assertTrue(
                RecipeInputs.consume(recipe, List.of(single, doubled), List.of(), 1) == null,
                "the item re-plan must not let a recipe match without its fluid input");
        var remaining =
                RecipeInputs.consume(recipe, List.of(single, doubled), List.of(new FluidStack(Fluids.WATER, 1000)), 1);
        helper.assertTrue(remaining != null, "the rescued item demand must match when the fluid is present");
        helper.assertTrue(
                remaining.fluids().get(0).getAmount() == 0,
                "the item re-plan must not leave the fluid input unspent");
        helper.succeed();
    }
}
