package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * GT6 machine wrench overlay: 3x3 grid on all six faces; GT6 connection markers (X / corner brackets)
 * on the currently aimed face only ({@code gregapi.render.RenderHelper.drawWrenchOverlay}).
 */
final class GTWrenchOverlay {

    private GTWrenchOverlay() {}

    static void drawMachineOverlay(PoseStack poseStack, VertexConsumer consumer,
                                   Direction aimedSide, Direction frontFacing, float shade) {
        GTRenderHelper.drawWrenchOverlayGrid(poseStack, consumer, aimedSide, shade);
        drawWireframeCube(poseStack, consumer, shade);
        drawWrenchOverlayConnections(poseStack, consumer, aimedSide, 1 << frontFacing.ordinal(), shade);
    }

    /** Pipe wrench overlay: 3x3 grid on aimed face + wireframe cube + connection markers for all connected sides. */
    static void drawPipeOverlay(PoseStack poseStack, VertexConsumer consumer,
                                Direction aimedSide, int connections, float shade) {
        GTRenderHelper.drawWrenchOverlayGrid(poseStack, consumer, aimedSide, shade);
        drawWireframeCube(poseStack, consumer, shade);
        drawWrenchOverlayConnections(poseStack, consumer, aimedSide, connections, shade);
    }

    /** GT6 wireframe cube: 12 edges of the block bounding box (±0.5), drawn from block center. */
    static void drawWireframeCube(PoseStack poseStack, VertexConsumer consumer, float shade) {
        float r = shade;
        float g = shade;
        float b = shade;
        float a = 0.5F;
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Bottom face
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.5F, -0.5F, -0.5F,  0.5F, -0.5F, -0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a,  0.5F, -0.5F, -0.5F,  0.5F, -0.5F,  0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a,  0.5F, -0.5F,  0.5F, -0.5F, -0.5F,  0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.5F, -0.5F,  0.5F, -0.5F, -0.5F, -0.5F);
        // Top face
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.5F,  0.5F, -0.5F,  0.5F,  0.5F, -0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a,  0.5F,  0.5F, -0.5F,  0.5F,  0.5F,  0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a,  0.5F,  0.5F,  0.5F, -0.5F,  0.5F,  0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.5F,  0.5F,  0.5F, -0.5F,  0.5F, -0.5F);
        // Vertical edges
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.5F, -0.5F, -0.5F, -0.5F,  0.5F, -0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a,  0.5F, -0.5F, -0.5F,  0.5F,  0.5F, -0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a,  0.5F, -0.5F,  0.5F,  0.5F,  0.5F,  0.5F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.5F, -0.5F,  0.5F, -0.5F,  0.5F,  0.5F);
    }

    /** GT6 {@code RenderHelper.drawWrenchOverlay} connection decorations on one face. */
    static void drawWrenchOverlayConnections(PoseStack poseStack, VertexConsumer consumer,
                                             Direction side, int connections, float shade) {
        poseStack.pushPose();
        GTRenderHelper.applySideRotation(poseStack, side);
        poseStack.translate(0.0D, -0.5025D, 0.0D);

        float r = shade;
        float g = shade;
        float b = shade;
        float a = 0.5F;
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        switch (side) {
            case DOWN -> drawConnectionsDown(consumer, pose, normal, r, g, b, a, connections);
            case UP -> drawConnectionsUp(consumer, pose, normal, r, g, b, a, connections);
            case NORTH -> drawConnectionsNorth(consumer, pose, normal, r, g, b, a, connections);
            case SOUTH -> drawConnectionsSouth(consumer, pose, normal, r, g, b, a, connections);
            case WEST -> drawConnectionsWest(consumer, pose, normal, r, g, b, a, connections);
            case EAST -> drawConnectionsEast(consumer, pose, normal, r, g, b, a, connections);
        }

        poseStack.popPose();
    }

    private static boolean connected(Direction direction, int connections) {
        return (connections & (1 << direction.ordinal())) != 0;
    }

    private static void drawConnectionsDown(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                            float r, float g, float b, float a, int connections) {
        if (connected(Direction.DOWN, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.UP, connections)) {
            drawFullCornerBrackets(consumer, pose, normal, r, g, b, a);
        }
        if (connected(Direction.NORTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);
        }
        if (connected(Direction.SOUTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);
        }
        if (connected(Direction.WEST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.EAST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
    }

    private static void drawConnectionsUp(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                          float r, float g, float b, float a, int connections) {
        if (connected(Direction.DOWN, connections)) {
            drawFullCornerBrackets(consumer, pose, normal, r, g, b, a);
        }
        if (connected(Direction.UP, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.NORTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);
        }
        if (connected(Direction.SOUTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);
        }
        if (connected(Direction.WEST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.EAST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
    }

    private static void drawConnectionsNorth(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                             float r, float g, float b, float a, int connections) {
        if (connected(Direction.DOWN, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);
        }
        if (connected(Direction.UP, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);
        }
        if (connected(Direction.NORTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.SOUTH, connections)) {
            drawFullCornerBrackets(consumer, pose, normal, r, g, b, a);
        }
        if (connected(Direction.WEST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.EAST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
    }

    private static void drawConnectionsSouth(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                             float r, float g, float b, float a, int connections) {
        if (connected(Direction.DOWN, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);
        }
        if (connected(Direction.UP, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);
        }
        if (connected(Direction.NORTH, connections)) {
            drawFullCornerBrackets(consumer, pose, normal, r, g, b, a);
        }
        if (connected(Direction.SOUTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.WEST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.EAST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
    }

    private static void drawConnectionsWest(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                            float r, float g, float b, float a, int connections) {
        if (connected(Direction.DOWN, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.UP, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.NORTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);
        }
        if (connected(Direction.SOUTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);
        }
        if (connected(Direction.WEST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.EAST, connections)) {
            drawFullCornerBrackets(consumer, pose, normal, r, g, b, a);
        }
    }

    private static void drawConnectionsEast(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                            float r, float g, float b, float a, int connections) {
        if (connected(Direction.DOWN, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.UP, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
        if (connected(Direction.NORTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);
        }
        if (connected(Direction.SOUTH, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);
        }
        if (connected(Direction.WEST, connections)) {
            drawFullCornerBrackets(consumer, pose, normal, r, g, b, a);
        }
        if (connected(Direction.EAST, connections)) {
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, -0.25F, 0.25F, 0.0F, 0.25F);
            GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.25F, 0.0F, 0.25F, 0.25F, 0.0F, -0.25F);
        }
    }

    /** GT6 eight-line corner bracket set used when the opposite face is connected. */
    private static void drawFullCornerBrackets(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                               float r, float g, float b, float a) {
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.50F, -0.25F, 0.0F, -0.25F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, -0.25F, -0.25F, 0.0F, -0.50F);

        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.50F, 0.25F, 0.0F, 0.25F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, 0.25F, 0.25F, 0.0F, 0.50F);

        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.50F, 0.25F, 0.0F, -0.25F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, 0.50F, 0.0F, -0.25F, 0.25F, 0.0F, -0.50F);

        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.50F, -0.25F, 0.0F, 0.25F);
        GTRenderHelper.line(consumer, pose, normal, r, g, b, a, -0.50F, 0.0F, 0.25F, -0.25F, 0.0F, 0.50F);
    }
}
