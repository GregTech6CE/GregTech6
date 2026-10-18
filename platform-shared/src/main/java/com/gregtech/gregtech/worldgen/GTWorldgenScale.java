package com.gregtech.gregtech.worldgen;

/**
 * Y-coordinate remapping for the 1.18+ extended world depth.
 *
 * <p>GT6's worldgen tables assume a 1.7.10 world with bedrock at y=0; modern worlds
 * bottom out at y=-64. All table Y values below the old band top (112) are stretched
 * proportionally so the old [0,112] generation band covers [-64,112]: the surface
 * geology stays put while deep ores/strata extend into the deepslate range.
 * Values at or above 112 are left untouched (continuous at the seam).</p>
 */
public final class GTWorldgenScale {
    /** Top of the scaled band — old and new coordinates agree here and above. */
    public static final int BAND_TOP = 112;
    /** New world floor. */
    public static final int WORLD_FLOOR = -64;

    private GTWorldgenScale() {}

    /** Remaps a GT6-era Y value (band [0,112]) into the extended band [-64,112]. */
    public static int remapY(int oldY) {
        if (oldY >= BAND_TOP) return oldY;
        return Math.round(oldY * (float) (BAND_TOP - WORLD_FLOOR) / BAND_TOP) + WORLD_FLOOR;
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
