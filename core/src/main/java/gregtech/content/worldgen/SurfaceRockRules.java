package gregtech.content.worldgen;

import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * GT6 {@code WorldgenStoneLayers}' per-column surface-rock tracking ({@code tLastRock},
 * {@code tLastOre}, {@code tCanPlaceRocks}) and the 1/128 rock placement roll.
 */
public final class SurfaceRockRules {
    /** GT6 {@code aRandom.nextInt(128) == 0}. */
    public static final int CHANCE_DIVISOR = 128;

    /** GT6 initial {@code tLastRock = MT.STONES.Deepslate}. */
    public static final String INITIAL_ROCK = "Deepslate";

    private SurfaceRockRules() {}

    /** Tracks the exposed stone layer and indicator ore while scanning a single column upward. */
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

        /** Bedrock allows a surface rock directly above if exposed. */
        public void bedrock() {
            canPlaceRocks = true;
        }

        /** Replacing stone, cobble, or mossy host updates the column's topmost rock material. */
        public void replaced(String surfaceMaterial) {
            lastRock = surfaceMaterial;
            canPlaceRocks = true;
        }

        /** Placing an ore records its material only when the entry requests surface indicators. */
        public void ore(String material, boolean indicators) {
            if (indicators) lastOre = material;
        }

        /** Air, fluids, or non-solid blocks break column continuity so buried ores cannot leak up. */
        public void reset() {
            lastOre = null;
            canPlaceRocks = false;
        }

        /** Opaque hosts that do not replace stone update placement eligibility without changing rock. */
        public void opaque(boolean suitable) {
            canPlaceRocks = suitable;
        }
    }

    /**
     * Picks the rock's material matching GT6's short-circuit order:
     * {@code nextBoolean() && tLastOre != null ? targetCrushing(tLastOre) : tLastRock}.
     */
    public static String rockMaterial(
            ColumnState state, BooleanSupplier nextBoolean, UnaryOperator<String> targetCrushing) {
        if (nextBoolean.getAsBoolean() && state.lastOre() != null) {
            return targetCrushing.apply(state.lastOre());
        }
        return state.lastRock();
    }

    /** GT6 1/128 surface rock roll. */
    public static boolean roll(IntUnaryOperator nextInt) {
        return nextInt.applyAsInt(CHANCE_DIVISOR) == 0;
    }
}
