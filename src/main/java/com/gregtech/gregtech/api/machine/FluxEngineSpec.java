package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** RF → KU engine. {@code inputRate} is RF/t, using RF_PER_EU=4 convention. */
public record FluxEngineSpec(
        String id, GTMaterial material, int tier,
        long inputRate, long outputRate,
        float hardness, float blastResistance
) {
    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }
    public static String textureFolder() { return "kinetic_flux"; }
    public EngineType type() { return EngineType.FLUX; }
}
