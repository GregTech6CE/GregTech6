package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.machine.PipeGeometry;
import com.gregtech.gregtech.api.machine.PipeModelData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic pipe/wire model implementing the GT6 connection look: arms are sized
 * by this block, thin pipes extend into the block of a strictly thicker
 * neighbor (the thick side draws nothing), insulated cables expose the wire
 * core on connection faces, and full-cube pipes show the end texture only on
 * connected faces. Neighbor thicknesses arrive via
 * {@link PipeModelData#NEIGHBOR_HALVES}.
 */
public final class PipeWireBakedModel implements IDynamicBakedModel {

    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private static final ChunkRenderTypeSet SOLID_CUTOUT =
            ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutoutMipped());
    /** Overlay quads float this many px above the body to avoid z-fighting. */
    private static final double OVERLAY_OFFSET = 0.02;
    /** End caps sit this many px inside the boundary so two opposing caps
     *  (stale/missing neighbor data on both sides) can never be coplanar. */
    private static final double CAP_INSET = 0.015;

    private final Map<Direction,ResourceLocation> faceTextures;
    private final double half;
    private final ResourceLocation sideTex;
    private final int sideTint;
    private final ResourceLocation endTex;
    private final int endTint;
    /** Sheath ring width in px drawn around cap/connection faces (insulated cables). */
    private final double capRing;
    @Nullable
    private final ResourceLocation overlayTex;

    private final Map<ResourceLocation, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();
    private final Map<Integer, List<BakedQuad>> itemQuads = new ConcurrentHashMap<>();

    public PipeWireBakedModel(double half,
                              ResourceLocation sideTex, int sideTint,
                              ResourceLocation endTex, int endTint,
                              double capRing,
                              @Nullable ResourceLocation overlayTex) {
        this(half,sideTex,sideTint,endTex,endTint,capRing,overlayTex,Map.of());
    }

    public PipeWireBakedModel(double half, ResourceLocation sideTex, int sideTint,
                              ResourceLocation endTex,int endTint,double capRing,
                              @Nullable ResourceLocation overlayTex,Map<Direction,ResourceLocation> faceTextures) {
        this.faceTextures=Map.copyOf(faceTextures);
        this.half = half;
        this.sideTex = sideTex;
        this.sideTint = sideTint;
        this.endTex = endTex;
        this.endTint = endTint;
        this.capRing = capRing;
        this.overlayTex = overlayTex;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (state != null) return buildQuads(state, side, data, renderType);
        int pass = renderType == null ? 0 : renderType == RenderType.solid() ? 1
                : renderType == RenderType.cutoutMipped() ? 2 : 3;
        int key = (side == null ? 6 : side.ordinal()) * 4 + pass;
        return itemQuads.computeIfAbsent(key, ignored -> {
            List<BakedQuad> quads = new ArrayList<>(buildQuads(null, side, ModelData.EMPTY, renderType));
            if (renderType == null && overlayTex != null)
                quads.addAll(buildQuads(null, side, ModelData.EMPTY, RenderType.cutoutMipped()));
            return List.copyOf(quads);
        });
    }

    private List<BakedQuad> buildQuads(@Nullable BlockState state, @Nullable Direction side,
                                      ModelData data, @Nullable RenderType renderType) {
        boolean overlayPass = renderType != null && renderType == RenderType.cutoutMipped();
        if (renderType != null && renderType != RenderType.solid() && !overlayPass) {
            return Collections.emptyList();
        }
        if (overlayPass && overlayTex == null) return Collections.emptyList();

        float[] halves = data.get(PipeModelData.NEIGHBOR_HALVES);
        List<BakedQuad> quads = new ArrayList<>();
        double lo = 8.0 - half, hi = 8.0 + half;
        boolean fullCube = half >= 8.0;

        for (Direction dir : Direction.values()) {
            boolean conn = connected(state, dir);
            double n = conn
                    ? (halves != null ? halves[dir.ordinal()] : PipeGeometry.NONE)
                    : PipeGeometry.NONE;

            // Core face: end texture where connected, side texture elsewhere.
            // Full-cube faces sit on the boundary and go into the culled bucket.
            Direction cull = fullCube ? dir : null;
            if (side == cull) {
                if (!overlayPass) {
                    if (conn) {
                        capFace(quads, dir, new double[]{lo, lo, lo, hi, hi, hi});
                    } else {
                        face(quads, dir, lo, lo, lo, hi, hi, hi, sideTex, sideTint);
                    }
                } else if (!conn) {
                    double g = OVERLAY_OFFSET;
                    face(quads, dir, lo - g, lo - g, lo - g, hi + g, hi + g, hi + g,
                            overlayTex, -1);
                }
            }

            if (!conn || fullCube) continue;
            double[] arm = PipeGeometry.armBox(dir, half, n);
            if (arm != null) {
                tube(quads, dir, arm, PipeGeometry.hasEndCap(n), side, overlayPass);
            } else {
                // virtual connector thinner than this pipe (steam engine socket):
                // render its stub reaching from the boundary to our core surface
                double[] stub = PipeGeometry.stubBox(dir, half, n);
                if (stub != null) tube(quads, dir, stub, true, side, overlayPass);
            }
        }
        return quads;
    }

    /** Tube segment: four side faces plus an optional inset end cap toward {@code dir}. */
    private void tube(List<BakedQuad> quads, Direction dir, double[] box, boolean cap,
                      @Nullable Direction side, boolean overlayPass) {
        if (side == null) {
            for (Direction f : Direction.values()) {
                if (f.getAxis() == dir.getAxis()) continue;
                if (!overlayPass) {
                    face(quads, f, box, sideTex, sideTint);
                } else {
                    double g = OVERLAY_OFFSET;
                    face(quads, f, new double[]{box[0] - g, box[1] - g, box[2] - g,
                            box[3] + g, box[4] + g, box[5] + g}, overlayTex, -1);
                }
            }
        }
        // End cap just inside the block boundary; culled against full blocks, and
        // inset so two opposing caps (stale data on both sides) are never coplanar.
        if (!overlayPass && cap && side == dir) {
            double[] capBox = box.clone();
            int axisIdx = dir.getAxis().ordinal(); // 0=x,1=y,2=z matches box layout
            if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                capBox[axisIdx + 3] -= CAP_INSET;
            } else {
                capBox[axisIdx] += CAP_INSET;
            }
            capFace(quads, dir, capBox);
        }
    }

    /**
     * Cap/connection face: plain end texture, or — for sheathed cables — the
     * end texture center framed by a {@link #capRing} px ring of the body
     * (rubber) texture.
     */
    private void capFace(List<BakedQuad> out, Direction dir, double[] box) {
        if (capRing <= 0) {
            face(out, dir, box, endTex, endTint);
            return;
        }
        int a1, a2; // cross-section axis indices into the box array (0=x,1=y,2=z)
        switch (dir.getAxis()) {
            case X -> { a1 = 1; a2 = 2; }
            case Y -> { a1 = 0; a2 = 2; }
            default -> { a1 = 0; a2 = 1; }
        }
        double lo1 = box[a1], hi1 = box[a1 + 3], lo2 = box[a2], hi2 = box[a2 + 3];
        double r = Math.min(capRing, Math.min(hi1 - lo1, hi2 - lo2) / 2.0);
        if (r <= 0) {
            face(out, dir, box, endTex, endTint);
            return;
        }
        face(out, dir, sub(box, a1, lo1 + r, hi1 - r, a2, lo2 + r, hi2 - r), endTex, endTint);
        face(out, dir, sub(box, a1, lo1, lo1 + r, a2, lo2, hi2), sideTex, sideTint);
        face(out, dir, sub(box, a1, hi1 - r, hi1, a2, lo2, hi2), sideTex, sideTint);
        face(out, dir, sub(box, a1, lo1 + r, hi1 - r, a2, lo2, lo2 + r), sideTex, sideTint);
        face(out, dir, sub(box, a1, lo1 + r, hi1 - r, a2, hi2 - r, hi2), sideTex, sideTint);
    }

    private static double[] sub(double[] box, int a1, double lo1, double hi1,
                                int a2, double lo2, double hi2) {
        double[] b = box.clone();
        b[a1] = lo1;
        b[a1 + 3] = hi1;
        b[a2] = lo2;
        b[a2 + 3] = hi2;
        return b;
    }

    private void face(List<BakedQuad> out, Direction face, double[] box,
                      ResourceLocation tex, int tint) {
        face(out, face, box[0], box[1], box[2], box[3], box[4], box[5], tex, tint);
    }

    private static boolean connected(BlockState state, Direction dir) {
        if (state == null) return PipeGeometry.itemConnected(dir);
        Property<?> prop = state.getBlock().getStateDefinition().getProperty(dir.getName());
        return prop instanceof BooleanProperty bool && state.getValue(bool);
    }

    // ── Quad emission ────────────────────────────────────────────────────────

    /** Emit one face of the px-coordinate box, position-mapped UVs (clamped to 0..16).
     *  Vertex order/UV mapping mirrors {@link ArmRenderHelper#drawCuboid}. */
    private void face(List<BakedQuad> out, Direction face,
                      double x0, double y0, double z0, double x1, double y1, double z1,
                      ResourceLocation tex, int tint) {
        TextureAtlasSprite sprite = sprite(faceTextures.getOrDefault(face,tex));
        // vertex positions (block units) + uv source coords, CCW from outside
        float ax = (float) (x0 / 16.0), ay = (float) (y0 / 16.0), az = (float) (z0 / 16.0);
        float bx = (float) (x1 / 16.0), by = (float) (y1 / 16.0), bz = (float) (z1 / 16.0);
        float[][] v;   // {x, y, z, uSrc(0..16), vSrc(0..16)}
        switch (face) {
            case DOWN -> v = new float[][]{
                    {ax, ay, az, p(x0), q(z0)}, {bx, ay, az, p(x1), q(z0)},
                    {bx, ay, bz, p(x1), q(z1)}, {ax, ay, bz, p(x0), q(z1)}};
            case UP -> v = new float[][]{
                    {ax, by, az, p(x0), p(z0)}, {ax, by, bz, p(x0), p(z1)},
                    {bx, by, bz, p(x1), p(z1)}, {bx, by, az, p(x1), p(z0)}};
            case NORTH -> v = new float[][]{
                    {ax, ay, az, p(x0), q(y0)}, {ax, by, az, p(x0), q(y1)},
                    {bx, by, az, p(x1), q(y1)}, {bx, ay, az, p(x1), q(y0)}};
            case SOUTH -> v = new float[][]{
                    {ax, ay, bz, q(x0), q(y0)}, {bx, ay, bz, q(x1), q(y0)},
                    {bx, by, bz, q(x1), q(y1)}, {ax, by, bz, q(x0), q(y1)}};
            case WEST -> v = new float[][]{
                    {ax, ay, az, p(z0), q(y0)}, {ax, ay, bz, p(z1), q(y0)},
                    {ax, by, bz, p(z1), q(y1)}, {ax, by, az, p(z0), q(y1)}};
            default -> v = new float[][]{ // EAST
                    {bx, ay, az, q(z0), q(y0)}, {bx, by, az, q(z0), q(y1)},
                    {bx, by, bz, q(z1), q(y1)}, {bx, ay, bz, q(z1), q(y0)}};
        }
        BakedQuad[] baked = new BakedQuad[1];
        QuadBakingVertexConsumer qb = new QuadBakingVertexConsumer(q -> baked[0] = q);
        qb.setSprite(sprite);
        qb.setDirection(face);
        qb.setTintIndex(tint);
        qb.setShade(true);
        float nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        for (float[] vert : v) {
            float u = u0 + (u1 - u0) * vert[3] / 16.0f;
            float w = v0 + (v1 - v0) * vert[4] / 16.0f;
            qb.vertex(vert[0], vert[1], vert[2])
                    .color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(u, w)
                    .uv2(0)
                    .normal(nx, ny, nz)
                    .endVertex();
        }
        if (baked[0] != null) out.add(baked[0]);
    }

    /** Clamp px coord into 0..16 (extended arms repeat the edge texels). */
    private static float p(double c) {
        return (float) Math.max(0.0, Math.min(16.0, c));
    }

    /** Inverted texture axis (Minecraft auto-UV mirrors some faces). */
    private static float q(double c) {
        return 16.0f - p(c);
    }

    private TextureAtlasSprite sprite(ResourceLocation id) {
        return sprites.computeIfAbsent(id, key -> Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(key));
    }

    // ── BakedModel boilerplate ───────────────────────────────────────────────

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return overlayTex != null ? SOLID_CUTOUT : SOLID;
    }

    @Override
    public boolean useAmbientOcclusion() { return true; }

    @Override
    public boolean isGui3d() { return true; }

    @Override
    public boolean usesBlockLight() { return true; }

    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() { return sprite(sideTex); }

    @Override
    public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
}
