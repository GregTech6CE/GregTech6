package gregtech.client;

import gregtech.content.cover.CoverFaceCoordinates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;

/** Full host faces stay flush; non-full hosts have the original two-pixel attachment and holder. */
final class CoverSurfaceRenderer {
    private CoverSurfaceRenderer() {}
    static int light(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side, int fallback) {
        var level = owner.getLevel();
        if (level == null) return fallback;
        var adjacent = owner.getBlockPos().relative(side);
        // Vanilla ModelBlockRenderer samples a full boundary face at its outward neighbour,
        // using the host state for light emission. Other faces must not brighten this face.
        if (!needsAttachment(owner, side))
            return net.minecraft.client.renderer.LevelRenderer.getLightColor(level, owner.getBlockState(), adjacent);
        int face = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, adjacent);
        if (!level.getBlockState(adjacent).isSolidRender(level, adjacent)) return face;
        int host = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, owner.getBlockPos());
        for (Direction exposed : Direction.values()) {
            var sample = owner.getBlockPos().relative(exposed);
            if (level.getBlockState(sample).isSolidRender(level, sample)) continue;
            int value = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, sample);
            host = net.minecraft.client.renderer.LightTexture.pack(
                    Math.max(net.minecraft.client.renderer.LightTexture.block(host), net.minecraft.client.renderer.LightTexture.block(value)),
                    Math.max(net.minecraft.client.renderer.LightTexture.sky(host), net.minecraft.client.renderer.LightTexture.sky(value)));
        }
        // Preserve sky and block channels independently, without making the cover emissive.
        return net.minecraft.client.renderer.LightTexture.pack(
                Math.max(net.minecraft.client.renderer.LightTexture.block(face), net.minecraft.client.renderer.LightTexture.block(host)),
                Math.max(net.minecraft.client.renderer.LightTexture.sky(face), net.minecraft.client.renderer.LightTexture.sky(host)));
    }
    static boolean needsAttachment(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side) {
        return owner.getLevel()!=null && !gregtech.content.cover.CoverWorldInteraction.isCoverSurface(owner,side);
    }
    private static float shade(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side) {
        return owner == null || owner.getLevel() == null || needsAttachment(owner, side)
                ? 1 : owner.getLevel().getShade(side, true);
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
    /** Original GT6 {@code CoverPressureValve}: a recessed 4x4 px knob, front art on the outward
     *  face and the side texture on the lateral faces. The face toward the host interior stays open. */
    static void valve(PoseStack pose, VertexConsumer vertices, Direction side,
                      TextureAtlasSprite front, TextureAtlasSprite body, int light) {
        var box = gregtech.content.cover.CoverPlateGeometry.valve(side.ordinal());
        for (Direction face : Direction.values()) {
            if (face == side.getOpposite()) continue;
            boolean outward = face == side;
            face(pose, vertices, outward ? front : body, light, face, box,
                    6.0 / 16, 10.0 / 16, outward ? 6.0 / 16 : 0.0, outward ? 10.0 / 16 : 8.0 / 16);
        }
    }
    private static void face(PoseStack pose, VertexConsumer vertices, TextureAtlasSprite sprite, int light,
                             Direction face, gregtech.content.cover.CoverPlateGeometry.Box b,
                             double uMin, double uMax, double vMin, double vMax) {
        float x0 = (float) b.minX(), y0 = (float) b.minY(), z0 = (float) b.minZ();
        float x1 = (float) b.maxX(), y1 = (float) b.maxY(), z1 = (float) b.maxZ();
        float[][] corners = switch (face) {
            case DOWN -> new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1}};
            case UP -> new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}};
            case NORTH -> new float[][]{{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y0,z0}};
            case SOUTH -> new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
            case WEST -> new float[][]{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}};
            case EAST -> new float[][]{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}};
        };
//? if >=1.21.1 {
        /*float su0 = sprite.getU((float) uMin), su1 = sprite.getU((float) uMax);
        float sv0 = sprite.getV((float) vMin), sv1 = sprite.getV((float) vMax);
*///?} else {
        float su0 = sprite.getU((float) (uMin * 16)), su1 = sprite.getU((float) (uMax * 16));
        float sv0 = sprite.getV((float) (vMin * 16)), sv1 = sprite.getV((float) (vMax * 16));
//?}
        float[][] uv = {{su0, sv0}, {su1, sv0}, {su1, sv1}, {su0, sv1}};
        var mat = pose.last().pose();
        for (int i = 0; i < 4; i++) {
//? if >=1.21.1 {
            /*vertices.addVertex(mat, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(1f, 1f, 1f, 1f).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
*///?} else {
            vertices.vertex(mat, corners[i][0], corners[i][1], corners[i][2])
                    .color(1f, 1f, 1f, 1f).uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
//?}
        }
    }
    static void drawBack(PoseStack pose, VertexConsumer vertices, Direction side,
                         TextureAtlasSprite sprite, int light, int layer, int color) {
        drawAt(pose, vertices, side.getOpposite(), sprite, light,
                gregtech.content.cover.CoverPlateGeometry.THICKNESS - 1 + distance(layer), color);
    }
    // Stay outside the host's layered texture planes while keeping the cover flush to the face.
    static double distance(int layer) { return .002 + Math.max(0, layer) * .0005; }
    static void draw(net.minecraft.world.level.block.entity.BlockEntity owner, PoseStack pose, VertexConsumer vertices,
                     Direction side, TextureAtlasSprite sprite, int light, int layer) {
        draw(owner, pose, vertices, side, sprite, light, layer, -1);
    }
    static void draw(net.minecraft.world.level.block.entity.BlockEntity owner, PoseStack pose, VertexConsumer vertices,
                     Direction side, TextureAtlasSprite sprite, int light, int layer, int color) {
        drawAt(owner, pose, vertices, side, sprite, light, distance(layer), color);
    }
    static void drawAt(net.minecraft.world.level.block.entity.BlockEntity owner, PoseStack pose, VertexConsumer vertices,
                       Direction side, TextureAtlasSprite sprite, int light, double depth, int color) {
        drawAt(pose, vertices, side, sprite, light, depth, color, 0, 1, shade(owner, side));
    }
    private static void drawAt(PoseStack pose, VertexConsumer vertices, Direction side,
                               TextureAtlasSprite sprite, int light, double depth, int color) {
        drawAt(pose,vertices,side,sprite,light,depth,color,0,1);
    }
    private static void drawAt(PoseStack pose, VertexConsumer vertices, Direction side,
                               TextureAtlasSprite sprite, int light, double depth, int color, double min, double max) {
        drawAt(pose, vertices, side, sprite, light, depth, color, min, max, 1);
    }
    private static void drawAt(PoseStack pose, VertexConsumer vertices, Direction side,
                               TextureAtlasSprite sprite, int light, double depth, int color, double min, double max, float shade) {
        float red = ((color >> 16) & 255) / 255f * shade;
        float green = ((color >> 8) & 255) / 255f * shade;
        float blue = (color & 255) / 255f * shade;
        for (int corner = 0; corner < 4; corner++) {
            double u = corner >= 2 ? max : min, v = corner == 1 || corner == 2 ? max : min;
            var point = CoverFaceCoordinates.to(side, u, v, depth);
//? if >=1.21.1 {
            /*vertices.addVertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                    .setColor(red, green, blue, 1f).setUv(sprite.getU((float)u), sprite.getV((float)v))
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(), side.getStepX(), side.getStepY(), side.getStepZ());
*///?} else {
            vertices.vertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                    .color(red, green, blue, 1f).uv(sprite.getU(u*16), sprite.getV(v*16))
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.last().normal(), side.getStepX(), side.getStepY(), side.getStepZ()).endVertex();
//?}
        }
    }
}
