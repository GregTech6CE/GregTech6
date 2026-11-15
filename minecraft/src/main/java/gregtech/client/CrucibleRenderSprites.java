package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** Block-atlas sprites for crucible interior fill (atlas-safe, with fallbacks). */
public final class CrucibleRenderSprites {
    private CrucibleRenderSprites() {}

    /** Last-resort solid fill: the {@code blockraw} art of the metallic set. */
    private static ResourceLocation fallback() {
        return BlockMaterialIcons.resolveTexture(MaterialTextureSet.METALLIC, "blockraw");
    }

    public static TextureAtlasSprite resolve(ResourceLocation texture) {
        TextureAtlas atlas = atlas();
        TextureAtlasSprite sprite = atlas.getSprite(texture);
        if (sprite == atlas.getSprite(MissingTextureAtlasSprite.getLocation())) {
            return atlas.getSprite(fallback());
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
