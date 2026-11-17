package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** EU → KU engine. {@code inputRate} is EU/t, power scales as 4× per tier. */
public record ElectricEngineSpec(
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
        return EngineType.ELECTRIC;
    }
}
