package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** GT6 MultiTileEntityCrucible render pass 5: molten/solid surface across the open 3x3 vessel. */
public final class LargeCrucibleRenderer implements BlockEntityRenderer<LargeCrucibleControllerBlockEntity> {
    public LargeCrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(LargeCrucibleControllerBlockEntity be, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int packedLight, int overlay) {
        if (!be.isFormedForRendering()) return;
        renderHull(be, pose, buffers, packedLight);
        if (be.getDisplayedHeight() == 0 || be.getDisplayedMaterialId() <= 0) return;
        var material = be.getDisplayedMaterial();
        if (material == null || !material.isValid()) return;
        float top = 1.125F + (be.getDisplayedHeight() & 0xFF) / 150.0F;
        if (be.isDisplayedMolten()) {
            var appearance = CrucibleContentIcons.moltenAppearance(material);
            SmeltingCrucibleRenderer.drawTop(pose, buffers, appearance.baseTexture(),
                    appearance.tintBase() ? appearance.tintArgb() : 0xFFFFFFFF,
                    top, -0.999F, -0.999F, 1.999F, 1.999F, 0xF000F0);
        } else {
            SmeltingCrucibleRenderer.drawTop(pose, buffers, CrucibleContentIcons.SOLID_GRAY_BASE,
                    CrucibleContentIcons.solidGrayTint(), top,
                    -0.999F, -0.999F, 1.999F, 1.999F, packedLight);
        }
    }

    /** GT6 MultiTileEntityCrucible:628-655: four half-block walls and a 1.125-block floor. */
    private static void renderHull(LargeCrucibleControllerBlockEntity be, PoseStack pose,
                                   MultiBufferSource buffers, int fallbackLight) {
        float outer0 = -.999F, outer1 = 1.999F, inner0 = -.5F, inner1 = 1.5F;
        faceBox(be, pose, buffers, outer0, 0, outer0, inner0, 3, outer1, fallbackLight,
                net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST);
        faceBox(be, pose, buffers, inner1, 0, outer0, outer1, 3, outer1, fallbackLight,
                net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST);
        faceBox(be, pose, buffers, outer0, 0, outer0, outer1, 3, inner0, fallbackLight,
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH);
        faceBox(be, pose, buffers, outer0, 0, inner1, outer1, 3, outer1, fallbackLight,
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH);
        faceBox(be, pose, buffers, outer0, 0, outer0, outer1, 1.125F, outer1, fallbackLight,
                net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN);
        // Partition the rim so its four top faces do not overlap at the corners.
        faceBox(be, pose, buffers, outer0, 0, outer0, outer1, 3, inner0, fallbackLight, net.minecraft.core.Direction.UP);
        faceBox(be, pose, buffers, outer0, 0, inner1, outer1, 3, outer1, fallbackLight, net.minecraft.core.Direction.UP);
        faceBox(be, pose, buffers, outer0, 0, inner0, inner0, 3, inner1, fallbackLight, net.minecraft.core.Direction.UP);
        faceBox(be, pose, buffers, inner1, 0, inner0, outer1, 3, inner1, fallbackLight, net.minecraft.core.Direction.UP);
    }

    private static void faceBox(LargeCrucibleControllerBlockEntity be, PoseStack pose, MultiBufferSource buffers,
                                float x0, float y0, float z0, float x1, float y1, float z1, int fallbackLight,
                                net.minecraft.core.Direction... faces) {
        var out = buffers.getBuffer(net.minecraft.client.renderer.RenderType.cutout());
        int tint = be.variant().material().getColor();
        if (be.isMeltDownWarning()) tint = CrucibleBlockBakedModel.meltdownRgb(tint);
        var facing = be.getBlockState().getValue(com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock.FACING);
        for (var face : faces) {
            String part = face == net.minecraft.core.Direction.UP ? "top"
                    : face == net.minecraft.core.Direction.DOWN ? "bottom" : "side";
            String front = face == facing ? "_front" : "";
            float shade = be.getLevel() == null ? 1 : be.getLevel().getShade(face, true);
            int light = fallbackLight;
            if (be.getLevel() != null) {
                var sample = be.getBlockPos().above(2).relative(face, 2);
                light = net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(), sample);
            }
            for (int layer = 0; layer < 2; layer++) {
                var sprite = CrucibleRenderSprites.resolve(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "gregtech", "block/machines/multiblockmains/crucible/"
                                + (layer == 0 ? "colored" : "overlay") + front + "/" + part));
                int rgb = layer == 0 ? tint : 0xFFFFFF;
                LargeCrucibleHullFace.draw(pose.last(), out, face, x0, y0, z0, x1, y1, z1,
                        sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), rgb, shade, light);
            }
        }
    }

    @Override public boolean shouldRenderOffScreen(LargeCrucibleControllerBlockEntity be) { return true; }
}
