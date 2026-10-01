package com.gregtech.gregtech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code mMeltDown}: passthrough hull tint via block color handler; adds fullbright when warning.
 */
public final class CrucibleBlockBakedModel implements BakedModel {
    private final BakedModel inner;

    public CrucibleBlockBakedModel(BakedModel inner) {
        this.inner = inner;
    }

    /** GT6 {@code tRGBaArray[0..2] = channel*2+50 / channel/2+50}. */
    public static int meltdownRgb(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) * 2 + 50);
        int g = Math.min(255, ((rgb >> 8) & 0xFF) * 2 + 50);
        int b = Math.min(255, ((rgb & 0xFF) / 2 + 50));
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return process(inner.getQuads(state, direction, random), ModelData.EMPTY);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return process(inner.getQuads(state, direction, random, data, renderType), data);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return inner.getRenderTypes(state, rand, data);
    }

    private List<BakedQuad> process(List<BakedQuad> source, ModelData data) {
        if (source.isEmpty() || !Boolean.TRUE.equals(data.get(CrucibleModelData.MELTDOWN))) {
            return source;
        }
        List<BakedQuad> out = new ArrayList<>(source.size());
        for (BakedQuad quad : source) {
            out.add(shouldGlow(quad) ? fullBright(quad) : quad);
        }
        return out;
    }

    private static boolean shouldGlow(BakedQuad quad) {
        String path = quad.getSprite().contents().name().getPath();
        return !path.contains("_overlay") && !path.contains("/overlay");
    }

    private static BakedQuad fullBright(BakedQuad quad) {
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            vertices[i * 8 + 6] = LightTexture.FULL_BRIGHT;
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return inner.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return inner.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return inner.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        ItemTransforms block = BlockItemModelHelper.blockItemTransforms();
        return block != ItemTransforms.NO_TRANSFORMS ? block : inner.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return inner.getOverrides();
    }
}
