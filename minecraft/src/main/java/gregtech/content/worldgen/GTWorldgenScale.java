package gregtech.content.worldgen;

/**
 * Y-coordinate remapping for the 1.18+ extended world depth.
 *
 * <p>GT6 worldgen tables use old world heights. Overworld features use a curve
 * anchored at Y=40 so shallow deposits keep their original height while deeper
 * deposits extend downwards. Other dimensions keep their original heights.</p>
 */
public final class GTWorldgenScale {
    /** Top of the scaled band — old and new coordinates agree here and above. */
    public static final int BAND_TOP = 144;
    /** Lower bound of the remapped generation band, not the world's build floor. */
    public static final int WORLD_FLOOR = -48;
    /** Old heights at and above this value stay at their original Y. */
    public static final int HEIGHT_ANCHOR_Y = 40;

    private GTWorldgenScale() {}

    /** Maps old Overworld heights [0,40] to [-48,40] with a 1.5-power curve. */
    public static int remapY(int oldY) {
        if (oldY >= HEIGHT_ANCHOR_Y) return oldY;
        if (oldY <= 0) return WORLD_FLOOR + oldY;
        double fraction = oldY / (double) HEIGHT_ANCHOR_Y;
        return (int) Math.round(WORLD_FLOOR + (HEIGHT_ANCHOR_Y - WORLD_FLOOR) * Math.pow(fraction, 1.5));
    }

    /**
     * Dimension-aware variant: only the Overworld got the 1.18 depth extension —
     * the Nether (0..128) and the End (0..) keep the original GT6 Y values.
     * Remapping End veins to negative Y was why naquadah veins never generated.
     */
    public static int remapY(net.minecraft.world.level.WorldGenLevel level, int oldY) {
        return level.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD ? remapY(oldY) : oldY;
    }
}
