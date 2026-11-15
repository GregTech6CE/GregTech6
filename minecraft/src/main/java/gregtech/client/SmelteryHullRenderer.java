package gregtech.client;


import gregtech.engine.machine.CrucibleSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import gregtech.platform.Mc;
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
            Mc.gregtechId("block/material_icons/metallic/blocksolid");

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

    /** Render raised grid cells with the same directional diffuse shade as the baked hull. */
    private static void renderMoldGrid(MoldBlockEntity mold, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight) {
        int shape = mold.getMoldShape();
        if (shape == MoldGridGeometry.ALL_CAVITIES) return; // all cells are cavities, nothing to render

        CrucibleSpec spec = mold.spec();
        int tint = spec != null ? spec.tintRgb() : 0xFFFFFF;
        TextureAtlasSprite sprite = CrucibleRenderSprites.resolve(spec == null || spec.material().getTextureSet() == null ? SOLID_BASE
                : BlockMaterialIcons.baseTexture(spec.material().getTextureSet(), gregtech.engine.prefix.BlockMaterialPrefix.blockSolid));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        int overlay = OverlayTexture.NO_OVERLAY;
        var level = mold.getLevel();
        float topShade = level == null ? 1F : level.getShade(net.minecraft.core.Direction.UP, true);
        float northShade = level == null ? .8F : level.getShade(net.minecraft.core.Direction.NORTH, true);
        float southShade = level == null ? .8F : level.getShade(net.minecraft.core.Direction.SOUTH, true);
        float westShade = level == null ? .6F : level.getShade(net.minecraft.core.Direction.WEST, true);
        float eastShade = level == null ? .6F : level.getShade(net.minecraft.core.Direction.EAST, true);

        float uSpan = sprite.getU1() - sprite.getU0();
        float vSpan = sprite.getV1() - sprite.getV0();
        float y0 = MoldGridGeometry.BOTTOM / 16F;
        float y1 = MoldGridGeometry.TOP / 16F;

        MoldGridGeometry.forEachVisibleFace(shape, (side, cx0, cz0, cx1, cz1, u0, v0, u1, v1) -> {
            float uu0 = sprite.getU0() + u0 / 16F * uSpan;
            float uu1 = sprite.getU0() + u1 / 16F * uSpan;
            float vv0 = sprite.getV0() + v0 / 16F * vSpan;
            float vv1 = sprite.getV0() + v1 / 16F * vSpan;
            float x0 = cx0 / 16F, z0 = cz0 / 16F, x1 = cx1 / 16F, z1 = cz1 / 16F;
            switch (side) {
                case UP -> gridQuad(consumer, matrix, pose, overlay, packedLight, r * topShade, g * topShade, b * topShade,
                        0, 1, 0,
                        x0, y1, z0, uu0, vv0,  x0, y1, z1, uu0, vv1,  x1, y1, z1, uu1, vv1,  x1, y1, z0, uu1, vv0);
                case NORTH -> gridQuad(consumer, matrix, pose, overlay, packedLight, r * northShade, g * northShade, b * northShade,
                        0, 0, -1,
                        x1, y0, z0, uu0, vv1,  x0, y0, z0, uu1, vv1,  x0, y1, z0, uu1, vv0,  x1, y1, z0, uu0, vv0);
                case SOUTH -> gridQuad(consumer, matrix, pose, overlay, packedLight, r * southShade, g * southShade, b * southShade,
                        0, 0, 1,
                        x0, y0, z1, uu0, vv1,  x1, y0, z1, uu1, vv1,  x1, y1, z1, uu1, vv0,  x0, y1, z1, uu0, vv0);
                case WEST -> gridQuad(consumer, matrix, pose, overlay, packedLight, r * westShade, g * westShade, b * westShade,
                        -1, 0, 0,
                        x0, y0, z0, uu0, vv1,  x0, y0, z1, uu1, vv1,  x0, y1, z1, uu1, vv0,  x0, y1, z0, uu0, vv0);
                case EAST -> gridQuad(consumer, matrix, pose, overlay, packedLight, r * eastShade, g * eastShade, b * eastShade,
                        1, 0, 0,
                        x1, y0, z1, uu0, vv1,  x1, y0, z0, uu1, vv1,  x1, y1, z0, uu1, vv0,  x1, y1, z1, uu0, vv0);
                default -> { }
            }
        });
    }

    /** One grid face as four vertices in GT6's counter-clockwise-from-outside order. */
    private static void gridQuad(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose, int overlay, int packedLight,
                                 float r, float g, float b, float nx, float ny, float nz,
                                 float x0, float y0, float z0, float u0, float v0,
                                 float x1, float y1, float z1, float u1, float v1,
                                 float x2, float y2, float z2, float u2, float v2,
                                 float x3, float y3, float z3, float u3, float v3) {
//? if >=1.21.1 {
        /*consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, 1F).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 1F).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 1F).setUv(u2, v2).setOverlay(overlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, 1F).setUv(u3, v3).setOverlay(overlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
*///?} else {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, 1F).uv(u0, v0).overlayCoords(overlay).uv2(packedLight).normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, 1F).uv(u1, v1).overlayCoords(overlay).uv2(packedLight).normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, 1F).uv(u2, v2).overlayCoords(overlay).uv2(packedLight).normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, 1F).uv(u3, v3).overlayCoords(overlay).uv2(packedLight).normal(pose.normal(), nx, ny, nz).endVertex();
//?}
    }

    /** Render content fill — molten fluid (fullbright) or solidifying material.
     *  Mold content uses the same centered 12×12 texture region as the grid. */
    private static void renderContent(PoseStack poseStack, MultiBufferSource bufferSource,
                                      GTMaterial material, long temperature, float topY,
                                      float x0, float z0, float x1, float z1, int packedLight,
                                      boolean useMoldUv) {
        boolean isMolten = material.getMeltingPoint() > 0 && temperature >= material.getMeltingPoint();
        if (isMolten) {
            MaterialFluidVisuals.Appearance appearance = MaterialFluidVisuals.forMaterial(material);
            int tint = appearance.tintBase() ? appearance.tintArgb() : 0xFFFFFFFF;
            SmeltingCrucibleRenderer.drawFluidTop(poseStack, bufferSource, appearance.baseTexture(), tint,
                    topY, x0, z0, x1, z1, 0xF000F0);
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
//? if >=1.21.1 {
        /*consumer.addVertex(matrix, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
*///?} else {
        consumer.vertex(matrix, x0, y, z0).color(r, g, b, a).uv(u0, v0).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x0, y, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y, z0).color(r, g, b, a).uv(u1, v0).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
//?}
    }

    /** drawTop variant using the same centered 12×12 texture region as the grid — for mold content. */
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
        // Center 12×12 of the 16×16 texture, aligned with the cavity grid
        float u0 = sprite.getU0() + 2.0F / 16.0F * uSpan;
        float u1 = sprite.getU0() + 14.0F / 16.0F * uSpan;
        float v0 = sprite.getV0() + 2.0F / 16.0F * vSpan;
        float v1 = sprite.getV0() + 14.0F / 16.0F * vSpan;
        int overlay = OverlayTexture.NO_OVERLAY;
//? if >=1.21.1 {
        /*consumer.addVertex(matrix, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(packedLight).setNormal(0, 1, 0);
*///?} else {
        consumer.vertex(matrix, x0, y, z0).color(r, g, b, a).uv(u0, v0).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x0, y, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y, z0).color(r, g, b, a).uv(u1, v0).overlayCoords(overlay).uv2(packedLight).normal(0, 1, 0).endVertex();
//?}
    }
}
