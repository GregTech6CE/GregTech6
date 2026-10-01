package com.gregtech.gregtech.content.material;
import com.gregtech.gregtech.api.material.*;
/** Descriptive matter for modern vanilla items absent from GT6; no fabricated chemistry or generated ore/forms. */
public final class VanillaMatterMaterials {
    private VanillaMatterMaterials() {}
    public static void declare() {
        String[] names = {"Wool", "PlantMatter", "Soil", "Feather", "AnimalShell", "Sculk", "Prismarine", "DyeMatter", "MagicMatter", "Dripstone", "Calcite", "Tuff", "Deepslate"};
        for (int i = 0; i < names.length; i++) if (!GTMaterialRegistry.get(names[i]).isValid())
            MaterialDefinition.builder(7900 + i, names[i]).color(0xC0C0C0).meltingPointKelvin(0).boilingPointKelvin(0).density(0).build().register();
    }
}
