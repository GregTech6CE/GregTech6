package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.content.tool.CoinGeometry;
import com.gregtech.gregtech.item.MaterialItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Each occupied cell uses its own position and exact height, with the stored coin's GT6 die relief. */
public final class CoinPileRenderer implements BlockEntityRenderer<CoinPileBlockEntity> {
    static final net.minecraft.resources.ResourceLocation TEXTURE = GregTech.id("textures/block/iconsets/coin.png");
    static final net.minecraft.resources.ResourceLocation SIDE_TEXTURE = GregTech.id("textures/block/iconsets/coin_side.png");
    public CoinPileRenderer(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(CoinPileBlockEntity pile, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var material = MaterialItem.getMaterial(pile.coinItem());
        int color = material == null ? 0xFFFFFF : material.getColor();
        CoinGeometry.MintPattern pattern = pile.coinPattern();
        // BufferSource may share a single builder between these render types. Finish each
        // texture pass before requesting the next buffer; never retain both consumers.
        for (int pass = 0; pass < 2; pass++) {
            var out = buffers.getBuffer(RenderType.entityCutout(pass == 0 ? TEXTURE : SIDE_TEXTURE));
            for (int face = 0; face < 16; face++) {
                int count = pile.faceCount(face);
                if (count == 0) continue;
                float cellX = (face / 4) / 4.0F;
                float cellZ = (face % 4) / 4.0F;
                pose.pushPose();
                pose.translate(cellX, 0, cellZ);
                for (var box : pattern.pixelBoxes(count))
                    drawCoinPixel(pose.last(), out, box, pattern, color, light, pass == 0, cellX, cellZ);
                pose.popPose();
            }
        }
    }

    private static void drawCoinPixel(PoseStack.Pose pose, VertexConsumer out, AABB box,
                                      CoinGeometry.MintPattern pattern,
                                      int color, int light, boolean horizontal, float cellX, float cellZ) {
        if (horizontal) {
            quad(pose, out, box, color, light, Direction.DOWN, cellX, cellZ);
            quad(pose, out, box, color, light, Direction.UP, cellX, cellZ);
        } else {
            int x = (int) (box.minX * 64);
            int z = (int) (box.minZ * 64);
            for (Direction side : Direction.Plane.HORIZONTAL)
                if (pattern.isExposedSide(x, z, side))
                    quad(pose, out, box, color, light, side, cellX, cellZ);
        }
    }

    /** GT6 samples world coin textures from bounds relative to the whole block. */
    static void quad(PoseStack.Pose pose, VertexConsumer out, AABB b,
                     int color, int light, Direction side, float cellX, float cellZ) {
        float x0 = (float) b.minX, x1 = (float) b.maxX;
        float y0 = (float) b.minY, y1 = (float) b.maxY;
        float z0 = (float) b.minZ, z1 = (float) b.maxZ;
        switch (side) {
            case DOWN -> {
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y0, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y0, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y0, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y0, z1);
            }
            case UP -> {
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y1, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y1, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y1, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y1, z0);
            }
            case NORTH -> {
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y0, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y1, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y1, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y0, z0);
            }
            case SOUTH -> {
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y0, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y0, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y1, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y1, z1);
            }
            case WEST -> {
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y0, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y0, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y1, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x0, y1, z0);
            }
            case EAST -> {
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y0, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y1, z0);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y1, z1);
                vertex(pose, out, color, light, side, cellX, cellZ, x1, y0, z1);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, int color, int light,
                               Direction side, float cellX, float cellZ,
                               float x, float y, float z) {
        out.vertex(pose.pose(), x, y, z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)
                .uv(CoinGeometry.surfaceU(side, x + cellX, z + cellZ),
                        CoinGeometry.surfaceV(side, y, z + cellZ))
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), side.getStepX(), side.getStepY(), side.getStepZ()).endVertex();
    }
}
