package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import com.gregtech.gregtech.content.food.SandwichIngredients;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Inventory/hand preview of the same actual layers and source geometry as the placed sandwich. */
public final class SandwichItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static SandwichItemRenderer instance;
    private SandwichItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    public static SandwichItemRenderer instance() {
        if (instance == null) instance = new SandwichItemRenderer();
        return instance;
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        var layers = SandwichBlockEntity.readItemIngredients(stack, Minecraft.getInstance().level == null ? net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY) : Minecraft.getInstance().level.registryAccess());
        SandwichRenderer.renderLayers(slot -> {
            var layer = SandwichIngredients.forItem(layers[slot]);
            return layer == null ? 255 : layer.id();
        }, pose, buffers, light);
    }
}
