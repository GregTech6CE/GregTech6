package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.data.MaterialPrefix;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/** Shared original material icon selection and resource paths, without client classes. */
public final class MaterialIconDefinitions {
    private MaterialIconDefinitions() {}

    public static MaterialTextureSet resolveTextureSet(GTMaterial material) {
        // Original OreDictMaterial keeps EMPTY on SET_NONE (headless arrows/charged casings).
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
        return "item/material_icons/" + set.folder() + "/" + prefix.getTextureFileName() + (overlay ? "_overlay" : "");
    }

    public static String modelPath(MaterialTextureSet set, MaterialPrefix prefix) {
        return "item/material/" + set.folder() + "/" + prefix.getTextureFileName();
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

    /** Both platform geometry loaders delegate this reference to the original baked template cache. */
    public static String sharedInventoryJson(String parent) {
        return "{\"loader\":\"" + gregtech.engine.mod.GregTechIdentity.MOD_ID + ":shared_inventory\",\"parent\":\""
                + parent + "\"}";
    }

    /**
     * Nearest set in the inheritance chain that actually ships {@code fileName}, or {@code null}
     * when no set in the chain does. {@code resourceDirectory} is relative to {@code textures/}.
     */
    @Nullable
    public static MaterialTextureSet resolveTextureOwner(
            MaterialTextureSet set, String resourceDirectory, String fileName, Predicate<String> resourceExists) {
        for (MaterialTextureSet current = set; current != null; current = current.parent()) {
            if (resourceExists.test(
                    "textures/" + resourceDirectory + "/" + current.folder() + "/" + fileName + ".png")) {
                return current;
            }
        }
        return null;
    }

    /** Icon set owning the item sprite for this prefix, following {@link MaterialTextureSet#parent()}. */
    @Nullable
    public static MaterialTextureSet resolveItemTextureOwner(
            MaterialTextureSet set, MaterialPrefix prefix, boolean overlay, Predicate<String> resourceExists) {
        return resolveTextureOwner(
                set, "item/material_icons", prefix.getTextureFileName() + (overlay ? "_overlay" : ""), resourceExists);
    }

    /** Icon set owning a block sprite, following {@link MaterialTextureSet#parent()}. */
    @Nullable
    public static MaterialTextureSet resolveBlockTextureOwner(
            MaterialTextureSet set, String fileName, Predicate<String> resourceExists) {
        return resolveTextureOwner(set, "block/material_icons", fileName, resourceExists);
    }

    /** Classpath probe for probes taken before the first client resource reload finishes. */
    public static boolean classpathResourceExists(String relativePath) {
        return MaterialIconDefinitions.class.getResource(
                        "/assets/" + gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE + "/" + relativePath)
                != null;
    }

    /**
     * Shared item model JSON for one set and prefix, or {@code null} when no set in the chain ships
     * the base sprite. The overlay layer is emitted only when the chain ships a detail sprite.
     */
    @Nullable
    public static String sharedModelJson(
            MaterialTextureSet set, MaterialPrefix prefix, Predicate<String> resourceExists) {
        MaterialTextureSet baseOwner = resolveItemTextureOwner(set, prefix, false, resourceExists);
        if (baseOwner == null) return null;
        MaterialTextureSet overlayOwner = resolveItemTextureOwner(set, prefix, true, resourceExists);
        return generatedModelJson(
                "gt6ce:" + texturePath(baseOwner, prefix, false),
                overlayOwner == null ? null : "gt6ce:" + texturePath(overlayOwner, prefix, true));
    }

    private static String generatedModelJson(String layer0, @Nullable String layer1) {
        if (layer1 == null) {
            return """
                    {
                      "parent": "minecraft:item/generated",
                      "textures": {
                        "layer0": "%s"
                      }
                    }
                    """.formatted(layer0);
        }
        return """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s",
                    "layer1": "%s"
                  }
                }
                """.formatted(layer0, layer1);
    }
}
