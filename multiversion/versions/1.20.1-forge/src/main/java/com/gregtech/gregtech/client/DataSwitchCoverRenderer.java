package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Draws the selected-mode cover on either data switch using the shared cover renderer. */
public final class DataSwitchCoverRenderer implements BlockEntityRenderer<UsbSwitchBlockEntity> {
    public DataSwitchCoverRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(UsbSwitchBlockEntity machine, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        for (Direction side : Direction.values())
            PanelCoverRenderer.renderFace(machine, side, pose, buffers, light);
    }
}
