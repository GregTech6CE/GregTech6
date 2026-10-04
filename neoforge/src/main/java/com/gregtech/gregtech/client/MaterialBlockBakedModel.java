package com.gregtech.gregtech.client;

import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Assigns {@code tintindex 0} on material template quads so {@link net.neoforged.neoforge.client.event.RegisterColorHandlersEvent}
 * can apply material RGB (same approach as crucible / burning-box hulls).
 */
public final class MaterialBlockBakedModel implements BakedModel {
    private final BakedModel inner;
    private final BoundedCache<BakedQuad, BakedQuad> tintedQuads = new BoundedCache<>(128);
    private final BoundedCache<BakedModel, BakedModel> itemPasses = new BoundedCache<>(8);

    public MaterialBlockBakedModel(BakedModel inner) {
        this.inner = inner;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return tintQuads(inner.getQuads(state, direction, random));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return tintQuads(inner.getQuads(state, direction, random, data, renderType));
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return inner.getRenderTypes(state, rand, data);
    }

    @Override
    public List<net.minecraft.client.renderer.RenderType> getRenderTypes(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
        return inner.getRenderTypes(stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
        return inner.getRenderPasses(stack, fabulous).stream()
                .map(pass -> pass == inner ? this : itemPasses.computeIfAbsent(pass, MaterialBlockBakedModel::new)).toList();
    }

    private List<BakedQuad> tintQuads(List<BakedQuad> source) {
        if (source.isEmpty()) {
            return source;
        }
        List<BakedQuad> out = null;
        for (int index = 0; index < source.size(); index++) {
            BakedQuad quad = source.get(index);
            if (quad.getTintIndex() != 0 && shouldTint(quad)) {
                if (out == null) out = new ArrayList<>(source);
                out.set(index, tintedQuads.computeIfAbsent(quad, MaterialBlockBakedModel::assignTintIndex));
            }
        }
        return out == null ? source : out;
    }

    private static boolean shouldTint(BakedQuad quad) {
        String path = quad.getSprite().contents().name().getPath();
        if (path.contains("iconsets/crate")) {
            return false;
        }
        // GT6 machine shells: colored layer tints, overlay + overlay_active stay literal.
        if (path.contains("/overlay") || path.contains("_overlay")) {
            return false;
        }
        if (path.contains("/colored/")) {
            return true;
        }
        if (path.contains("material_icons") && !path.contains("_overlay")) {
            return true;
        }
        return path.contains("block/material/") || path.contains("item/material/");
    }

    private static BakedQuad assignTintIndex(BakedQuad quad) {
        if (quad.getTintIndex() == 0) {
            return quad;
        }
        return new BakedQuad(quad.getVertices(), 0, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
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
