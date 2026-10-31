package gregtech.client;

import gregtech.api.material.GTMaterial;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
*///?} else {
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
//?}
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
//? if >=1.21.1 {
/*public final class SmeltingCrucibleRenderer implements BlockEntityRenderer<SmeltingCrucibleEntity> {
*///?} else {
public final class SmeltingCrucibleRenderer implements BlockEntityRenderer<SmeltingCrucibleBlockEntity> {
//?}
    private static final float INTERIOR_MIN = 2 / 16.0F;
    private static final float INTERIOR_MAX = 14 / 16.0F;
    private static final float FLOOR = 2 / 16.0F;

    public SmeltingCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
//? if >=1.21.1 {
    /*public void render(SmeltingCrucibleEntity be, float partialTick, PoseStack poseStack,
*///?} else {
    public void render(SmeltingCrucibleBlockEntity be, float partialTick, PoseStack poseStack,
//?}
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (be.getDisplayedHeight() == 0 || be.getDisplayedMaterialId() <= 0) {
            return;
        }

        GTMaterial material = be.getDisplayedMaterial();
        if (material == null || !material.isValid()) {
            return;
        }

//? if >=1.21.1 {
        /*float topY = (.125F+(be.getDisplayedHeight()&255)/292.571428F);
*///?} else {
        float topY = be.getContentFillMaxY();
//?}
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
            int tint = CrucibleContentIcons.surfaceTint(material, appearance, be.getLevel(), be.getBlockPos());
            int light = CrucibleContentIcons.surfaceLight(be.getLevel(), be.getBlockPos().above(),
                    be.getTemperature(), packedLight);
            drawFluidTop(poseStack, bufferSource, appearance.baseTexture(), tint, topY, x0, z0, x1, z1, light);
        } else {
            drawTop(poseStack, bufferSource, CrucibleContentIcons.SOLID_GRAY_BASE, CrucibleContentIcons.solidGrayTint(),
                    topY, x0, z0, x1, z1, packedLight);
        }
        poseStack.popPose();
    }

    /** Single opaque top surface — GT6 pass 5 only draws {@code SIDES_TOP}. */
    static void drawTop(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int tint,
                                float y, float x0, float z0, float x1, float z1, int packedLight) {
        drawTop(poseStack, bufferSource, texture, tint, y, x0, z0, x1, z1, packedLight, RenderType.solid());
    }

    /** A separate cutout pass retains the transparent pixels in GT6 material detail layers. */
    static void drawTop(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int tint,
                        float y, float x0, float z0, float x1, float z1, int packedLight, RenderType renderType) {
        drawTopUv(poseStack, bufferSource, texture, tint, y, x0, z0, x1, z1, packedLight, renderType, 0, 0, 1, 1);
    }

    static void drawFluidTop(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int tint,
                             float y, float x0, float z0, float x1, float z1, int light) {
        drawFluidTop(pose, buffers, texture, tint, y, x0, z0, x1, z1, light, RenderType.solid());
    }

    /** Crop small surfaces and tile large ones at block boundaries, without sampling adjacent atlas sprites. */
    static void drawFluidTop(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int tint,
                             float y, float x0, float z0, float x1, float z1, int light, RenderType layer) {
        for (int x = (int) Math.floor(x0); x < Math.ceil(x1); x++) {
            for (int z = (int) Math.floor(z0); z < Math.ceil(z1); z++) {
                float left = Math.max(x0, x), right = Math.min(x1, x + 1);
                float near = Math.max(z0, z), far = Math.min(z1, z + 1);
                drawTopUv(pose, buffers, texture, tint, y, left, near, right, far, light, layer,
                        left - x, near - z, right - x, far - z);
            }
        }
    }

    private static void drawTopUv(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int tint,
                                  float y, float x0, float z0, float x1, float z1, int packedLight, RenderType renderType,
                                  float minU, float minV, float maxU, float maxV) {
        TextureAtlasSprite sprite = CrucibleRenderSprites.resolve(texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        Matrix4f matrix = poseStack.last().pose();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        float a = ((tint >> 24) & 0xFF) / 255.0F;
        if (a == 0) {
            a = 1.0F;
        }
        float u0 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * minU;
        float u1 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * maxU;
        float v0 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * minV;
        float v1 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * maxV;
        int overlay = OverlayTexture.NO_OVERLAY;
        // CCW when viewed from above (+Y): normal (0, 1, 0)
//? if >=1.21.1 {
        /*consumer.addVertex(matrix, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
        consumer.addVertex(matrix, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
        consumer.addVertex(matrix, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(),0,1,0);
*///?} else {
        consumer.vertex(matrix, x0, y, z0).color(r, g, b, a).uv(u0, v0).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x0, y, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y, z0).color(r, g, b, a).uv(u1, v0).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
//?}
    }

    @Override
//? if >=1.21.1 {
    /*public boolean shouldRenderOffScreen(SmeltingCrucibleEntity be) {
*///?} else {
    public boolean shouldRenderOffScreen(SmeltingCrucibleBlockEntity be) {
//?}
        return be.getDisplayedHeight() != 0;
    }
}
