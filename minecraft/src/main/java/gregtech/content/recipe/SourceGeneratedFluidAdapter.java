package gregtech.content.recipe;

import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProperties;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.content.material.MaterialFactKeys;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.PublishedMaterialFacts;

/** Native fingerprint checks for generated fluids declared by material facts. */
public final class SourceGeneratedFluidAdapter {
    private SourceGeneratedFluidAdapter() {}

    public static boolean mercury(FluidCatalog.FluidEntry entry) {
        return checked(GTMaterialRegistry.get("Mercury"), entry, false);
    }

    public static boolean wholeDustMetal(GTMaterial material, FluidCatalog.FluidEntry entry) {
        return checked(material, entry, true);
    }

    private static boolean checked(GTMaterial material, FluidCatalog.FluidEntry entry, boolean wholeDust) {
        if (material == null || !material.isValid() || entry == null) return false;
        material = material.resolve();
        if (!GTMaterialRegistry.isCoreMaterial(material) || AddonMaterials.owns(material)) return false;
        var rule = MaterialFactKeys.value(material, MaterialFactKeys.SOURCE_GENERATED_FLUID)
                .flatMap(rules -> rules.rules().stream()
                        .filter(value -> value.wholeDustMetal() == wholeDust)
                        .findFirst())
                .orElse(null);
        if (rule == null
                || !FluidCatalog.canonicalField(rule.field()).equals(rule.field())
                || !entry.registryName().equals(rule.registryPath())
                || !material.getName().equals(entry.materialKey())
                || entry.temperature() != rule.temperature()
                || entry.density() != 0
                || entry.glint()
                || entry.gas()) return false;
        if (wholeDust) {
            var target = PublishedMaterialFacts.targetMaterial(material, MaterialProcessKeys.SMELTING);
            if (target == null
                    || target.resolve() != material
                    || PublishedMaterialFacts.target(material, MaterialProcessKeys.SMELTING)
                                    .amount()
                            != gregtech.api.material.GTValues.U
                    || PublishedMaterialFacts.scalar(material, MaterialProperties.MELTING_POINT) != rule.temperature()
                    || PublishedMaterialFacts.scalar(material, MaterialProperties.DENSITY) != rule.materialDensity()
                    || !material.has(MaterialProperty.MELTING)
                    || material.has(MaterialProperty.ANTIMATTER)
                    || !FluidCatalog.isGeneratedMoltenDefinition(rule.field(), entry)
                    || entry.textureMode() != FluidCatalog.FluidTextureMode.GENERIC_MOLTEN
                    || !entry.tintTexture()
                    || entry.viscosity() != 0
                    || entry.luminosity() != 10
                    || entry.flags() != FluidCatalog.FluidFlags.LIQUID) return false;
        } else if (!FluidCatalog.isGeneratedGasLiquidDefinition(rule.field(), entry)
                || entry.textureMode() != FluidCatalog.FluidTextureMode.DEDICATED
                || entry.tintArgb() != 0
                || !entry.tintTexture()
                || entry.viscosity() != 0
                || entry.luminosity() != 0
                || entry.flags() != FluidCatalog.FluidFlags.LIQUID) return false;
        return true;
    }
}
