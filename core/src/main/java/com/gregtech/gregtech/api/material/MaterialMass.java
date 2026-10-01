package com.gregtech.gregtech.api.material;

/** GT6 material mass arithmetic shared by servers and tooltips. */
public final class MaterialMass {
    private MaterialMass() {}
    public static double kilograms(GTMaterial material, long amount) {
        return kilograms(material.getDensity(), amount);
    }
    public static double kilograms(double density, long amount) {
        return density * 111.111111D * amount / GTValues.U;
    }
}
