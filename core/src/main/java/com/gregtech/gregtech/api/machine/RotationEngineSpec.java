package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** RU → KU engine. {@code inputRate} is RU/t. Swaps active/inactive textures when running. */
public record RotationEngineSpec(
        String id, GTMaterial material, int tier,
        long inputRate, long outputRate,
        float hardness, float blastResistance
) {
    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }
    public static String textureFolder() { return "kinetic_rotation"; }
    public EngineType type() { return EngineType.ROTATION; }
}
