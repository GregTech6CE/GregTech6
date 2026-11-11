package gregtech.client;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.platform.Mc;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves GregTech material icon paths under {@code textures/item/material_icons/{set}/{prefix}.png}.
 */

/** Neo resource identifiers over the shared original icon definitions. */
public final class MaterialIcons {
    private MaterialIcons() {}

    public static MaterialTextureSet resolveTextureSet(GTMaterial material) {
        return MaterialIconDefinitions.resolveTextureSet(material);
    }

    public static ResourceLocation baseTexture(GTMaterial material, MaterialPrefix prefix) {
        return texture(resolveTextureSet(material), prefix, false);
    }

    public static ResourceLocation overlayTexture(GTMaterial material, MaterialPrefix prefix) {
        return texture(resolveTextureSet(material), prefix, true);
    }

    public static ResourceLocation texture(MaterialTextureSet set, MaterialPrefix prefix, boolean overlay) {
        return Mc.gregtechId(MaterialIconDefinitions.texturePath(set, prefix, overlay));
    }

    public static String modelJson(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelJson(material, prefix);
    }

    /**
     * Baked model id for {@code models/item/material/{set}/{prefix}.json}.
     * Path is relative to the {@code models/} root, so {@code item/} is required.
     */
    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, MaterialPrefix prefix) {
        return Mc.gregtechId(MaterialIconDefinitions.modelPath(set, prefix));
    }

    public static String sharedModelId(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelPath(resolveTextureSet(material), prefix);
    }

    public static String sharedModelJson(MaterialTextureSet set, MaterialPrefix prefix) {
        return MaterialIconDefinitions.sharedModelJson(set, prefix);
    }
}
