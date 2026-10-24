package gregtech.content.material;

import gregtech.data.AntimatterMaterials;
import gregtech.data.MaterialGroups;
import gregtech.data.ImportedMaterialData;
import gregtech.data.generated.GT6Materials;
import gregtech.data.generated.MaterialModTags;
import gregtech.data.generated.MaterialRegistryExtras;
import gregtech.data.generated.MaterialCompositionData;

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
        VanillaMatterMaterials.declare();
    }

    public static void link() {
        GT6Materials.applyOreProcessing();
        MaterialModTags.apply();
        MaterialRegistryExtras.apply();
        MaterialCompositionData.apply();
        gregtech.data.generated.MaterialDecompositionData.apply();
        gregtech.data.generated.MaterialDataFacts.apply();
        MaterialFormCorrections.apply();
        MaterialGroups.init();

    }
}
