package gregtech.internal.material;

import gregtech.data.MaterialPrefix;

/** Shared original material icon selection and resource paths, without client classes. */
public final class MaterialIconDefinitions {
    private MaterialIconDefinitions() {}

    public static MaterialTextureSet resolveTextureSet(GTMaterial material) {
        // Original OreDictMaterial:252 keeps EMPTY on SET_NONE (headless arrows/charged casings).
        if ("Empty".equals(material.getName())) return MaterialTextureSet.NONE;
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

    /** One generated item model. The texture lives in the addon namespace and is tinted by material colour. */
    public static String addonItemModel(MaterialPrefix prefix) {
        return """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s:%s"
                  }
                }
                """.formatted(prefix.textureNamespace(), prefix.addonTexturePath());
    }

    public static String sharedModelJson(MaterialTextureSet set, MaterialPrefix prefix) {
        if (prefix == MaterialPrefix.coin) return "{\"parent\":\"gt6ce:item/coin_minted\"}";
        return generatedModelJson(set, prefix);
    }

    /** Both platform geometry loaders delegate this reference to the original baked template cache. */
    public static String sharedInventoryJson(String parent) {
        return "{\"loader\":\"" + gregtech.internal.mod.GregTechIdentity.MOD_ID
                + ":shared_inventory\",\"parent\":\"" + parent + "\"}";
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
                """.formatted("gt6ce:" + texturePath(set, prefix, false),
                "gt6ce:" + texturePath(set, prefix, true));
    }
}
