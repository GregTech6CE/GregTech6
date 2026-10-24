package gregtech.gametest;

import gregtech.internal.fluid.FluidSlotAmountOverlay;
import gregtech.client.gui.SlotFluid;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Native slot/stack contract only: does not instantiate a screen or claim GPU rendering. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FluidSlotAmountOverlayTests {
    @GameTest(template = "test_empty")
    public static void inputSlotBucketOverlayKeepsExactLiters(GameTestHelper helper) {
        verify(helper, true);
    }

    @GameTest(template = "test_empty")
    public static void outputSlotBucketOverlayKeepsExactLiters(GameTestHelper helper) {
        verify(helper, false);
    }

    private static void verify(GameTestHelper helper, boolean input) {
        int[] amounts = {1, 999, 1000, 1999, 2000, 8000, 8999, 126999, 127000, 1000000, Integer.MAX_VALUE};
        {
            SlotFluid slot = new SlotFluid(new ItemStackHandler(1), 0, 8, 20, 0, input);
            for (int amount : amounts) {
                FluidStack supplied = new FluidStack(Fluids.WATER, amount);
                slot.setFluid(supplied);
                helper.assertTrue(slot.fluid() != supplied, "slot owns a copy");
                String text = FluidSlotAmountOverlay.text(slot.fluid().getAmount());
                int count = Math.max(1, Math.min(127, amount / 1000));
                helper.assertTrue(text.equals(count == 1 ? "" : Integer.toString(count)), "native stack label");
                helper.assertTrue(slot.fluid().getAmount() == amount && supplied.getAmount() == amount,
                        "overlay leaves exact tooltip/storage liters unchanged");
                helper.assertTrue(slot.fluid().getFluid() == Fluids.WATER && slot.getItem().isEmpty(),
                        "does not fabricate or mutate backing item stacks");
                helper.assertTrue(!slot.mayPlace(ItemStack.EMPTY) && slot.isInput() == input, "slot contract unchanged");
                supplied.setAmount(2);
                helper.assertTrue(slot.fluid().getAmount() == amount, "source stack mutation is isolated");
            }
            slot.setFluid(null);
            helper.assertTrue(slot.fluid().isEmpty(), "null still clears slot");
        }
        helper.succeed();
    }
}
