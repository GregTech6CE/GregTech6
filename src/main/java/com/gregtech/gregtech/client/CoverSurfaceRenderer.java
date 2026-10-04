package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.cover.CoverFaceCoordinates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;

/** GT6 full surface covers compose with the host face; only a depth epsilon separates layers. */
final class CoverSurfaceRenderer {
    private CoverSurfaceRenderer() {}
    static double distance(int layer) { return .0005 + Math.max(0, layer) * .0001; }
    static void draw(PoseStack pose, VertexConsumer vertices, Direction side,
                     TextureAtlasSprite sprite, int light, int layer) {
        for (int corner = 0; corner < 4; corner++) {
            double u = corner >= 2 ? 1 : 0, v = corner == 1 || corner == 2 ? 1 : 0;
            var point = CoverFaceCoordinates.to(side, u, v, distance(layer));
            vertices.vertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                    .color(1f, 1f, 1f, 1f).uv(sprite.getU(u*16), sprite.getV(v*16))
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.last().normal(), side.getStepX(), side.getStepY(), side.getStepZ()).endVertex();
        }
    }
}
