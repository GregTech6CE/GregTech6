package com.gregtech.gregtech.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Wraps the shared fluid_item model, replacing all quads with the fluid's still-texture sprite. */
public final class FluidItemBakedModel implements BakedModel {
    private final BakedModel inner;
    private final TextureAtlasSprite fluidSprite;
    private final BoundedCache<BakedQuad, BakedQuad> remappedQuads = new BoundedCache<>(128);

    public FluidItemBakedModel(BakedModel inner, TextureAtlasSprite fluidSprite) {
        this.inner = inner;
        this.fluidSprite = fluidSprite;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return remap(inner.getQuads(state, direction, random));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return remap(inner.getQuads(state, direction, random, data, renderType));
    }

    private List<BakedQuad> remap(List<BakedQuad> source) {
        if (source.isEmpty() || fluidSprite == null) return source;
        List<BakedQuad> out = new ArrayList<>(source.size());
        for (BakedQuad q : source) {
            out.add(remappedQuads.computeIfAbsent(q, original -> {
                int[] verts = original.getVertices().clone();
                remapUv(verts, original.getSprite(), fluidSprite);
                return new BakedQuad(verts, original.getTintIndex(), original.getDirection(), fluidSprite, original.isShade(), original.hasAmbientOcclusion());
            }));
        }
        return out;
    }

    /** Remap UV0 from oldSprite atlas coords to fluidSprite atlas coords. */
    static void remapUv(int[] verts, TextureAtlasSprite from, TextureAtlasSprite to) {
        float u0 = from.getU0(), u1 = from.getU1(), v0 = from.getV0(), v1 = from.getV1();
        float du = u1 - u0, dv = v1 - v0;
        float tu0 = to.getU0(), tu1 = to.getU1(), tv0 = to.getV0(), tv1 = to.getV1();
        float tdu = tu1 - tu0, tdv = tv1 - tv0;

        for (int i = 0; i < 4; i++) {
            int base = i * 8;
            float u = Float.intBitsToFloat(verts[base + 4]);
            float v = Float.intBitsToFloat(verts[base + 5]);
            float relU = du > 0 ? (u - u0) / du : 0;
            float relV = dv > 0 ? (v - v0) / dv : 0;
            verts[base + 4] = Float.floatToRawIntBits(tu0 + relU * tdu);
            verts[base + 5] = Float.floatToRawIntBits(tv0 + relV * tdv);
        }
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return fluidSprite != null ? fluidSprite : inner.getParticleIcon();
    }

    @Override
    public boolean useAmbientOcclusion() { return inner.useAmbientOcclusion(); }
    @Override
    public boolean isGui3d() { return inner.isGui3d(); }
    @Override
    public boolean usesBlockLight() { return inner.usesBlockLight(); }
    @Override
    public boolean isCustomRenderer() { return inner.isCustomRenderer(); }
    @Override
    public ItemOverrides getOverrides() { return inner.getOverrides(); }
    @Override
    public ItemTransforms getTransforms() { return inner.getTransforms(); }
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return inner.getRenderTypes(state, rand, data);
    }
}
