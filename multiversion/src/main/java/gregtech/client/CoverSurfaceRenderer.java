package gregtech.client;

import gregtech.content.cover.CoverFaceCoordinates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;

/** Full host faces stay flush; non-full hosts have the original two-pixel attachment and holder. */
final class CoverSurfaceRenderer {
    private CoverSurfaceRenderer() {}
    static boolean needsAttachment(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side) {
        return owner.getLevel()!=null && !gregtech.content.cover.CoverWorldInteraction.isCoverSurface(owner,side);
    }
    static void attachment(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side,
                           PoseStack pose, VertexConsumer vertices, TextureAtlasSprite sprite, int light, int color) {
        if (!needsAttachment(owner, side)) return;
        int covers = fullCoverMask(owner), hostFaces = hostFaceMask(owner);
        int faces = gregtech.content.cover.CoverRenderGeometry.plateFaces(side.ordinal(), covers | hostFaces);
        // Covers on existing full host faces have no backing cuboid to claim edge volume.
        cuboid(pose, vertices, gregtech.content.cover.CoverRenderGeometry.plate(side.ordinal(), covers & ~hostFaces), sprite, light, color, faces);
        // Source holders inset their lateral faces by PX_OFFSET; physical torch bounds stay unchanged.
        cuboid(pose, vertices, gregtech.content.cover.CoverRenderGeometry.holder(side.ordinal()), sprite, light, color, side.getAxis());
    }
    private static int fullCoverMask(net.minecraft.world.level.block.entity.BlockEntity owner) {
        int mask = 0;
        for (var side : Direction.values()) {
            var stack = owner instanceof gregtech.content.cover.PanelCoverHost host
                    ? host.getCover(side) : net.minecraft.world.item.ItemStack.EMPTY;
            if (stack.isEmpty() && owner instanceof gregtech.content.logistics.LogisticsCoverHost logistics)
                stack = logistics.logisticsCovers().get(side);
            var id = gregtech.content.cover.CoverItems.behavior(stack);
            if (gregtech.content.cover.CoverRenderGeometry.fullTexture(id)) mask |= 1 << side.ordinal();
        }
        return mask;
    }
    private static int hostFaceMask(net.minecraft.world.level.block.entity.BlockEntity owner) {
        int mask = 0;
        for (var side : Direction.values())
            if (gregtech.content.cover.CoverWorldInteraction.isCoverSurface(owner, side))
                mask |= 1 << side.ordinal();
        return mask;
    }
    private static void cuboid(PoseStack pose, VertexConsumer vertices,
            gregtech.content.cover.CoverPlateGeometry.Box box, TextureAtlasSprite sprite,
            int light, int color, int faces) {
        ArmRenderHelper.drawCuboid(pose.last().pose(), vertices,
                (float)box.minX(), (float)box.minY(), (float)box.minZ(),
                (float)box.maxX(), (float)box.maxY(), (float)box.maxZ(),
                ((color>>16)&255)/255f, ((color>>8)&255)/255f, (color&255)/255f, sprite, light, faces);
    }
    private static void cuboid(PoseStack pose, VertexConsumer vertices,
            gregtech.content.cover.CoverPlateGeometry.Box box, TextureAtlasSprite sprite,
            int light, int color, Direction.Axis skipAxis) {
        ArmRenderHelper.drawCuboid(pose.last().pose(), vertices,
                (float)box.minX(), (float)box.minY(), (float)box.minZ(),
                (float)box.maxX(), (float)box.maxY(), (float)box.maxZ(),
                ((color>>16)&255)/255f, ((color>>8)&255)/255f, (color&255)/255f, sprite, light, skipAxis);
    }
    static void torch(PoseStack pose, VertexConsumer vertices, Direction side,
                      TextureAtlasSprite body, TextureAtlasSprite front, int light) {
        cuboid(pose,vertices,gregtech.content.cover.CoverPlateGeometry.holder(side.ordinal()),body,light,-1,null);
        drawAt(pose,vertices,side,front,light,distance(0),-1,7.0/16,9.0/16);
    }
    static void drawBack(PoseStack pose, VertexConsumer vertices, Direction side,
                         TextureAtlasSprite sprite, int light, int layer, int color) {
        drawAt(pose, vertices, side.getOpposite(), sprite, light,
                gregtech.content.cover.CoverPlateGeometry.THICKNESS - 1 + distance(layer), color);
    }
    // Stay outside the host's layered texture planes while keeping the cover flush to the face.
    static double distance(int layer) { return .002 + Math.max(0, layer) * .0005; }
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
//? if >=1.21.1 {
            /*vertices.addVertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                    .setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1f).setUv(sprite.getU((float)u), sprite.getV((float)v))
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(), side.getStepX(), side.getStepY(), side.getStepZ());
*///?} else {
            vertices.vertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                    .color(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1f).uv(sprite.getU(u*16), sprite.getV(v*16))
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.last().normal(), side.getStepX(), side.getStepY(), side.getStepZ()).endVertex();
//?}
        }
    }
}
