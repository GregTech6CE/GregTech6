package com.gregtech.gregtech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Client render helpers ported from GT6 {@code gregapi.render.RenderHelper}.
 */
public final class GTRenderHelper {
    private GTRenderHelper() {}

    /**
     * GT6 {@code RenderHelper.drawWrenchOverlay} base 3x3 grid on one face + wireframe cube.
     * PoseStack must already be at block center ({@code block + 0.5}).
     */
    public static void drawWrenchOverlay(PoseStack poseStack, VertexConsumer consumer, Direction side, float shade) {
        drawWrenchOverlayGrid(poseStack, consumer, side, shade);
        GTWrenchOverlay.drawWireframeCube(poseStack, consumer, shade);
    }

    /**
     * GT6 machine overlay: 3x3 grid on all six faces; connection markers on {@code aimedSide}
     * with {@code frontFacing} as the connected direction.
     */
    public static void drawFacingMachineWrenchOverlay(PoseStack poseStack, VertexConsumer consumer,
                                                      Direction aimedSide, Direction frontFacing, float shade) {
        GTWrenchOverlay.drawMachineOverlay(poseStack, consumer, aimedSide, frontFacing, shade);
    }

    /**
     * GT6 pipe wrench overlay: 3x3 grid on all six faces; connection markers on {@code aimedSide}
     * for all directions set in the {@code connections} bit mask.
     */
    public static void drawPipeWrenchOverlay(PoseStack poseStack, VertexConsumer consumer,
                                              Direction aimedSide, int connections, float shade) {
        GTWrenchOverlay.drawPipeOverlay(poseStack, consumer, aimedSide, connections, shade);
    }

    static void drawWrenchOverlayGrid(PoseStack poseStack, VertexConsumer consumer, Direction side, float shade) {
        poseStack.pushPose();
        applySideRotation(poseStack, side);
        poseStack.translate(0.0D, -0.5025D, 0.0D);

        double color = shade;
        float r = (float) color;
        float g = (float) color;
        float b = (float) color;
        float a = 0.5F;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, -0.50F, 0.0F, -0.25F);
        line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, -0.50F, 0.0F, 0.25F);
        line(consumer, pose, normal, r, g, b, a, 0.25F, 0.0F, -0.50F, 0.25F, 0.0F, 0.50F);
        line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, -0.25F, 0.0F, 0.50F);

        poseStack.popPose();
    }

    /** {@code codechicken.lib.vec.Rotation.sideRotations[side]} (side 0=DOWN .. 5=EAST). */
    static void applySideRotation(PoseStack poseStack, Direction side) {
        switch (side) {
            case DOWN -> { /* sideRotations[0] identity */ }
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }

    static void line(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                     float r, float g, float b, float a,
                     float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.vertex(pose, x1, y1, z1).color(r, g, b, a).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(r, g, b, a).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** GT6 pulsing overlay shade from {@code CLIENT_TIME}. */
    public static float wrenchOverlayShade(long clientTime) {
        return clientTime % 42L < 21L
                ? (float) (0.25D + (clientTime % 21L) / 40.0D)
                : (float) (0.75D - (clientTime % 21L) / 40.0D);
    }

    /** GT6 {@code GL11.glLineWidth(2)} setup for wrench overlays. */
    public static void beginWrenchOverlayLines() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.0F);
    }

    public static void endWrenchOverlayLines() {
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
    }
}
