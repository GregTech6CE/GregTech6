package gregtech.client;

import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
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
            var surface = be.getBlockPos().above((int) Math.ceil(top));
            int light = CrucibleContentIcons.surfaceLight(be.getLevel(), surface, be.getTemperature(), packedLight);
            SmeltingCrucibleRenderer.drawFluidTop(pose, buffers, appearance.baseTexture(),
                    CrucibleContentIcons.surfaceTint(material, appearance, be.getLevel(), surface),
                    top, -0.999F, -0.999F, 1.999F, 1.999F, light);
        } else {
            SmeltingCrucibleRenderer.drawFluidTop(pose, buffers, CrucibleContentIcons.SOLID_GRAY_BASE,
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
//? if >=1.21.1 {
        /*if (be.isMeltDownWarning()) tint = gregtech.engine.machine.crucible.CrucibleHullTint.meltdownRgb(tint);
*///?} else {
        if (be.isMeltDownWarning()) tint = CrucibleBlockBakedModel.meltdownRgb(tint);
//?}
        var facing = be.getBlockState().getValue(gregtech.content.block.machine.LargeCrucibleControllerBlock.FACING);
        for (var face : faces) {
            String part = face == net.minecraft.core.Direction.UP ? "top"
                    : face == net.minecraft.core.Direction.DOWN ? "bottom" : "side";
            String front = face == facing ? "_front" : "";
            float shade = be.getLevel() == null ? 1 : be.getLevel().getShade(face, true);
            // A wall spans three blocks. Split every face on block boundaries and sample each
            // cell's own exterior light, so one adjacent full block cannot blacken the whole wall.
            var cells = faceCells(face, x0, y0, z0, x1, y1, z1);
            for (int layer = 0; layer < 2; layer++) {
                var sprite = CrucibleRenderSprites.resolve(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "gt6ce", "block/machines/multiblockmains/crucible/"
                                + (layer == 0 ? "colored" : "overlay") + front + "/" + part));
                float su0 = sprite.getU0(), su1 = sprite.getU1(), sv0 = sprite.getV0(), sv1 = sprite.getV1();
                int rgb = layer == 0 ? tint : 0xFFFFFF;
                for (float[] cell : cells) {
                    int light = cellLight(be, face, cell, fallbackLight);
                    float[] uv = cellUv(face, cell, x0, y0, z0, x1, y1, z1);
                    LargeCrucibleHullFace.draw(pose.last(), out, face,
                            cell[0], cell[1], cell[2], cell[3], cell[4], cell[5],
                            su0 + uv[0] * (su1 - su0), su0 + uv[1] * (su1 - su0),
                            sv0 + uv[2] * (sv1 - sv0), sv0 + uv[3] * (sv1 - sv0),
                            rgb, shade, light);
                }
            }
        }
    }

    /** One cell per block in the face plane; each inherits its own light and texture fraction. */
    private static java.util.List<float[]> faceCells(net.minecraft.core.Direction face,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        var cells = new java.util.ArrayList<float[]>(9);
        switch (face.getAxis()) {
            case Y -> {
                for (int x = (int) Math.floor(x0); x < Math.ceil(x1); x++)
                    for (int z = (int) Math.floor(z0); z < Math.ceil(z1); z++)
                        cells.add(new float[]{Math.max(x0, x), y0, Math.max(z0, z),
                                Math.min(x1, x + 1), y1, Math.min(z1, z + 1)});
            }
            case X -> {
                for (int y = (int) Math.floor(y0); y < Math.ceil(y1); y++)
                    for (int z = (int) Math.floor(z0); z < Math.ceil(z1); z++)
                        cells.add(new float[]{x0, Math.max(y0, y), Math.max(z0, z),
                                x1, Math.min(y1, y + 1), Math.min(z1, z + 1)});
            }
            case Z -> {
                for (int x = (int) Math.floor(x0); x < Math.ceil(x1); x++)
                    for (int y = (int) Math.floor(y0); y < Math.ceil(y1); y++)
                        cells.add(new float[]{Math.max(x0, x), Math.max(y0, y), z0,
                                Math.min(x1, x + 1), Math.min(y1, y + 1), z1});
            }
        }
        return cells;
    }

    /**
     * Light of the block just outside the cell. The half-block offset clears the hull's own cell;
     * the epsilon keeps faces that land on a block boundary on their outward side.
     */
    private static int cellLight(LargeCrucibleControllerBlockEntity be, net.minecraft.core.Direction face,
                                 float[] cell, int fallback) {
        var level = be.getLevel();
        if (level == null) return fallback;
        var origin = be.getBlockPos();
        var sample = net.minecraft.core.BlockPos.containing(
                origin.getX() + (cell[0] + cell[3]) * 0.5D + face.getStepX() * 0.5001D,
                origin.getY() + (cell[1] + cell[4]) * 0.5D + face.getStepY() * 0.5001D,
                origin.getZ() + (cell[2] + cell[5]) * 0.5D + face.getStepZ() * 0.5001D);
        return level.hasChunkAt(sample)
                ? net.minecraft.client.renderer.LevelRenderer.getLightColor(level, sample) : fallback;
    }

    /** Texture fraction covered by one cell, following the vertex UV orientation in the face writer. */
    private static float[] cellUv(net.minecraft.core.Direction face, float[] cell,
                                  float x0, float y0, float z0, float x1, float y1, float z1) {
        float hLow = horizontalFraction(face, cell[0], cell[1], cell[2], x0, y0, z0, x1, y1, z1);
        float hHigh = horizontalFraction(face, cell[3], cell[4], cell[5], x0, y0, z0, x1, y1, z1);
        float vLow = verticalFraction(face, cell[0], cell[1], cell[2], x0, y0, z0, x1, y1, z1);
        float vHigh = verticalFraction(face, cell[3], cell[4], cell[5], x0, y0, z0, x1, y1, z1);
        boolean flipH = face == net.minecraft.core.Direction.NORTH || face == net.minecraft.core.Direction.EAST;
        boolean flipV = face != net.minecraft.core.Direction.UP && face != net.minecraft.core.Direction.DOWN;
        return new float[]{flipH ? hHigh : hLow, flipH ? hLow : hHigh,
                flipV ? vHigh : vLow, flipV ? vLow : vHigh};
    }

    private static float horizontalFraction(net.minecraft.core.Direction face, float x, float y, float z,
                                            float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case NORTH -> 1.0F - (x - x0) / (x1 - x0);
            case SOUTH -> (x - x0) / (x1 - x0);
            case WEST -> (z - z0) / (z1 - z0);
            case EAST -> 1.0F - (z - z0) / (z1 - z0);
            default -> (x - x0) / (x1 - x0);
        };
    }

    private static float verticalFraction(net.minecraft.core.Direction face, float x, float y, float z,
                                          float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case UP -> (z - z0) / (z1 - z0);
            case DOWN -> 1.0F - (z - z0) / (z1 - z0);
            default -> 1.0F - (y - y0) / (y1 - y0);
        };
    }

    @Override public boolean shouldRenderOffScreen(LargeCrucibleControllerBlockEntity be) { return true; }
//? if >=1.21.1 {
    /*@Override public net.minecraft.world.phys.AABB getRenderBoundingBox(LargeCrucibleControllerBlockEntity be) {
        return be.getRenderBoundingBox();
    }
*///?} else {
//?}
}
