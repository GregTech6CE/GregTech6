package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Strong steam → KU engine (4× values of regular). Uses spring + plateDense in recipes. */
public record StrongSteamEngineSpec(
        String id, GTMaterial material, int efficiency,
        int steamCapacity, long outputRate,
        float hardness, float blastResistance
) implements SteamEngineData {
    @Override public int tintRgb() { return material.getColor(); }
    @Override public String materialName() { return material.getLocalName(); }
    public EngineType type() { return EngineType.STEAM; }
}
