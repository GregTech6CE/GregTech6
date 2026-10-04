package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Flush covers on the host's block face, with separate texture layers and no protruding plate. */
public final class PipeCoverRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {
    public PipeCoverRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(FluidPipeBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers,
                                  int packedLight, int packedOverlay) {
        if (!be.hasCovers()) return;
        var level = be.getLevel();
        for (Direction side : Direction.values()) {
            var cover = be.getCover(side);
            if (cover.isEmpty() || PanelCoverRenderer.renderFace(be, side, pose, buffers, packedLight)) continue;
            var model = Minecraft.getInstance().getItemRenderer().getModel(cover, level, null, 0);
            var sprite = model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
            int light = level == null ? packedLight : LevelRenderer.getLightColor(level, be.getBlockPos().relative(side));
            var vertices = buffers.getBuffer(RenderType.cutout());
            boolean layered = com.gregtech.gregtech.content.cover.MachineCoverSpec.of(cover) != null;
            if (layered) CoverSurfaceRenderer.draw(pose, vertices, side, ArmRenderHelper.getSprite(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "block/machines/covers/base")), light, 0);
            CoverSurfaceRenderer.draw(pose, vertices, side, sprite, light, layered ? 1 : 0);
        }
    }
}
