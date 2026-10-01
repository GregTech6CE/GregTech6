package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Shows the stored item and amount on the front face of a mass storage. */
public final class MassStorageRenderer<T extends MassStorageBlockEntity> implements BlockEntityRenderer<T> {

    public MassStorageRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(T be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // GT6 isFaceVisible() hides the storage display while duct-taped.
        if (be.isPacked()) return;
        ItemStack shown = be.template();
        if (be.getLevel() == null) return;
        var state = be.getBlockState();
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));

        ps.pushPose();
        ps.translate(0.5 + facing.getStepX() * 0.502, 0.5 + facing.getStepY() * 0.502,
                0.5 + facing.getStepZ() * 0.502);
        ps.mulPose(com.gregtech.gregtech.api.block.DisplayFaceOrientation.rotation(facing));
        ps.pushPose();
        ps.translate(0, .5 - com.gregtech.gregtech.api.inventory.MassStorageFace.ICON_Y / 16, .005);
        ps.scale(0.45f, 0.45f, 0.02f);
        if (!shown.isEmpty()) Minecraft.getInstance().getItemRenderer().renderStatic(shown, ItemDisplayContext.GUI,
                light, packedOverlay, ps, buffer, be.getLevel(), 0);
        ps.popPose();

        int numeralColor = be instanceof com.gregtech.gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity
                ? 0x00FFFF : 0xFFFFFF;
        SixCellDisplayRenderer.draw(com.gregtech.gregtech.api.sensor.SixCellDisplay.storage(
                be.stored(), be.CAPACITY, numeralColor), ps, buffer, light);
        ps.popPose();
    }
}
