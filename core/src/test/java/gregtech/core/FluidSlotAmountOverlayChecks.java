package gregtech.core;

import gregtech.internal.fluid.FluidSlotAmountOverlay;

/** Pure presentation contracts, not a GPU/client visual test. */
public final class FluidSlotAmountOverlayChecks {
    private static int assertions;
    public static void main(String[] args) {
        long[] amounts = {Long.MIN_VALUE, -1, 0, 1, 999, 1000, 1001, 1999, 2000, 2999,
                7999, 8000, 8999, 9000, 99999, 100000, 126999, 127000, 127999, 128000,
                999999, 1000000, 1000000000, Integer.MAX_VALUE, Long.MAX_VALUE};
        String[] expected = {"", "", "", "", "", "", "", "", "2", "2", "7", "8", "8", "9",
                "99", "100", "126", "127", "127", "127", "127", "127", "127", "127", "127"};
        for (int i = 0; i < amounts.length; i++)
            check(expected[i].equals(FluidSlotAmountOverlay.text(amounts[i])), "boundary " + amounts[i]);
        for (int bucket = 2; bucket <= 127; bucket++) {
            check(Integer.toString(bucket).equals(FluidSlotAmountOverlay.text(bucket * 1000L)), "bucket start");
            check(Integer.toString(bucket).equals(FluidSlotAmountOverlay.text(bucket * 1000L + 999)), "bucket end");
        }
        // Real text widths are supplied by Font; use variable widths rather than assuming fixed digits.
        for (int slotX : new int[]{-20, 0, 8, 176, 1920})
            for (int width : new int[]{0, 3, 6, 12, 18, 27})
                check(FluidSlotAmountOverlay.x(slotX, width) + width == slotX + 17, "right edge");
        for (int slotY : new int[]{-20, 0, 8, 166, 1080})
            check(FluidSlotAmountOverlay.y(slotY) == slotY + 9, "baseline");
        System.out.println("FluidSlotAmountOverlayChecks passed: " + assertions + " assertions");
    }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
}
