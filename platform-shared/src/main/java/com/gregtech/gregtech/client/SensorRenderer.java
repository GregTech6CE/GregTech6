package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DirectionalBlock;

/** Draws the measured value on the sensor's display face. */
public final class SensorRenderer implements BlockEntityRenderer<SensorBlockEntity> {

    public SensorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SensorBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getLevel() == null) return;
        var state = be.getBlockState();
        Direction facing = state.hasProperty(DirectionalBlock.FACING)
                ? state.getValue(DirectionalBlock.FACING) : Direction.NORTH;
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));

        ps.pushPose();
        ps.translate(0.5 + facing.getStepX() * -0.373, 0.5 + facing.getStepY() * -0.373,
                0.5 + facing.getStepZ() * -0.373);
        ps.mulPose(com.gregtech.gregtech.api.block.DisplayFaceOrientation.rotation(facing));

        SixCellDisplayRenderer.draw(be.displayCells(), ps, buffer, light);
        ps.popPose();
    }
}
