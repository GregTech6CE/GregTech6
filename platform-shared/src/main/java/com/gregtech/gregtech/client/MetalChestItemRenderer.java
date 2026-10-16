package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.inventory.MetalChestBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class MetalChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MetalChestItemRenderer instance;
    private final MetalChestVisuals visuals = new MetalChestVisuals();
    private MetalChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }
    public static MetalChestItemRenderer instance() {
        if (instance == null) instance = new MetalChestItemRenderer();
        return instance;
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MetalChestBlock chest)
            visuals.render(Direction.NORTH, chest, 0, pose, buffers, light, overlay);
    }
}
