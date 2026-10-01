package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Regular steam → KU engine. Uses springSmall + plateDouble in recipes. */
public record SteamEngineSpec(
        String id, GTMaterial material, int efficiency,
        int steamCapacity, long outputRate,
        float hardness, float blastResistance
) implements SteamEngineData {
    @Override public int tintRgb() { return material.getColor(); }
    @Override public String materialName() { return material.getLocalName(); }
    public EngineType type() { return EngineType.STEAM; }
}
