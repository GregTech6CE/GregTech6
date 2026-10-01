package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.content.material.Materials;

/** One component in a composite {@link GTMaterial} (GT6 {@code OreDictMaterialStack}). */
public record MaterialComponent(GTMaterial material, long amount) {
    public MaterialComponent {
        material = material == null ? MaterialSentinels.Invalid : material.resolve();
    }

    public static MaterialComponent of(GTMaterial material, long amount) {
        return new MaterialComponent(material, amount);
    }
}
