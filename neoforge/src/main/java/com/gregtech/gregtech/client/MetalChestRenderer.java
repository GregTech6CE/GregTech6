package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.inventory.MetalChestBlock;
import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Shares the original 64x64 chest UV layout with the inventory renderer. */
public final class MetalChestRenderer implements BlockEntityRenderer<MetalChestBlockEntity> {
    private final MetalChestVisuals visuals = new MetalChestVisuals();
    public MetalChestRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(MetalChestBlockEntity chest, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (chest.getBlockState().getBlock() instanceof MetalChestBlock block)
            visuals.render(chest.getBlockState().getValue(MetalChestBlock.FACING), block,
                    chest.openness(partialTick), pose, buffers, light, overlay);
    }
}
