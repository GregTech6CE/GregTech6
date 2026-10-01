package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.tool.CoinGeometry;
import com.gregtech.gregtech.item.CoinItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Retains the baked default coin, selecting the stack-aware renderer only for a stamped die. */
public final class CoinItemBakedModel implements BakedModel {
    private final BakedModel bakedDefault;
    private final boolean stamped;
    private final ItemOverrides overrides;

    public CoinItemBakedModel(BakedModel bakedDefault) {
        this.bakedDefault = bakedDefault;
        this.stamped = false;
        CoinItemBakedModel custom = new CoinItemBakedModel(bakedDefault, true);
        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack,
                                      @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                return stack.getItem() instanceof CoinItem && CoinGeometry.hasCustomPattern(stack)
                        ? custom : original;
            }
        };
    }

    private CoinItemBakedModel(BakedModel bakedDefault, boolean stamped) {
        this.bakedDefault = bakedDefault;
        this.stamped = stamped;
        this.overrides = ItemOverrides.EMPTY;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource random) {
        return stamped ? List.of() : bakedDefault.getQuads(state, side, random);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource random, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        return stamped ? List.of() : bakedDefault.getQuads(state, side, random, data, renderType);
    }

    @Override public boolean useAmbientOcclusion() { return bakedDefault.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return bakedDefault.isGui3d(); }
    @Override public boolean usesBlockLight() { return bakedDefault.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return stamped; }
    @Override public @NotNull TextureAtlasSprite getParticleIcon() { return bakedDefault.getParticleIcon(); }
    @Override public @NotNull ItemOverrides getOverrides() { return overrides; }
    @Override public @NotNull ItemTransforms getTransforms() { return bakedDefault.getTransforms(); }
    @Override public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random,
                                                                @NotNull ModelData data) {
        return bakedDefault.getRenderTypes(state, random, data);
    }
}
