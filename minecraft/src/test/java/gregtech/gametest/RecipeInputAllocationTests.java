package gregtech.gametest;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
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
}
