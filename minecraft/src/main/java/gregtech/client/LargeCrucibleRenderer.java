package gregtech.client;

import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** GT6 MultiTileEntityCrucible render pass 5: molten/solid surface across the open 3x3 vessel. */
public final class LargeCrucibleRenderer implements BlockEntityRenderer<LargeCrucibleControllerBlockEntity> {
    /** Whole-vessel bounds; every face samples its texture fraction against these, not its own box. */
    private static final float HULL_MIN = -0.999F, HULL_MAX = 1.999F, HULL_HEIGHT = 3F;
    private static final float INNER_MIN = -0.5F, INNER_MAX = 1.5F;

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

    /** GT6 MultiTileEntityCrucible: four half-block walls and a 1.125-block floor. */
    private static void renderHull(LargeCrucibleControllerBlockEntity be, PoseStack pose,
                                   MultiBufferSource buffers, int fallbackLight) {
        float outer0 = HULL_MIN, outer1 = HULL_MAX, inner0 = INNER_MIN, inner1 = INNER_MAX;
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
        for (var face : faces) {
            String part = face == net.minecraft.core.Direction.UP ? "top"
                    : face == net.minecraft.core.Direction.DOWN ? "bottom" : "side";
            // GT6 MultiTileEntityCrucible keeps its default side UP with no rotatable sides.
            String front = face == net.minecraft.core.Direction.UP ? "_front" : "";
            float shade = be.getLevel() == null ? 1 : be.getLevel().getShade(face, true);
            var sprites = new net.minecraft.client.renderer.texture.TextureAtlasSprite[2];
            for (int layer = 0; layer < 2; layer++) {
                sprites[layer] = CrucibleRenderSprites.resolve(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "gt6ce", "block/machines/multiblockmains/crucible/"
                                + (layer == 0 ? "colored" : "overlay") + front + "/" + part));
            }
            // A wall spans three blocks. Split every face on block boundaries and light each
            // corner from the four blocks just outside it; cells sharing a corner get the same
            // value, so the wall stays continuous while one adjacent block only dims its corner.
            var cells = faceCells(face, x0, y0, z0, x1, y1, z1);
            for (float[] cell : cells) {
                // Normalize against the whole hull, not this box: the four rim strips are one
                // top surface, so their texture has to line up across the corners.
                float[] uv = cellUv(face, cell, HULL_MIN, 0, HULL_MIN, HULL_MAX, HULL_HEIGHT, HULL_MAX);
                int[] lights = cornerLights(be, face, cell, fallbackLight);
                for (int layer = 0; layer < 2; layer++) {
                    var sprite = sprites[layer];
                    float su0 = sprite.getU0(), su1 = sprite.getU1(), sv0 = sprite.getV0(), sv1 = sprite.getV1();
                    int rgb = layer == 0 ? tint : 0xFFFFFF;
                    LargeCrucibleHullFace.draw(pose.last(), out, face,
                            cell[0], cell[1], cell[2], cell[3], cell[4], cell[5],
                            su0 + uv[0] * (su1 - su0), su0 + uv[1] * (su1 - su0),
                            sv0 + uv[2] * (sv1 - sv0), sv0 + uv[3] * (sv1 - sv0),
                            rgb, shade, lights);
                }
            }
        }
    }

    /** One cell per block in the face plane; each inherits its own texture fraction and corner lights. */
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

    /** One light per face corner, sampled from the four blocks meeting just outside it. */
    private static int[] cornerLights(LargeCrucibleControllerBlockEntity be, net.minecraft.core.Direction face,
                                      float[] cell, int fallback) {
        var corners = LargeCrucibleHullFace.corners(face, cell[0], cell[1], cell[2], cell[3], cell[4], cell[5]);
        var lights = new int[4];
        for (int i = 0; i < 4; i++) lights[i] = cornerLight(be, face, corners[i], fallback);
        return lights;
    }

    /**
     * The half-block offset reaches the neighbour layer outside the hull, and the quarter offsets
     * pick the four blocks whose corner this vertex is. Adjacent cells therefore compute the same
     * shared value, which smooths the whole wall instead of lighting it per block.
     */
    private static int cornerLight(LargeCrucibleControllerBlockEntity be, net.minecraft.core.Direction face,
                                   float[] corner, int fallback) {
        var level = be.getLevel();
        if (level == null) return fallback;
        var origin = be.getBlockPos();
        double x = origin.getX() + corner[0] + face.getStepX() * 0.5001D;
        double y = origin.getY() + corner[1] + face.getStepY() * 0.5001D;
        double z = origin.getZ() + corner[2] + face.getStepZ() * 0.5001D;
        int sky = 0, block = 0, count = 0;
        for (int a = -1; a <= 1; a += 2) {
            for (int b = -1; b <= 1; b += 2) {
                double sx = x, sy = y, sz = z;
                switch (face.getAxis()) {
                    case X -> { sy = y + a * 0.25D; sz = z + b * 0.25D; }
                    case Y -> { sx = x + a * 0.25D; sz = z + b * 0.25D; }
                    case Z -> { sx = x + a * 0.25D; sy = y + b * 0.25D; }
                }
                var sample = net.minecraft.core.BlockPos.containing(sx, sy, sz);
                if (!level.hasChunkAt(sample)) continue;
                int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, sample);
                sky += net.minecraft.client.renderer.LightTexture.sky(light);
                block += net.minecraft.client.renderer.LightTexture.block(light);
                count++;
            }
        }
        if (count == 0) return fallback;
        return net.minecraft.client.renderer.LightTexture.pack(
                (block + count / 2) / count, (sky + count / 2) / count);
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
