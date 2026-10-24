package com.gregtech.gregtech.api.fluid;

/** Presentation only: the original basic-machine FL.display(tank, true, true) count.
 * Exact liters remain in the tank and tooltip; the icon uses capped whole buckets.
 */
public final class FluidSlotAmountOverlay {
    private FluidSlotAmountOverlay() {}

    public static String text(long liters) {
        long count = Math.max(1, Math.min(127, liters / 1000));
        return count == 1 ? "" : Long.toString(count);
    }

    /** Vanilla item-count right edge, using the active font's measured width. */
    public static int x(int slotX, int textWidth) { return slotX + 17 - textWidth; }
    public static int y(int slotY) { return slotY + 9; }
}
