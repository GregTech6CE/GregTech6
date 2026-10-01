package com.gregtech.gregtech.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Picks GT6 faucet hull geometry by {@code facing} (attachment side + culled face). */
public final class CrucibleFaucetBakedModel implements BakedModel {
    private final Map<Direction, BakedModel> byFacing;
    private final BakedModel itemModel;

    public CrucibleFaucetBakedModel(Map<Direction, BakedModel> byFacing, BakedModel itemModel) {
        this.byFacing = byFacing;
        this.itemModel = itemModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return delegate(state).getQuads(state, direction, random);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        return delegate(state).getQuads(state, direction, random, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return delegate(state).getRenderTypes(state, rand, data);
    }

    private BakedModel delegate(@Nullable BlockState state) {
        if (state == null || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return itemModel;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return byFacing.getOrDefault(facing, itemModel);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
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

    private TextureAtlasSprite cachedParticle;

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if (cachedParticle != null) {
            return cachedParticle;
        }
        for (BakedQuad quad : itemModel.getQuads(null, null, RandomSource.create(0L))) {
            String path = quad.getSprite().contents().name().getPath();
            if (!path.contains("_overlay") && !path.contains("/overlay")) {
                cachedParticle = quad.getSprite();
                return cachedParticle;
            }
        }
        return itemModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return itemModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemModel.getOverrides();
    }
}
