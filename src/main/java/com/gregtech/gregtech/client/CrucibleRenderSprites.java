package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** Block-atlas sprites for crucible interior fill (atlas-safe, with fallbacks). */
public final class CrucibleRenderSprites {
    private static final ResourceLocation FALLBACK = GregTech.id("block/material_icons/metallic/blockraw");

    private CrucibleRenderSprites() {}

    public static TextureAtlasSprite resolve(ResourceLocation texture) {
        TextureAtlas atlas = atlas();
        TextureAtlasSprite sprite = atlas.getSprite(texture);
        if (sprite == atlas.getSprite(MissingTextureAtlasSprite.getLocation())) {
            return atlas.getSprite(FALLBACK);
        }
        return sprite;
    }

    public static boolean hasTexture(ResourceLocation texture) {
        TextureAtlas atlas = atlas();
        return atlas.getSprite(texture) != atlas.getSprite(MissingTextureAtlasSprite.getLocation());
    }

    private static TextureAtlas atlas() {
        return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
    }
}
