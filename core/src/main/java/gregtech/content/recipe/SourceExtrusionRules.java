package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.material.MaterialFactKeys;
import gregtech.content.material.catalog.SourceExtrusionMaterialFacts;
import gregtech.content.recipe.generated.GTMaterialFields;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;

/** Native extrusion recipe behavior backed by the material's source profile. */
public final class SourceExtrusionRules {
    private SourceExtrusionRules() {}

    public static SourceExtrusionMaterialFacts.Profile profile(GTMaterial material) {
        if (material == null || !material.isValid() || material.resolve() != material || AddonMaterials.owns(material))
            return null;
        if (GTMaterialRegistry.get(material.getId()) != material
                || GTMaterialRegistry.get(material.getName()) != material) return null;
        return MaterialFactKeys.value(material, MaterialFactKeys.SOURCE_EXTRUSION)
                .orElse(null);
    }

    public static boolean smithable(GTMaterial material) {
        var profile = profile(material);
        return profile != null ? profile.smithable() : material.has(MaterialProperty.SMITHABLE);
    }

    public static GTMaterial target(GTMaterial material, SourceExtrusionMaterialFacts.Profile profile) {
        if (profile.target().isEmpty()) return material;
        String name = GTMaterialFields.materialOf(profile.target());
        GTMaterial result = name == null ? null : GTMaterialRegistry.get(name);
        return result == null ? null : result.resolve();
    }

    public static long[] amounts(long inputWeight, long outputWeight) {
        if (inputWeight <= 0 || outputWeight <= 0) return null;
        long input = outputWeight > inputWeight ? (outputWeight + inputWeight - 1) / inputWeight : 1;
        long output = outputWeight > inputWeight ? 1 : inputWeight / outputWeight;
        if (input > 64 || output < 1) return null;
        return new long[] {input, Math.min(128, output), output};
    }

    public static long normalTicks(GTMaterial input, GTMaterial output, long inputUnits) {
        double kilograms = input.getDensity() * 111.111111 * inputUnits / GTValues.U;
        return Math.max(16, 1 + (long) Math.abs((output.getMeltingPoint() - 293d) * kilograms / (75 * 96)));
    }

    public static long simpleTicks(long outputUnits) {
        return Math.max(1, (outputUnits * 64 + GTValues.U - 1) / GTValues.U);
    }
}
