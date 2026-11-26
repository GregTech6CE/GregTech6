package gregtech.content.worldgen;

import java.util.function.UnaryOperator;

/**
 * GT6 {@code WorldgenStoneLayers}' per-column surface-rock tracking ({@code tLastRock},
 * {@code tLastOre}, {@code tCanPlaceRocks}) and the 1/128 rock placement roll.
 */
public final class SurfaceRockRules {
    /** GT6 {@code aRandom.nextInt(128) == 0}. */
    public static final int CHANCE_DIVISOR = 128;

    private static final long CHANCE_SALT = 0x5B4F78FE36C9201DL;
    private static final long MATERIAL_SALT = 0x96A0CD92487B3EF5L;

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

        /** Starts a new rock trace after a gap in the terrain. */
        public void gap() {
            reset();
            lastRock = "Stone";
        }

        /** Opaque hosts that do not replace stone update placement eligibility without changing rock. */
        public void opaque(boolean suitable) {
            canPlaceRocks = suitable;
        }
    }

    public record Pebble(String material, boolean indicator) {
        public boolean shouldPlace(boolean surfaceRocks) {
            return indicator || surfaceRocks;
        }
    }

    /** On half the eligible positions, the rock reports the last exposed ore. */
    public static Pebble pebble(ColumnState state, long positionSeed, UnaryOperator<String> targetCrushing) {
        if (state.lastOre() != null && (WorldgenHash.mix(positionSeed ^ MATERIAL_SALT) & 1L) == 0) {
            return new Pebble(targetCrushing.apply(state.lastOre()), true);
        }
        return new Pebble(state.lastRock(), false);
    }

    /** GT6 1/128 surface rock roll. */
    public static boolean roll(long positionSeed) {
        return WorldgenHash.roll(WorldgenHash.mix(positionSeed ^ CHANCE_SALT), CHANCE_DIVISOR) == 0;
    }
}
