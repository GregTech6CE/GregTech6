package com.gregtech.gregtech.client;


import com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GT6 {@code MultiTileEntityGearBox.getTexture2}: each face shows the gearbox
 * plate or axle plate, then optionally a gear that animates in its own direction.
 * These are the five original animated iconset sprites, tinted together by the
 * registered material block/item color handler.
 */
public final class GearboxBakedModel implements IDynamicBakedModel {
    private net.minecraft.client.renderer.block.model.ItemTransforms transforms=net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS;
    public GearboxBakedModel itemTransforms(net.minecraft.client.renderer.block.model.ItemTransforms value){transforms=value;return this;}
    @Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms(){return transforms;}
    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.fromNamespaceAndPath("gregtech","block/iconsets/gearbox"),
            ResourceLocation.fromNamespaceAndPath("gregtech","block/iconsets/gearbox_axle"),
            ResourceLocation.fromNamespaceAndPath("gregtech","block/iconsets/gear"),
            ResourceLocation.fromNamespaceAndPath("gregtech","block/iconsets/gear_clockwise"),
            ResourceLocation.fromNamespaceAndPath("gregtech","block/iconsets/gear_counterclockwise")
    };
    private static final ChunkRenderTypeSet LAYERS =
            ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutoutMipped());

    /** Only 5 sprites x 6 faces are ever baked, regardless of gear combinations. */
    private final Map<Integer, List<BakedQuad>> faceQuads = new ConcurrentHashMap<>();

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource random, ModelData data, @Nullable RenderType layer) {
        if (side == null) return List.of();
        if (layer != null && layer != RenderType.solid() && layer != RenderType.cutoutMipped())
            return List.of();

        // Fresh block items have no axle and no mounted gears, like GT6's
        // default item TileEntity. An installed block obtains its data from BE.
        Integer encoded = state == null ? null : data.get(GearboxBlockEntity.VISUAL_CONFIGURATION);
        Integer rotation = state == null ? null : data.get(GearboxBlockEntity.VISUAL_ROTATION);
        int config = encoded == null ? 0 : encoded;
        int gearMask = config & 63;
        int axis = (config >>> 6) & 3;
        int sideBit = 1 << side.ordinal();
        boolean gear = (gearMask & sideBit) != 0;
        boolean onAxis = axis == switch (side.getAxis()) {
            case X -> 1;
            case Y -> 2;
            case Z -> 3;
        };

        List<BakedQuad> base = face(onAxis ? 1 : 0, side);
        if (layer == RenderType.solid()) return base;
        if (!gear) return layer == null ? base : List.of();

        int visual = rotation == null ? 0 : rotation;
        int gearTexture = (visual & 64) == 0 ? 2 : (visual & sideBit) != 0 ? 3 : 4;
        List<BakedQuad> overlay = face(gearTexture, side);
        if (layer == RenderType.cutoutMipped()) return overlay;
        return List.of(base.get(0), overlay.get(0));
    }

    private List<BakedQuad> face(int texture, Direction side) {
        return faceQuads.computeIfAbsent(texture * 6 + side.ordinal(),
                ignored -> List.of(bakeFace(texture, side)));
    }

    private static BakedQuad bakeFace(int texture, Direction side) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(TEXTURES[texture]);
        // GT6 composes both textures in one pass. A tiny offset prevents depth
        // fighting when the two transparent textures are drawn in Forge layers.
        float d = texture >= 2 ? 0.0005f : 0f;
        float lo = -d, hi = 1f + d;
        float[][] vertices = switch (side) {
            case DOWN -> new float[][]{
                    {0, lo, 0, 0, 16}, {1, lo, 0, 16, 16},
                    {1, lo, 1, 16, 0}, {0, lo, 1, 0, 0}};
            case UP -> new float[][]{
                    {0, hi, 0, 0, 0}, {0, hi, 1, 0, 16},
                    {1, hi, 1, 16, 16}, {1, hi, 0, 16, 0}};
            case NORTH -> new float[][]{
                    {0, 0, lo, 0, 16}, {0, 1, lo, 0, 0},
                    {1, 1, lo, 16, 0}, {1, 0, lo, 16, 16}};
            case SOUTH -> new float[][]{
                    {0, 0, hi, 16, 16}, {1, 0, hi, 0, 16},
                    {1, 1, hi, 0, 0}, {0, 1, hi, 16, 0}};
            case WEST -> new float[][]{
                    {lo, 0, 0, 0, 16}, {lo, 0, 1, 16, 16},
                    {lo, 1, 1, 16, 0}, {lo, 1, 0, 0, 0}};
            case EAST -> new float[][]{
                    {hi, 0, 0, 16, 16}, {hi, 1, 0, 16, 0},
                    {hi, 1, 1, 0, 0}, {hi, 0, 1, 0, 16}};
        };
        QuadBakingVertexConsumer consumer = new QuadBakingVertexConsumer();
        consumer.setSprite(sprite);
        consumer.setDirection(side);
        consumer.setTintIndex(0);
        consumer.setShade(true);
        for (float[] vertex : vertices) {
            float u = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * vertex[3] / 16f;
            float v = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * vertex[4] / 16f;
            consumer.addVertex(vertex[0], vertex[1], vertex[2])
                    .setColor(1f, 1f, 1f, 1f)
                    .setUv(u, v)
                    .setLight(0)
                    .setNormal(side.getStepX(), side.getStepY(), side.getStepZ());
        }
        return consumer.bakeQuad();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return LAYERS;
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() {
        return Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(TEXTURES[0]);
    }
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
}
