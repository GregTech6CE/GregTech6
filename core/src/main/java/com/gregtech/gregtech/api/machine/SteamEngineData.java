package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Shared interface for {@link SteamEngineSpec} and {@link StrongSteamEngineSpec}. */
public interface SteamEngineData {
    String id();
    GTMaterial material();
    int efficiency();
    int steamCapacity();
    long outputRate();
    float hardness();
    float blastResistance();

    default int tintRgb() { return material().getColor(); }
    default String materialName() { return material().getLocalName(); }
    default String textureFolder() { return "kinetic_steam"; }

    /** Steam consumed per operation (mB). */
    default int steamPerOp() { return 200; }
    /** Distilled water produced per operation (mB). */
    default int waterPerOp() { return 1; }
}
