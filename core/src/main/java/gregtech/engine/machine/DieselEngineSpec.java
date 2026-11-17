package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** Liquid fuel → RU engine. Consumes FM.Engine fuels (Diesel, Fuel, Nitrofuel, etc.). */
public record DieselEngineSpec(
        String id,
        GTMaterial material,
        int tier,
        long inputRate,
        long outputRate,
        float hardness,
        float blastResistance)
        implements EngineSpec {
    @Override
    public EngineType type() {
        return EngineType.DIESEL;
    }
}
