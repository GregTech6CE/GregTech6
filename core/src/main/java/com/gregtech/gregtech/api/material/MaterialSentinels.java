package com.gregtech.gregtech.api.material;

/** Sentinel lookup must not initialize the entire built-in material alias table. */
public final class MaterialSentinels {
    private MaterialSentinels() {}
    public static final GTMaterial Invalid = GTMaterialRegistry.createMaterial(-1, "NULL", "Null", 0xFF00FF)
            .put(MaterialProperty.HIDDEN);
    public static final GTMaterial Empty = GTMaterialRegistry.createMaterial(0, "Empty", "Empty", 0xFFFFFF)
            .setAtomicProperties(AtomicProperties.ZERO)
            .put(MaterialProperty.HIDDEN);
}
