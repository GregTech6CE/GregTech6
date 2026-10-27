package gregtech.client;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
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
//? if >=1.21.1 {
            /*var sprite = model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
*///?} else {
            var sprite = model.getParticleIcon();
//?}
            int light = CoverSurfaceRenderer.light(be, side, packedLight);
            var vertices = buffers.getBuffer(RenderType.cutout());
            boolean layered = gregtech.content.cover.MachineCoverSpec.of(cover) != null;
            if (layered) CoverSurfaceRenderer.draw(be,pose, vertices, side, ArmRenderHelper.getSprite(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "block/machines/covers/base")), light, 0);
            CoverSurfaceRenderer.draw(be,pose, vertices, side, sprite, light, layered ? 1 : 0);
        }
    }
}
