package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

/** Content rendering for mold and mold basin — molten fluid, tinted solid, and mold 5x5 grid. */
public final class SmelteryHullRenderer implements BlockEntityRenderer<BlockEntity> {

    private static final ResourceLocation SOLID_BASE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/metallic/blocksolid");

    public SmelteryHullRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (be instanceof MoldBlockEntity mold) {
            renderMold(mold, poseStack, bufferSource, packedLight);
        } else if (be instanceof MoldBasinBlockEntity basin) {
            renderBasin(basin, poseStack, bufferSource, packedLight);
        }
    }

    private static void renderMold(MoldBlockEntity mold, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight) {
        // 1. Mold grid — solid cells (bit=0) render as raised bumps at y=1/16 to 3/16
        renderMoldGrid(mold, poseStack, bufferSource, packedLight);

        // 2. Solid output item (already extracted, nothing to render in-world)
        ItemStack solidOutput = mold.getSolidOutput();
        if (!solidOutput.isEmpty()) {
            return;
        }

        // 3. Content (molten/solidifying) at fixed 2px height (y=3/16 to 5/16)
        GTMaterial material = mold.getMoldContentMaterial();
        long amount = mold.getMoldContentAmount();
        if (material == null || !material.isValid() || amount <= 0) return;

        Geometry geo = moldContentGeometry();
        float topY = geo.maxY;

        poseStack.pushPose();
        if (mold.isContentSolidified()) {
            int tint = material.getColor() | 0xFF000000;
            drawTopCentered(poseStack, bufferSource, SOLID_BASE, tint,
                    topY, geo.x0, geo.z0, geo.x1, geo.z1, packedLight);
        } else {
            renderContent(poseStack, bufferSource, material, mold.getTemperature(), topY,
                    geo.x0, geo.z0, geo.x1, geo.z1, packedLight, true);
        }
        poseStack.popPose();
    }

    private static void renderBasin(MoldBasinBlockEntity basin, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight) {
        // 1. Solid output fills the entire basin to the brim
        ItemStack solidOutput = basin.getSolidOutput();
        if (!solidOutput.isEmpty()) {
            int tint = basin.getSolidOutputTint();
            Geometry geo = basinGeometry();
            poseStack.pushPose();
            drawTop(poseStack, bufferSource, SOLID_BASE, tint,
                    geo.maxY, geo.x0, geo.z0, geo.x1, geo.z1, packedLight);
            poseStack.popPose();
            return;
        }

        // 2. Render molten/solidifying content
        GTMaterial material = basin.getMoldContentMaterial();
        long amount = basin.getMoldContentAmount();
        if (material == null || !material.isValid() || amount <= 0) return;

        long capacity = basinCapacity(basin);
        if (capacity <= 0) return;

        Geometry geo = basinGeometry();
        float fillFraction = Math.min(1.0F, (float) amount / (float) capacity);
        float topY = geo.floor + (geo.maxY - geo.floor) * fillFraction;
        if (topY <= geo.floor) return;

        poseStack.pushPose();
        renderContent(poseStack, bufferSource, material, basin.getTemperature(), topY,
                geo.x0, geo.z0, geo.x1, geo.z1, packedLight, false);
        poseStack.popPose();
    }

    /** Render the 5x5 mold grid — only top faces with UV from center 12/16 of the texture. */
    private static void renderMoldGrid(MoldBlockEntity mold, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight) {
        int shape = mold.getMoldShape();
        if (shape == 0b11111_11111_11111_11111_11111) return; // all cells are cavities, nothing to render

        CrucibleSpec spec = mold.spec();
        int tint = spec != null ? spec.tintRgb() : 0xFFFFFF;
        TextureAtlasSprite sprite = CrucibleRenderSprites.resolve(SOLID_BASE);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        float a = 1.0F;
        int overlay = OverlayTexture.NO_OVERLAY;

        // 12px grid area (2/16 to 14/16), each cell 12/5 = 2.4px
        float cellSize = 12.0F / 5.0F / 16.0F;
        float x0 = 2 / 16.0F;
        float z0 = 2 / 16.0F;
        float y1 = 3 / 16.0F; // top face of cells

        // UV: center 12/16 of the full 16x16 texture
        float uSpan = sprite.getU1() - sprite.getU0();
        float vSpan = sprite.getV1() - sprite.getV0();
        float uBase = sprite.getU0() + 2.0F / 16.0F * uSpan;
        float vBase = sprite.getV0() + 2.0F / 16.0F * vSpan;
        float uCellSpan = 12.0F / 16.0F * uSpan / 5.0F;
        float vCellSpan = 12.0F / 16.0F * vSpan / 5.0F;

        float y0 = 1 / 16.0F; // bottom of cell

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int bit = 1 << (row * 5 + col);
                if ((shape & bit) != 0) continue; // GT6: bit=1 = cavity, skip; bit=0 = solid, render

                float cx0 = x0 + col * cellSize;
                float cz0 = z0 + row * cellSize;
                float cx1 = cx0 + cellSize;
                float cz1 = cz0 + cellSize;

                float cu0 = uBase + col * uCellSpan;
                float cu1 = cu0 + uCellSpan;
                float cv0 = vBase + row * vCellSpan;
                float cv1 = cv0 + vCellSpan;

                // Top face
                consumer.addVertex(matrix, cx0, y1, cz0).setColor(r, g, b, a).setUv(cu0, cv0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
                consumer.addVertex(matrix, cx0, y1, cz1).setColor(r, g, b, a).setUv(cu0, cv1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
                consumer.addVertex(matrix, cx1, y1, cz1).setColor(r, g, b, a).setUv(cu1, cv1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
                consumer.addVertex(matrix, cx1, y1, cz0).setColor(r, g, b, a).setUv(cu1, cv0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);

                // Side faces — render where adjacent cell is cavity (bit=1) or out-of-bounds
                // UV: center 12/16 region, V narrowed to center strip for 2px face height
                float sideU0 = cu0;
                float sideU1 = cu1;
                float sideV0 = sprite.getV0() + (7.0F / 16.0F) * vSpan;
                float sideV1 = sprite.getV0() + (9.0F / 16.0F) * vSpan;

                // North face (z-), CW from south view: LB→RB→RT→LT (X left, Y up)
                if (row == 0 || (shape & (1 << ((row - 1) * 5 + col))) != 0) {
                    consumer.addVertex(matrix, cx1, y0, cz0).setColor(r, g, b, a).setUv(sideU0, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, -1);
                    consumer.addVertex(matrix, cx0, y0, cz0).setColor(r, g, b, a).setUv(sideU1, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, -1);
                    consumer.addVertex(matrix, cx0, y1, cz0).setColor(r, g, b, a).setUv(sideU1, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, -1);
                    consumer.addVertex(matrix, cx1, y1, cz0).setColor(r, g, b, a).setUv(sideU0, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, -1);
                }
                // South face (z+), CW from north view: LB→RB→RT→LT (X right, Y up)
                if (row == 4 || (shape & (1 << ((row + 1) * 5 + col))) != 0) {
                    consumer.addVertex(matrix, cx0, y0, cz1).setColor(r, g, b, a).setUv(sideU0, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, 1);
                    consumer.addVertex(matrix, cx1, y0, cz1).setColor(r, g, b, a).setUv(sideU1, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, 1);
                    consumer.addVertex(matrix, cx1, y1, cz1).setColor(r, g, b, a).setUv(sideU1, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, 1);
                    consumer.addVertex(matrix, cx0, y1, cz1).setColor(r, g, b, a).setUv(sideU0, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(0, 0, 1);
                }
                // West face (x-), CW from west view: LB→RB→RT→LT (Z right, Y up)
                if (col == 0 || (shape & (1 << (row * 5 + (col - 1)))) != 0) {
                    consumer.addVertex(matrix, cx0, y0, cz0).setColor(r, g, b, a).setUv(sideU0, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(-1, 0, 0);
                    consumer.addVertex(matrix, cx0, y0, cz1).setColor(r, g, b, a).setUv(sideU1, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(-1, 0, 0);
                    consumer.addVertex(matrix, cx0, y1, cz1).setColor(r, g, b, a).setUv(sideU1, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(-1, 0, 0);
                    consumer.addVertex(matrix, cx0, y1, cz0).setColor(r, g, b, a).setUv(sideU0, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(-1, 0, 0);
                }
                // East face (x+), CW from east view: LB→RB→RT→LT (Z left, Y up)
                if (col == 4 || (shape & (1 << (row * 5 + (col + 1)))) != 0) {
                    consumer.addVertex(matrix, cx1, y0, cz1).setColor(r, g, b, a).setUv(sideU0, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(1, 0, 0);
                    consumer.addVertex(matrix, cx1, y0, cz0).setColor(r, g, b, a).setUv(sideU1, sideV1).setOverlay(overlay).setLight(packedLight).setNormal(1, 0, 0);
                    consumer.addVertex(matrix, cx1, y1, cz0).setColor(r, g, b, a).setUv(sideU1, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(1, 0, 0);
                    consumer.addVertex(matrix, cx1, y1, cz1).setColor(r, g, b, a).setUv(sideU0, sideV0).setOverlay(overlay).setLight(packedLight).setNormal(1, 0, 0);
                }
            }
        }
    }

    /** Render content fill — molten fluid (fullbright) or solidifying material.
     *  Mold content uses center 12×12 of texture shifted down 4px. */
    private static void renderContent(PoseStack poseStack, MultiBufferSource bufferSource,
                                      GTMaterial material, long temperature, float topY,
                                      float x0, float z0, float x1, float z1, int packedLight,
                                      boolean useMoldUv) {
        boolean isMolten = material.getMeltingPoint() > 0 && temperature >= material.getMeltingPoint();
        if (isMolten) {
            MaterialFluidVisuals.Appearance appearance = MaterialFluidVisuals.forMaterial(material);
            int tint = appearance.tintBase() ? appearance.tintArgb() : 0xFFFFFFFF;
            if (useMoldUv) {
                drawTopCentered(poseStack, bufferSource, appearance.baseTexture(), tint,
                        topY, x0, z0, x1, z1, 0xF000F0);
            } else {
                drawTop(poseStack, bufferSource, appearance.baseTexture(), tint,
                        topY, x0, z0, x1, z1, 0xF000F0);
            }
        } else {
            int tint = material.getColor() | 0xFF000000;
            if (useMoldUv) {
                drawTopCentered(poseStack, bufferSource, SOLID_BASE, tint,
                        topY, x0, z0, x1, z1, packedLight);
            } else {
                drawTop(poseStack, bufferSource, SOLID_BASE, tint,
                        topY, x0, z0, x1, z1, packedLight);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEntity be) {
        if (be instanceof MoldBlockEntity mold) {
            return mold.getMoldContentAmount() > 0 || !mold.getSolidOutput().isEmpty();
        }
        if (be instanceof MoldBasinBlockEntity basin) {
            return basin.getMoldContentAmount() > 0 || !basin.getSolidOutput().isEmpty();
        }
        return false;
    }

    // --- Geometry ---

    private record Geometry(float x0, float z0, float x1, float z1, float floor, float maxY) {}

    /** Mold content area: same footprint as grid, y=2.5/16 (within grid cells) */
    private static Geometry moldContentGeometry() {
        return new Geometry(2 / 16.0F, 2 / 16.0F, 14 / 16.0F, 14 / 16.0F, 2.5F / 16.0F, 2.5F / 16.0F);
    }

    private static Geometry basinGeometry() {
        return new Geometry(1 / 16.0F, 1 / 16.0F, 15 / 16.0F, 15 / 16.0F, 1 / 16.0F, 15 / 16.0F);
    }

    private static long basinCapacity(MoldBasinBlockEntity basin) {
        long required = basin.getMoldRequiredMaterialUnits();
        return required > 0 ? required : 1;
    }

    // --- Draw helpers ---

    private static void drawTop(PoseStack poseStack, MultiBufferSource bufferSource,
                                ResourceLocation texture, int tint,
                                float y, float x0, float z0, float x1, float z1, int packedLight) {
        TextureAtlasSprite sprite = CrucibleRenderSprites.resolve(texture);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        float a = ((tint >> 24) & 0xFF) / 255.0F;
        if (a == 0) a = 1.0F;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int overlay = OverlayTexture.NO_OVERLAY;
        consumer.addVertex(matrix, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
    }

    /** drawTop variant using center 12×12 of texture, shifted down 4px — for mold content. */
    private static void drawTopCentered(PoseStack poseStack, MultiBufferSource bufferSource,
                                        ResourceLocation texture, int tint,
                                        float y, float x0, float z0, float x1, float z1, int packedLight) {
        TextureAtlasSprite sprite = CrucibleRenderSprites.resolve(texture);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        float a = ((tint >> 24) & 0xFF) / 255.0F;
        if (a == 0) a = 1.0F;
        float uSpan = sprite.getU1() - sprite.getU0();
        float vSpan = sprite.getV1() - sprite.getV0();
        // Center 12×12 of 16×16 texture, shifted down 4px
        float u0 = sprite.getU0() + 2.0F / 16.0F * uSpan;
        float u1 = sprite.getU0() + 14.0F / 16.0F * uSpan;
        float v0 = sprite.getV0() + 4.0F / 16.0F * vSpan;
        float v1 = sprite.getV0() + 16.0F / 16.0F * vSpan;
        int overlay = OverlayTexture.NO_OVERLAY;
        consumer.addVertex(matrix, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
    }
}
