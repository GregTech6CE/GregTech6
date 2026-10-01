package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.item.FluidItem;
import com.gregtech.gregtech.registry.GTFluidItems;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Registers the shared fluid_item model and aliases all {@link FluidItem} models.
 * <p>
 * Each fluid item gets a thin wrapper whose {@link ItemOverrides} returns
 * {@link FluidItemOverrideList}. That list lazily resolves the correct fluid
 * sprite at <em>render time</em> (not bake time), so the atlas is always ready.
 */
@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidItemClientModels {
    private static final ResourceLocation SHARED_MODEL = GregTech.id("item/fluid_item");

    private FluidItemClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        event.register(SHARED_MODEL);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        FluidItemOverrideList.clearCache();
        FluidAppearance.clearCache();
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BakedModel shared = models.get(SHARED_MODEL);
        if (shared == null) {
            return;
        }

        // Thin wrapper that delegates everything to the shared model but provides
        // FluidItemOverrideList so sprites are resolved at render time.
        BakedModel wrapper = new DelegatingModel(shared) {
            @Override
            public @NotNull ItemOverrides getOverrides() {
                return FluidItemOverrideList.INSTANCE;
            }
        };

        for (var entry : GTFluidItems.ITEMS.getEntries()) {
            if (!(entry.get() instanceof FluidItem)) {
                continue;
            }
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.get());
            if (itemId == null) {
                continue;
            }
            models.put(new ModelResourceLocation(itemId, "inventory"), wrapper);
        }
    }

    /** Every method delegates to {@code inner}; subclasses override just what they need. */
    private static class DelegatingModel implements BakedModel {
        final BakedModel inner;

        DelegatingModel(BakedModel inner) {
            this.inner = inner;
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction,
                                                  @NotNull RandomSource random) {
            return inner.getQuads(state, direction, random);
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction,
                                                  @NotNull RandomSource random, @NotNull ModelData data,
                                                  @Nullable net.minecraft.client.renderer.RenderType renderType) {
            return inner.getQuads(state, direction, random, data, renderType);
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
        public @NotNull TextureAtlasSprite getParticleIcon() { return inner.getParticleIcon(); }

        @Override
        public @NotNull ItemOverrides getOverrides() { return inner.getOverrides(); }

        @Override
        public @NotNull ItemTransforms getTransforms() { return inner.getTransforms(); }

        @Override
        public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
                                                          @NotNull ModelData data) {
            return inner.getRenderTypes(state, rand, data);
        }
    }
}
