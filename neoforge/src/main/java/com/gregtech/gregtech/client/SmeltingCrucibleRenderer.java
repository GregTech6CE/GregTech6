package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * GT6 {@code MultiTileEntitySmeltery} render pass 5: top face only ({@code SIDES_TOP}).
 */
public final class SmeltingCrucibleRenderer implements BlockEntityRenderer<SmeltingCrucibleEntity> {
    private static final float INTERIOR_MIN = 2 / 16.0F;
    private static final float INTERIOR_MAX = 14 / 16.0F;
    private static final float FLOOR = 2 / 16.0F;

    public SmeltingCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SmeltingCrucibleEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (be.getDisplayedHeight() == 0 || be.getDisplayedMaterialId() <= 0) {
            return;
        }

        GTMaterial material = be.getDisplayedMaterial();
        if (material == null || !material.isValid()) {
            return;
        }

        float topY = (.125F+(be.getDisplayedHeight()&255)/292.571428F);
        if (topY <= FLOOR) {
            return;
        }

        float x0 = INTERIOR_MIN;
        float x1 = INTERIOR_MAX;
        float z0 = INTERIOR_MIN;
        float z1 = INTERIOR_MAX;

        poseStack.pushPose();
        if (be.isDisplayedMolten()) {
            MaterialFluidVisuals.Appearance appearance = CrucibleContentIcons.moltenAppearance(material);
            int tint = appearance.tintBase() ? appearance.tintArgb() : 0xFFFFFFFF;
            drawTop(poseStack, bufferSource, appearance.baseTexture(), tint, topY, x0, z0, x1, z1, 0xF000F0);
        } else {
            drawTop(poseStack, bufferSource, CrucibleContentIcons.SOLID_GRAY_BASE, CrucibleContentIcons.solidGrayTint(),
                    topY, x0, z0, x1, z1, packedLight);
        }
        poseStack.popPose();
    }

    /** Single opaque top surface — GT6 pass 5 only draws {@code SIDES_TOP}. */
    static void drawTop(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int tint,
                                float y, float x0, float z0, float x1, float z1, int packedLight) {
        TextureAtlasSprite sprite = CrucibleRenderSprites.resolve(texture);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        float a = ((tint >> 24) & 0xFF) / 255.0F;
        if (a == 0) {
            a = 1.0F;
        }
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int overlay = OverlayTexture.NO_OVERLAY;
        // CCW when viewed from above (+Y): normal (0, 1, 0)
        consumer.addVertex(matrix, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
        consumer.addVertex(matrix, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
        consumer.addVertex(matrix, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
    }

    @Override
    public boolean shouldRenderOffScreen(SmeltingCrucibleEntity be) {
        return be.getDisplayedHeight() != 0;
    }
}
