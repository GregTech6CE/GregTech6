package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Static hopper/queuehopper parameters (GT6 {@code MultiTileEntityRegistry.add} NBT fields). */
public record HopperSpec(
        String id,
        GTMaterial material,
        int slotCount,
        float hardness,
        float blastResistance
) {
    public HopperSpec {
        if (slotCount < 1) throw new IllegalArgumentException("slotCount must be >= 1");
    }

    public int tintRgb() {
        return material.getColor();
    }

    public String materialName() {
        return material.getLocalName();
    }

    public static HopperSpec of(String id, GTMaterial material, int slotCount,
                                 float hardness, float blastResistance) {
        return new HopperSpec(id, material, slotCount, hardness, blastResistance);
    }
}
