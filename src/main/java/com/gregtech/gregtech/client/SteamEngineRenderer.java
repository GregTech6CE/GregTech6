package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;

/** Renders engine core heat color every frame. */
public class SteamEngineRenderer implements BlockEntityRenderer<KineticSteamEngineBlockEntity> {
    private static final ResourceLocation ENGINE_TEX =
            ResourceLocation.fromNamespaceAndPath("gregtech", "block/machines/engines/kinetic_steam/colored/engine");

    // Core box in model space (facing NORTH): [4,4,0] to [12,12,16]
    private static final float X0 = 4f / 16f, X1 = 12f / 16f;
    private static final float Y0 = 4f / 16f, Y1 = 12f / 16f;
    private static final float Z0 = 0f,        Z1 = 1f;

    // Tiny offset to lift BER faces just above model geometry, avoiding z-fighting
    private static final float EPS = 0.001f;

    public SteamEngineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(KineticSteamEngineBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        long cap = be.energyCapacity();
        if (cap <= 0) return;

        int color = heatColor(be.getKuEnergy(), cap);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ENGINE_TEX);
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;

        ps.pushPose();
        applyFacing(ps, facing);

        Matrix4f m = ps.last().pose();
        int overlay = OverlayTexture.NO_OVERLAY;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        faceNorthZ(m, vc, X0, Y0, X1, Y1, Z0 - EPS, u0, v0, u1, v1, r, g, b, packedLight, overlay);
        faceEastX (m, vc, X1 + EPS, Y0, Y1, Z0, Z1, u0, v0, u1, v1, r, g, b, packedLight, overlay);
        faceWestX (m, vc, X0 - EPS, Y0, Y1, Z0, Z1, u0, v0, u1, v1, r, g, b, packedLight, overlay);
        faceUpY   (m, vc, X0, X1, Y1 + EPS, Z0, Z1, u0, v0, u1, v1, r, g, b, packedLight, overlay);
        faceDownY (m, vc, X0, X1, Y0 - EPS, Z0, Z1, u0, v0, u1, v1, r, g, b, packedLight, overlay);

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(KineticSteamEngineBlockEntity be) {
        return true;
    }

    // ── Heat color (blue 0% → green 50% → red 100%) ─────────────────────

    static int heatColor(long kuEnergy, long capacity) {
        if (capacity <= 0) return 0x0000FF;
        int pct = (int) Math.min(100, kuEnergy * 100 / capacity);
        int r, g, b;
        if (pct <= 50) {
            double f = pct / 50.0;
            r = 0;
            g = (int) (255.0 * f);
            b = (int) (255.0 * (1.0 - f));
        } else {
            double f = (pct - 50) / 50.0;
            r = (int) (255.0 * f);
            g = (int) (255.0 * (1.0 - f));
            b = 0;
        }
        return (r << 16) | (g << 8) | b;
    }

    // ── Facing rotation ─────────────────────────────────────────────────

    private static void applyFacing(PoseStack ps, Direction facing) {
        ps.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case NORTH -> {}
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST  -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case EAST  -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case UP    -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN  -> ps.mulPose(Axis.XP.rotationDegrees(-90));
        }
        ps.translate(-0.5, -0.5, -0.5);
    }

    // ── Face primitives (CCW winding when viewed from outside) ───────────

    private static void faceNorthZ(Matrix4f m, VertexConsumer vc,
                                   float x0, float y0, float x1, float y1, float z,
                                   float u0, float v0, float u1, float v1,
                                   float r, float g, float b, int light, int overlay) {
        vc.vertex(m, x1, y0, z).color(r, g, b, 1).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        vc.vertex(m, x0, y0, z).color(r, g, b, 1).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, 1).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, 1).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
    }

    private static void faceEastX(Matrix4f m, VertexConsumer vc,
                                  float x, float y0, float y1, float z0, float z1,
                                  float u0, float v0, float u1, float v1,
                                  float r, float g, float b, int light, int overlay) {
        vc.vertex(m, x, y0, z1).color(r, g, b, 1).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        vc.vertex(m, x, y0, z0).color(r, g, b, 1).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        vc.vertex(m, x, y1, z0).color(r, g, b, 1).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        vc.vertex(m, x, y1, z1).color(r, g, b, 1).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
    }

    private static void faceWestX(Matrix4f m, VertexConsumer vc,
                                  float x, float y0, float y1, float z0, float z1,
                                  float u0, float v0, float u1, float v1,
                                  float r, float g, float b, int light, int overlay) {
        vc.vertex(m, x, y0, z0).color(r, g, b, 1).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
        vc.vertex(m, x, y0, z1).color(r, g, b, 1).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
        vc.vertex(m, x, y1, z1).color(r, g, b, 1).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
        vc.vertex(m, x, y1, z0).color(r, g, b, 1).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
    }

    private static void faceUpY(Matrix4f m, VertexConsumer vc,
                                float x0, float x1, float y, float z0, float z1,
                                float u0, float v0, float u1, float v1,
                                float r, float g, float b, int light, int overlay) {
        vc.vertex(m, x1, y, z0).color(r, g, b, 1).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(m, x0, y, z0).color(r, g, b, 1).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(m, x0, y, z1).color(r, g, b, 1).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(m, x1, y, z1).color(r, g, b, 1).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
    }

    private static void faceDownY(Matrix4f m, VertexConsumer vc,
                                  float x0, float x1, float y, float z0, float z1,
                                  float u0, float v0, float u1, float v1,
                                  float r, float g, float b, int light, int overlay) {
        vc.vertex(m, x1, y, z1).color(r, g, b, 1).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
        vc.vertex(m, x0, y, z1).color(r, g, b, 1).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
        vc.vertex(m, x0, y, z0).color(r, g, b, 1).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
        vc.vertex(m, x1, y, z0).color(r, g, b, 1).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
    }
}
