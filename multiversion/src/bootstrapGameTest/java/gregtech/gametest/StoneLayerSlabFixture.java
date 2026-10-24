package gregtech.gametest;

import gregtech.content.worldgen.GTCellNoise;
import gregtech.content.worldgen.GTStoneLayersGen;

/** Test-only logical sample; deliberately independent of GameTest's randomized native position. */
final class StoneLayerSlabFixture {
    static final int X = 1, Y = -59, Z = 1;
    // Actual GTCellNoise/LAYERS: seed 0 -> layer 72 (LIMESTONE); 126 -> 106 (no cobble target).
    // The seed changes cell jitter, not the final cell hash. Some coordinates can NEVER
    // select both branches, so scanning more world seeds is not a robust fixture strategy.
    static final long TARGET_SEED = 0, NO_TARGET_SEED = 126;

    private StoneLayerSlabFixture() {}

    static GTStoneLayersGen.LayerDef definition(long seed) {
        return GTStoneLayersGen.LAYERS.get(new GTCellNoise(seed).get(X, Y, Z, GTStoneLayersGen.LAYERS.size()));
    }
}
