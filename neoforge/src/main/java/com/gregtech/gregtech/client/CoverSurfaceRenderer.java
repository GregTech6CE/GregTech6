package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.cover.CoverFaceCoordinates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;

/** Full host faces stay flush; non-full hosts have the original two-pixel attachment and holder. */
final class CoverSurfaceRenderer {
    private CoverSurfaceRenderer() {}
    static boolean needsAttachment(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side) {
        return owner.getLevel()!=null && !com.gregtech.gregtech.content.cover.CoverWorldInteraction.isCoverSurface(owner,side);
    }
    static void attachment(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side,
                           PoseStack pose, VertexConsumer vertices, TextureAtlasSprite sprite, int light, int color) {
        if (!needsAttachment(owner, side)) return;
        cuboid(pose, vertices, com.gregtech.gregtech.content.cover.CoverPlateGeometry.plate(side.ordinal()), sprite, light, color, null);
        // The source holder is a two-pixel rod from the plate to the host's centre, without end caps.
        cuboid(pose, vertices, com.gregtech.gregtech.content.cover.CoverPlateGeometry.holder(side.ordinal()), sprite, light, color, side.getAxis());
    }
    private static void cuboid(PoseStack pose, VertexConsumer vertices,
            com.gregtech.gregtech.content.cover.CoverPlateGeometry.Box box, TextureAtlasSprite sprite,
            int light, int color, Direction.Axis skipAxis) {
        ArmRenderHelper.drawCuboid(pose.last().pose(), vertices,
                (float)box.minX(), (float)box.minY(), (float)box.minZ(),
                (float)box.maxX(), (float)box.maxY(), (float)box.maxZ(),
                ((color>>16)&255)/255f, ((color>>8)&255)/255f, (color&255)/255f, sprite, light, skipAxis);
    }
    static void torch(PoseStack pose, VertexConsumer vertices, Direction side,
                      TextureAtlasSprite body, TextureAtlasSprite front, int light) {
        cuboid(pose,vertices,com.gregtech.gregtech.content.cover.CoverPlateGeometry.holder(side.ordinal()),body,light,-1,null);
        drawAt(pose,vertices,side,front,light,distance(0),-1,7.0/16,9.0/16);
    }
    static void drawBack(PoseStack pose, VertexConsumer vertices, Direction side,
                         TextureAtlasSprite sprite, int light, int layer, int color) {
        drawAt(pose, vertices, side.getOpposite(), sprite, light,
                com.gregtech.gregtech.content.cover.CoverPlateGeometry.THICKNESS - 1 + distance(layer), color);
    }
    static double distance(int layer) { return .0005 + Math.max(0, layer) * .0001; }
    static void draw(PoseStack pose, VertexConsumer vertices, Direction side,
                     TextureAtlasSprite sprite, int light, int layer) {draw(pose,vertices,side,sprite,light,layer,-1);}
    static void draw(PoseStack pose, VertexConsumer vertices, Direction side,TextureAtlasSprite sprite,int light,int layer,int color) {
        drawAt(pose, vertices, side, sprite, light, distance(layer), color);
    }
    private static void drawAt(PoseStack pose, VertexConsumer vertices, Direction side,
                               TextureAtlasSprite sprite, int light, double depth, int color) {
        drawAt(pose,vertices,side,sprite,light,depth,color,0,1);
    }
    private static void drawAt(PoseStack pose, VertexConsumer vertices, Direction side,
                               TextureAtlasSprite sprite, int light, double depth, int color, double min, double max) {
        for (int corner = 0; corner < 4; corner++) {
            double u = corner >= 2 ? max : min, v = corner == 1 || corner == 2 ? max : min;
            var point = CoverFaceCoordinates.to(side, u, v, depth);
            vertices.addVertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                    .setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1f).setUv(sprite.getU((float)u), sprite.getV((float)v))
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(), side.getStepX(), side.getStepY(), side.getStepZ());
        }
    }
}
