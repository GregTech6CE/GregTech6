package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialIconDefinitions;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

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
        return ResourceLocation.fromNamespaceAndPath("gregtech", MaterialIconDefinitions.texturePath(set, prefix, overlay));
    }

    public static String modelJson(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelJson(material, prefix);
    }

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, MaterialPrefix prefix) {
        return ResourceLocation.fromNamespaceAndPath("gregtech", MaterialIconDefinitions.modelPath(set, prefix));
    }

    public static ModelResourceLocation sharedModelInventory(MaterialTextureSet set, MaterialPrefix prefix) {
        return ModelResourceLocation.inventory(sharedModelLocation(set, prefix));
    }

    public static ModelResourceLocation sharedModelStandalone(MaterialTextureSet set, MaterialPrefix prefix) {
        return ModelResourceLocation.standalone(sharedModelLocation(set, prefix));
    }

    public static String sharedModelId(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelPath(resolveTextureSet(material), prefix);
    }

    public static String sharedModelJson(MaterialTextureSet set, MaterialPrefix prefix) {
        return MaterialIconDefinitions.sharedModelJson(set, prefix);
    }
}
