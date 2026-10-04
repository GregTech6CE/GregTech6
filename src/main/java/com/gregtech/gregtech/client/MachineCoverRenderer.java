package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Flush covers on the host's block face, with separate texture layers and no protruding plate. */
public final class MachineCoverRenderer implements BlockEntityRenderer<BasicMachineBlockEntity> {
    public MachineCoverRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(BasicMachineBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers,
                                  int packedLight, int packedOverlay) {

        var level = be.getLevel();
        for (Direction side : Direction.values()) {
            var cover = be.getCover(side);
            if (cover.isEmpty() || PanelCoverRenderer.renderFace(be, side, pose, buffers, packedLight)) continue;
            var model = Minecraft.getInstance().getItemRenderer().getModel(cover, level, null, 0);
            var sprite = model.getParticleIcon();
            int light = level == null ? packedLight : LevelRenderer.getLightColor(level, be.getBlockPos().relative(side));
            var vertices = buffers.getBuffer(RenderType.cutout());
            boolean layered = com.gregtech.gregtech.content.cover.MachineCoverSpec.of(cover) != null;
            if (layered) CoverSurfaceRenderer.draw(pose, vertices, side, ArmRenderHelper.getSprite(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "block/machines/covers/base")), light, 0);
            CoverSurfaceRenderer.draw(pose, vertices, side, sprite, light, layered ? 1 : 0);
        }
    }
}
