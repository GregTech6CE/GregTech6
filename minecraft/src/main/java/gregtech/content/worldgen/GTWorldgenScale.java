package gregtech.content.worldgen;

/**
 * Y-coordinate remapping for the 1.18+ extended world depth.
 *
 * <p>GT6 worldgen tables use old world heights. Overworld features use a curve
 * anchored at Y=40 so shallow deposits keep their original height while deeper
 * deposits extend downwards. Other dimensions keep their original heights.</p>
 */
public final class GTWorldgenScale {
    private GTWorldgenScale() {}

    /** Maps old Overworld heights [0,40] to [-48,40] with a 1.5-power curve. */
    public static int remapY(int oldY) {
        return MineralWorldgenRules.overworldHeight(oldY, MineralWorldgenRules.LEGACY_ORE_FLOOR);
    }

    /**
     * Dimension-aware variant: only the Overworld got the 1.18 depth extension —
     * the Nether (0..128) and the End (0..) keep the original GT6 Y values.
     */
    public static int remapY(net.minecraft.world.level.WorldGenLevel level, int oldY) {
        return level.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD ? remapY(oldY) : oldY;
    }
}
