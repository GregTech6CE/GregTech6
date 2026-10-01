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

/**
 * GT6 crates: placed blocks show the wooden hull only; items show hull + material + overlay.
 * Particles always use the crate hull sprite.
 */
public final class CrateBlockBakedModel implements BakedModel {
    public enum DisplayMode {
        /** Placed crate: wooden hull only. */
        WORLD,
        /** Inventory / hand: hull + material + overlay. */
        ITEM
    }

    private final BakedModel inner;
    private final boolean itemContext;
    @Nullable
    private TextureAtlasSprite crateParticle;

    public CrateBlockBakedModel(BakedModel inner, int materialColor) {
        this(inner, materialColor, DisplayMode.WORLD);
    }

    public CrateBlockBakedModel(BakedModel inner, int materialColor, DisplayMode mode) {
        this.inner = inner;
        this.itemContext = mode == DisplayMode.ITEM;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return filterQuads(inner.getQuads(state, direction, random));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return filterQuads(inner.getQuads(state, direction, random, data, renderType));
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return inner.getRenderTypes(state, rand, data);
    }

    private List<BakedQuad> filterQuads(List<BakedQuad> source) {
        if (source.isEmpty() || itemContext) {
            return source;
        }
        List<BakedQuad> out = new ArrayList<>(source.size());
        for (BakedQuad quad : source) {
            String path = quad.getSprite().contents().name().getPath();
            if (path.contains("iconsets/crate")) {
                if (crateParticle == null) {
                    crateParticle = quad.getSprite();
                }
                out.add(quad);
                continue;
            }
            if (!itemContext) {
                continue;
            }
            out.add(quad);
        }
        return out;
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
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if (crateParticle != null) {
            return crateParticle;
        }
        for (BakedQuad quad : inner.getQuads(null, null, RandomSource.create(0L))) {
            String path = quad.getSprite().contents().name().getPath();
            if (path.contains("iconsets/crate")) {
                crateParticle = quad.getSprite();
                return crateParticle;
            }
        }
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
