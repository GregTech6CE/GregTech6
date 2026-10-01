package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialPrefix;

/** Shared original material icon selection and resource paths, without client classes. */
public final class MaterialIconDefinitions {
    private MaterialIconDefinitions() {}

    public static MaterialTextureSet resolveTextureSet(GTMaterial material) {
        MaterialTextureSet set = material.getTextureSet();
        if (set != null) return set;
        if (material.has(MaterialProperty.WOOD)) return MaterialTextureSet.WOOD;
        if (material.has(MaterialProperty.STONE)) return MaterialTextureSet.STONE;
        if (material.has(MaterialProperty.GEM)) return MaterialTextureSet.RUBY;
        if (material.has(MaterialProperty.ORE)) return MaterialTextureSet.STONE;
        if (material.has(MaterialProperty.DUST)) return MaterialTextureSet.FINE;
        if (material.has(MaterialProperty.METAL)) return MaterialTextureSet.METALLIC;
        return MaterialTextureSet.DULL;
    }

    public static String texturePath(MaterialTextureSet set, MaterialPrefix prefix, boolean overlay) {
        return "item/material_icons/" + set.folder() + "/" + prefix.getTextureFileName()
                + (overlay ? "_overlay" : "");
    }

    public static String modelPath(MaterialTextureSet set, MaterialPrefix prefix) {
        return "item/material/" + set.folder() + "/" + prefix.getTextureFileName();
    }

    public static String modelJson(GTMaterial material, MaterialPrefix prefix) {
        return generatedModelJson(resolveTextureSet(material), prefix);
    }

    public static String sharedModelJson(MaterialTextureSet set, MaterialPrefix prefix) {
        if (prefix == MaterialPrefix.coin) return "{\"parent\":\"gregtech:item/coin_minted\"}";
        return generatedModelJson(set, prefix);
    }

    private static String generatedModelJson(MaterialTextureSet set, MaterialPrefix prefix) {
        return """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s",
                    "layer1": "%s"
                  }
                }
                """.formatted("gregtech:" + texturePath(set, prefix, false),
                "gregtech:" + texturePath(set, prefix, true));
    }
}
