package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialIconDefinitions;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.ModelResourceLocation;

/**
 * Resolves GregTech material icon paths under {@code textures/item/material_icons/{set}/{prefix}.png}.
 */
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
        return GregTech.id(MaterialIconDefinitions.texturePath(set, prefix, overlay));
    }

    public static String modelJson(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelJson(material, prefix);
    }

    /**
     * Baked model id for {@code models/item/material/{set}/{prefix}.json}.
     * Path is relative to the {@code models/} root, so {@code item/} is required.
     */
    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, MaterialPrefix prefix) {
        return GregTech.id(MaterialIconDefinitions.modelPath(set, prefix));
    }

    public static ModelResourceLocation sharedModelInventory(MaterialTextureSet set, MaterialPrefix prefix) {
        return new ModelResourceLocation(sharedModelLocation(set, prefix), "inventory");
    }

    public static String sharedModelId(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelPath(resolveTextureSet(material), prefix);
    }

    public static String sharedModelJson(MaterialTextureSet set, MaterialPrefix prefix) {
        return MaterialIconDefinitions.sharedModelJson(set, prefix);
    }
}
