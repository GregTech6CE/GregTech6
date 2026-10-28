package gregtech.content.material;

import gregtech.content.data.AntimatterMaterials;
import gregtech.content.data.ImportedMaterialData;
import gregtech.content.data.MaterialGroups;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.data.generated.MaterialCompositionData;
import gregtech.content.data.generated.MaterialModTags;
import gregtech.content.data.generated.MaterialRegistryExtras;

/** Ordered built-in material definitions and linking passes. Called once by GTMaterialRegistry.init(). */
public final class MaterialDefinitions {
    private MaterialDefinitions() {}

    public static void declare() {
        // Force nested holders to load, mirroring MT.init() in GT6.
        ImportedMaterialData.NULL.getClass();
        ImportedMaterialData.DATA.DYE_MATERIALS[0].getClass();
        ImportedMaterialData.Ma.getClass();
        ParticleMaterials.declare();
        AntimatterMaterials.register();
        GT6Materials.touchAll();
        SupplementalMaterials.declare();
    }

    public static void link() {
        GT6Materials.applyOreProcessing();
        MaterialModTags.apply();
        MaterialRegistryExtras.apply();
        MaterialCompositionData.apply();
        gregtech.content.data.generated.MaterialDecompositionData.apply();
        gregtech.content.data.generated.MaterialDataFacts.apply();
        MaterialFormCorrections.apply();
        SourceMaterialToolStats.apply();
        SupplementalMaterials.link();
        MaterialGroups.init();
        SourceLatheMaterialProfiles.captureDefaults();
    }
}
