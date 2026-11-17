package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** Registration and presentation data shared by the native engine families. */
public sealed interface EngineSpec
        permits DieselEngineSpec, ElectricEngineSpec, FluxEngineSpec, RotationEngineSpec, SteamEngineData {
    String id();

    GTMaterial material();

    EngineType type();

    long outputRate();

    float hardness();

    float blastResistance();

    default int tintRgb() {
        return material().getColor();
    }

    default String materialName() {
        return material().getLocalName();
    }
}
