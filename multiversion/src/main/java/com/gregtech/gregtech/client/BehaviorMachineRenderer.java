package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.addon.platform.BehaviorMachineClientView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Dispatches an addon behavior machine renderer. Machines without one are not drawn. */
public final class BehaviorMachineRenderer implements BlockEntityRenderer<BlockEntity> {
    public BehaviorMachineRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(BlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                                 int light, int overlay) {
        if (!(entity instanceof BehaviorMachineClientView view)) return;
        BlockEntityRenderer<BlockEntity> renderer = BehaviorMachineClients.renderer(view);
        if (renderer != null) renderer.render(entity, partialTick, pose, buffer, light, overlay);
    }

    @Override public boolean shouldRender(BlockEntity entity, Vec3 camera) {
        return entity instanceof BehaviorMachineClientView view && BehaviorMachineClients.renderer(view) != null;
    }
}
