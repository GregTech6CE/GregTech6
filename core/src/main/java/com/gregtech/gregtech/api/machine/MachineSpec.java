package com.gregtech.gregtech.api.machine;

/**
 * Static machine parameters (GT6 {@code MultiTileEntityRegistry.add} NBT fields).
 */
public record MachineSpec(
        String id,
        String materialName,
        int tintRgb,
        int efficiency,
        long outputRate,
        String textureSet,
        float hardness,
        float blastResistance
) {
    public MachineSpec {
        if (efficiency < 0 || efficiency > 10000) {
            throw new IllegalArgumentException("efficiency must be 0..10000");
        }
        if (outputRate < 0) {
            throw new IllegalArgumentException("outputRate must be >= 0");
        }
    }

    /** Backward-compatible ctor without block stats. */
    public MachineSpec(String id, String materialName, int tintRgb, int efficiency, long outputRate, String textureSet) {
        this(id, materialName, tintRgb, efficiency, outputRate, textureSet, 4.0F, 4.0F);
    }

    public float efficiencyPercent() {
        return efficiency / 100.0f;
    }
}
