package gregtech.engine.fluid;

/** Compact slot labels; one stored unit is one millibucket, or one legacy GT liter. */
public final class FluidSlotAmountOverlay {
    private FluidSlotAmountOverlay() {}

    public static String text(long liters) {
        return text(liters, true);
    }

    public static String text(long liters, boolean millibuckets) {
        if (liters <= 0) return "";
        if (liters < 10_000) return liters + (millibuckets ? "mB" : "L");
        long divisor = 1_000;
        String[] units = millibuckets
                ? new String[] {"B", "kB", "MB", "GB", "TB", "PB", "EB"}
                : new String[] {"KL", "ML", "GL", "TL", "PL", "EL"};
        int unit = 0;
        while (unit < units.length - 1 && divisor <= Long.MAX_VALUE / 1_000 && liters / divisor >= 1_000) {
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
