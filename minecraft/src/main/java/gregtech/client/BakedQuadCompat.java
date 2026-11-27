package gregtech.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/** 1.21.1 carries ambient occlusion on the quad; 1.20.1 has no such constructor argument. */
public final class BakedQuadCompat {
    private BakedQuadCompat() {}

    /** Keeps geometry, shading and UV layout when changing an atlas sprite. */
    static BakedQuad retexture(BakedQuad source, TextureAtlasSprite sprite, int tint) {
        if (source.getSprite() == sprite && source.getTintIndex() == tint) return source;
        int[] vertices = source.getVertices().clone();
        FluidItemBakedModel.remapUv(vertices, source.getSprite(), sprite);
        return withAmbientOcclusion(vertices, tint, source.getDirection(), sprite, source.isShade(), source.hasAmbientOcclusion());
    }

    public static BakedQuad withAmbientOcclusion(int[] vertices, int tintIndex, Direction direction,
            TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
//? if >=1.21.1 {
/*        return new BakedQuad(vertices, tintIndex, direction, sprite, shade, ambientOcclusion);
*///?} else {
        return new BakedQuad(vertices, tintIndex, direction, sprite, shade);
//?}
    }
}
