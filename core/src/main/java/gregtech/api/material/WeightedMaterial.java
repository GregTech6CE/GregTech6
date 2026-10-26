package gregtech.api.material;

import gregtech.api.material.WeightedMaterial;

/** One material and its GT units in a weighted composition. */
public record WeightedMaterial(GTMaterial material, long amount) {
    public WeightedMaterial {
        material = material == null ? MaterialSentinels.Invalid : material.resolve();
    }
}
