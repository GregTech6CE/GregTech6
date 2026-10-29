package gregtech.engine.fluid;

/** Compact slot labels; tooltips retain exact liters. */
public final class FluidSlotAmountOverlay {
    private FluidSlotAmountOverlay() {}

    public static String text(long liters) {
        if (liters <= 0) return "";
        if (liters < 10_000) return liters + "L";
        long divisor = 1_000;
        String[] units = {"KL", "ML", "BL", "TL", "QL", "EL"};
        int unit = 0;
        while (unit < units.length - 1 && liters / divisor >= 1_000) {
            divisor *= 1_000;
            unit++;
        }
        long whole = liters / divisor;
        long tenth = (liters % divisor) / (divisor / 10);
        return whole + "." + tenth + units[unit];
    }

    /** Left-aligned even when the text is scaled to fit the slot. */
    public static int x(int slotX, int textWidth) {
        return slotX;
    }

    public static int y(int slotY) {
        return slotY + 9;
    }
}
