package gregtech.content.worldgen;

import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * GT6 {@code WorldgenStoneLayers}' surface-rock state: the per-column {@code tLastRock},
 * {@code tLastOre} and {@code tCanPlaceRocks} values and the 1/128 rock roll.
 *
 * <p>The feature feeds every scanned block through {@link ColumnState}; the rock material is
 * chosen with {@link #rockMaterial} exactly like the original:
 * {@code nextBoolean() && tLastOre != null ? tLastOre.mTargetCrushing.mMaterial : tLastRock}.</p>
 */
public final class SurfaceRockRules {
    /** GT6's {@code aRandom.nextInt(128) == 0}. */
    public static final int CHANCE_DIVISOR = 128;

    /** GT6's initial {@code tLastRock = MT.STONES.Deepslate}. */
    public static final String INITIAL_ROCK = "Deepslate";

    private SurfaceRockRules() {}

    /** Per-column state, mirroring the three locals of {@code WorldgenStoneLayers.generate}. */
    public static final class ColumnState {
        private String lastRock = INITIAL_ROCK;
        private String lastOre;
        private boolean canPlaceRocks;

        public String lastRock() {
            return lastRock;
        }

        public String lastOre() {
            return lastOre;
        }

        public boolean canPlaceRocks() {
            return canPlaceRocks;
        }

        /** GT6's bedrock branch: the next air block may carry a rock. */
        public void bedrock() {
            canPlaceRocks = true;
        }

        /** GT6's stone/cobble/mossy/replaceable branch: the layer's surface material was used. */
        public void replaced(String surfaceMaterial) {
            lastRock = surfaceMaterial;
            canPlaceRocks = true;
        }

        /** GT6's ore branch: remember the material only when the ore generates indicators. */
        public void ore(String material, boolean indicators) {
            if (indicators) lastOre = material;
        }

        /** GT6's air / non-opaque branch: the next column segment starts fresh. */
        public void reset() {
            lastOre = null;
            canPlaceRocks = false;
        }

        /** GT6's opaque non-ground branch: only suitable ground keeps the rock flag. */
        public void opaque(boolean suitable) {
            canPlaceRocks = suitable;
        }
    }

    /**
     * GT6's rock material choice. {@code nextBoolean} is called unconditionally, matching the
     * original's short-circuit order.
     */
    public static String rockMaterial(
            ColumnState state, BooleanSupplier nextBoolean, UnaryOperator<String> targetCrushing) {
        if (nextBoolean.getAsBoolean() && state.lastOre() != null) {
            return targetCrushing.apply(state.lastOre());
        }
        return state.lastRock();
    }

    /** GT6's 1/128 roll. */
    public static boolean roll(IntUnaryOperator nextInt) {
        return nextInt.applyAsInt(CHANCE_DIVISOR) == 0;
    }
}
