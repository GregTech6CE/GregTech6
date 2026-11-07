package gregtech.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/** 1.21.1 carries ambient occlusion on the quad; 1.20.1 has no such constructor argument. */
public final class BakedQuadCompat {
    private BakedQuadCompat() {}

    public static BakedQuad withAmbientOcclusion(int[] vertices, int tintIndex, Direction direction,
            TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
//? if >=1.21.1 {
/*        return new BakedQuad(vertices, tintIndex, direction, sprite, shade, ambientOcclusion);
*///?} else {
        return new BakedQuad(vertices, tintIndex, direction, sprite, shade);
//?}
    }
}
