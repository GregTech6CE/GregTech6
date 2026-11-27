package gregtech.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/** 1.21.1 carries ambient occlusion on the quad; 1.20.1 has no such constructor argument. */
public final class BakedQuadCompat {
    private BakedQuadCompat() {}

    /** Keeps geometry, shading and UV layout when changing an atlas sprite. */
    static BakedQuad retexture(BakedQuad source, TextureAtlasSprite sprite, int tint) {
        return retexture(source, sprite, tint, 0);
    }

    static BakedQuad retexture(BakedQuad source, TextureAtlasSprite sprite, int tint, float offset) {
        if (source.getSprite() == sprite && source.getTintIndex() == tint && offset == 0) return source;
        int[] vertices = source.getVertices().clone();
        FluidItemBakedModel.remapUv(vertices, source.getSprite(), sprite);
        if (offset != 0) {
            Direction face = source.getDirection();
            int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                int base = i * stride;
                vertices[base] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base]) + face.getStepX() * offset);
                vertices[base + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 1]) + face.getStepY() * offset);
                vertices[base + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 2]) + face.getStepZ() * offset);
            }
        }
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
