package gregtech.internal.machine;

import gregtech.internal.material.GTMaterial;

/** Liquid fuel → RU engine. Consumes FM.Engine fuels (Diesel, Fuel, Nitrofuel, etc.). */
public record DieselEngineSpec(
        String id, GTMaterial material, int tier,
        long inputRate, long outputRate,
        float hardness, float blastResistance
) {
    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }
    public static String textureFolder() { return "kinetic_diesel"; }
    public EngineType type() { return EngineType.DIESEL; }
}
