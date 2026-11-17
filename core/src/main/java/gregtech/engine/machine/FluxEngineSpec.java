package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** RF → KU engine. {@code inputRate} is RF/t, using RF_PER_EU=4 convention. */
public record FluxEngineSpec(
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
        return EngineType.FLUX;
    }
}
