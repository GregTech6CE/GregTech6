package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** EU → KU engine. {@code inputRate} is EU/t, power scales as 4× per tier. */
public record ElectricEngineSpec(
        String id, GTMaterial material, int tier,
        long inputRate, long outputRate,
        float hardness, float blastResistance
) {
    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }
    public static String textureFolder() { return "kinetic_electric"; }
    public EngineType type() { return EngineType.ELECTRIC; }
}
