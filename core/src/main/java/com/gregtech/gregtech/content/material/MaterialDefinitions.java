package com.gregtech.gregtech.content.material;

import com.gregtech.gregtech.data.AntimatterMaterials;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.ImportedMaterialData;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.data.generated.MaterialModTags;
import com.gregtech.gregtech.data.generated.MaterialRegistryExtras;
import com.gregtech.gregtech.data.generated.MaterialCompositionData;

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
        MaterialFormCorrections.apply();
        MaterialGroups.init();

    }
}
