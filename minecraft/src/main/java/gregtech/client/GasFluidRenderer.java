package gregtech.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import gregtech.content.registry.GTFluids;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Top-anchored gas rendering with vanilla-style corner smoothing.
 *
 * <p>Original GT6's {@code RendererBlockFluid} draws a gas fluid from {@code y = 1 - height} to
 * {@code y = 1}: the visible surface is the underside, and its corner heights are averaged with
 * the neighbouring cells just like water's surface. Vanilla's liquid renderer is bottom-up, so
 * gas cannot use it; this renderer keeps vanilla's averaging idea but anchors the surface at the
 * ceiling and only averages cells that actually contain the same gas, so a small pocket stays a
 * thin layer instead of collapsing to nothing at its open edges.</p>
 *
 * <p>Quads are emitted double-sided because the chunk builder hands us the shared translucent
 * buffer and gas pockets are small; this avoids gaps from winding order at negligible cost.</p>
 */
public final class GasFluidRenderer {
    private GasFluidRenderer() {}

    private static final float QUANTA = 8.0F;

    public static void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer vc, FluidState state) {
        var entry = GTFluids.entryForFluid(state.getType());
        if (entry == null) return;
        var visual = FluidAppearance.appearance(entry);
        TextureAtlasSprite sprite = ArmRenderHelper.getSprite(visual.texture());
        int tint = visual.tint();
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        int light = (level.getBrightness(LightLayer.SKY, pos) << 20)
                | (level.getBrightness(LightLayer.BLOCK, pos) << 4);

        Fluid fluid = state.getType();
        float own = state.getAmount() / QUANTA;
        float nw = corner(level, pos, fluid, -1, -1, own);
        float ne = corner(level, pos, fluid, 1, -1, own);
        float se = corner(level, pos, fluid, 1, 1, own);
        float sw = corner(level, pos, fluid, -1, 1, own);

        float x0 = pos.getX() & 15;
        float z0 = pos.getZ() & 15;
        float x1 = x0 + 1.0F;
        float z1 = z0 + 1.0F;
        float base = pos.getY() & 15;
        float yNW = base + 1.0F - nw;
        float yNE = base + 1.0F - ne;
        float ySE = base + 1.0F - se;
        float ySW = base + 1.0F - sw;
        float top = base + 1.0F;

        BlockState below = level.getBlockState(pos.below());
        if (!below.getFluidState().getType().isSame(fluid) && !below.isSolidRender(level, pos.below())) {
            // Visible underside: NW, SW, SE, NE.
            quad(vc, sprite, x0, yNW, z0, x0, ySW, z1, x1, ySE, z1, x1, yNE, z0,
                    r, g, b, light, 0.0F, -1.0F, 0.0F);
        }

        side(level, pos, vc, sprite, fluid, Direction.NORTH,
                x0, yNW, z0, x1, yNE, z0, top, r, g, b, light);
        side(level, pos, vc, sprite, fluid, Direction.SOUTH,
                x1, ySE, z1, x0, ySW, z1, top, r, g, b, light);
        side(level, pos, vc, sprite, fluid, Direction.WEST,
                x0, ySW, z1, x0, yNW, z0, top, r, g, b, light);
        side(level, pos, vc, sprite, fluid, Direction.EAST,
                x1, yNE, z0, x1, ySE, z1, top, r, g, b, light);
    }

    private static float corner(BlockAndTintGetter level, BlockPos pos, Fluid fluid, int dx, int dz, float own) {
        float sum = 0.0F;
        int count = 0;
        for (int ox : new int[]{0, dx}) {
            for (int oz : new int[]{0, dz}) {
                FluidState neighbour = level.getFluidState(pos.offset(ox, 0, oz));
                if (neighbour.getType().isSame(fluid)) {
                    sum += neighbour.getAmount() / QUANTA;
                    count++;
                }
            }
        }
        return count == 0 ? own : sum / count;
    }

    private static void side(BlockAndTintGetter level, BlockPos pos, VertexConsumer vc, TextureAtlasSprite sprite,
                             Fluid fluid, Direction direction,
                             float xA, float yA, float zA, float xB, float yB, float zB,
                             float top, float r, float g, float b, int light) {
        BlockPos target = pos.relative(direction);
        BlockState targetState = level.getBlockState(target);
        if (level.getFluidState(target).getType().isSame(fluid) || targetState.isSolidRender(level, target)) return;
        float nx = direction.getStepX();
        float nz = direction.getStepZ();
        // Bottom edge follows the two corner heights; top edge sits at the ceiling plane.
        quad(vc, sprite, xA, yA, zA, xB, yB, zB, xB, top, zB, xA, top, zA,
                r, g, b, light, nx, 0.0F, nz);
    }

    /**
     * Emits one quad in both winding orders. The texture is mapped along the quad's local axes,
     * which is enough for gas: its texture has no directional flow.
     */
    private static void quad(VertexConsumer vc, TextureAtlasSprite sprite,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, int light,
                             float nx, float ny, float nz) {
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        vertex(vc, x0, y0, z0, u0, v0, r, g, b, light, nx, ny, nz);
        vertex(vc, x1, y1, z1, u1, v0, r, g, b, light, nx, ny, nz);
        vertex(vc, x2, y2, z2, u1, v1, r, g, b, light, nx, ny, nz);
        vertex(vc, x3, y3, z3, u0, v1, r, g, b, light, nx, ny, nz);
        vertex(vc, x3, y3, z3, u0, v1, r, g, b, light, -nx, -ny, -nz);
        vertex(vc, x2, y2, z2, u1, v1, r, g, b, light, -nx, -ny, -nz);
        vertex(vc, x1, y1, z1, u1, v0, r, g, b, light, -nx, -ny, -nz);
        vertex(vc, x0, y0, z0, u0, v0, r, g, b, light, -nx, -ny, -nz);
    }

    private static void vertex(VertexConsumer vc, float x, float y, float z, float u, float v,
                               float r, float g, float b, int light,
                               float nx, float ny, float nz) {
//? if >=1.21.1 {
        /*vc.addVertex(x, y, z).setColor(r, g, b, 1.0F).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
*///?} else {
        vc.vertex(x, y, z).color(r, g, b, 1.0F).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
//?}
    }
}
