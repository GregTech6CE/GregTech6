package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DirectionalBlock;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** GT6's extra 48-pixel rotor render pass spans the formed turbine's front 3x3 wall. */
public final class LargeTurbineRotorRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation IDLE = ResourceLocation.fromNamespaceAndPath(
            "gregtech", "textures/block/machines/multiblockmains/turbine.png");
    private static final ResourceLocation ACTIVE = ResourceLocation.fromNamespaceAndPath(
            "gregtech", "textures/block/machines/multiblockmains/turbine_active.png");
    private static final float FRONT = -0.002f;

    public LargeTurbineRotorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                 int packedLight, int packedOverlay) {
        boolean formed, active;
        if (be instanceof LargeTurbineControllerBlockEntity steam) {
            formed = steam.isRotorFormed();
            active = steam.isRotorActive();
        } else if (be instanceof LargeGasTurbineControllerBlockEntity gas) {
            formed = gas.isRotorFormed();
            active = gas.isRotorActive();
        } else return;
        if (!formed || be.getLevel() == null) return;

        BlockState state = be.getBlockState();
        Direction front = state.getValue(DirectionalBlock.FACING);
        ResourceLocation texture = active ? ACTIVE : IDLE;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        // The source sheet has eight vertically stacked 48x48 frames in reverse order.
        int frame = active ? 7 - Math.floorMod(be.getLevel().getGameTime(), 8) : 0;
        float v0 = active ? frame / 8f : 0f;
        float v1 = active ? (frame + 1) / 8f : 1f;

        pose.pushPose();
        pose.translate(.5, .5, .5);
        switch (front) {
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {}
        }
        pose.translate(-.5, -.5, -.5);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        // The same rotation above maps local +X to right and local +Y to up.
        // For vertical turbines +Y points south when facing up, north when facing down.
        Direction right = front.getAxis().isVertical() ? Direction.EAST : front.getClockWise();
        Direction up = switch (front) {
            case UP -> Direction.SOUTH;
            case DOWN -> Direction.NORTH;
            default -> Direction.UP;
        };
        // GT6 draws one 48x48 face over the 3x3 front wall. Split that face on its
        // 16-pixel boundaries so every third receives the light of its own exterior cell.
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
            BlockPos outside = be.getBlockPos().relative(right, x).relative(up, y).relative(front);
            int light = be.getLevel().hasChunkAt(outside)
                    ? LevelRenderer.getLightColor(be.getLevel(), outside) : packedLight;
            drawTile(vertices, matrix, normal, x, y, v0, v1, light, packedOverlay);
        }
        pose.popPose();
    }

    private static void drawTile(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                 int x, int y, float v0, float v1, int light, int overlay) {
        // Retain the original full-face UV: from outside the north face, world -X
        // is screen-right. Both neighboring tiles use the exact same edge coordinates.
        float x0 = x, x1 = x + 1f, y0 = y, y1 = y + 1f;
        float u0 = (2f - x1) / 3f, u1 = (2f - x0) / 3f;
        float frameHeight = v1 - v0;
        float lowerV = v1 - ((y0 + 1f) / 3f) * frameHeight;
        float upperV = v1 - ((y1 + 1f) / 3f) * frameHeight;
        vertex(vertices, matrix, normal, x1, y0, u0, lowerV, light, overlay);
        vertex(vertices, matrix, normal, x0, y0, u1, lowerV, light, overlay);
        vertex(vertices, matrix, normal, x0, y1, u1, upperV, light, overlay);
        vertex(vertices, matrix, normal, x1, y1, u0, upperV, light, overlay);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float u, float v, int light, int overlay) {
        vertices.vertex(matrix, x, y, FRONT).color(1f, 1f, 1f, 1f).uv(u, v)
                .overlayCoords(overlay).uv2(light).normal(normal, 0, 0, -1).endVertex();
    }

    @Override public boolean shouldRenderOffScreen(T be) { return true; }
}
