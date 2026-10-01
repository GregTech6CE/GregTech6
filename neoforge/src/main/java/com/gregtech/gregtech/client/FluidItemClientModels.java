package com.gregtech.gregtech.client;


import com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Registers the shared fluid_item model and aliases all {@link FluidDisplayItem} models.
 * <p>
 * Each fluid item gets a thin wrapper whose {@link ItemOverrides} returns
 * {@link FluidItemOverrideList}. That list lazily resolves the correct fluid
 * sprite at <em>render time</em> (not bake time), so the atlas is always ready.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidItemClientModels {
    private static final ResourceLocation SHARED_MODEL = ResourceLocation.fromNamespaceAndPath("gregtech","item/fluid_item");

    private FluidItemClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(SHARED_MODEL));
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        FluidItemOverrideList.clearCache();
        FluidAppearance.clearCache();
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel shared = models.get(ModelResourceLocation.standalone(SHARED_MODEL));
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

        for (var entry : com.gregtech.gregtech.platform.neoforge.fluid.FluidRegistries.ITEMS.getEntries()) {
            if (!(entry.get() instanceof FluidDisplayItem)) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.get());
            if (itemId == null) {
                continue;
            }
            models.put(ModelResourceLocation.inventory(itemId), wrapper);
        }
    }

    @SubscribeEvent
    public static void registerColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for(var entry:com.gregtech.gregtech.platform.neoforge.fluid.FluidRegistries.ITEMS.getEntries()) {
            if(!(entry.get() instanceof FluidDisplayItem item)) continue;
            event.register((stack,layer) -> {
                if(layer!=0) return 0xFFFFFF;
                if(com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack)) {
                    var actual=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolveCurrent(stack);
                    if(!actual.isEmpty()) return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(actual.getFluid()).getTintColor(actual);
                }
                return FluidAppearance.appearance(item.fluidEntry()).tint();
            },item);
        }
    }
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        com.gregtech.gregtech.api.fluid.FluidDisplayBinding.setClientLookup(() -> {
            var client=net.minecraft.client.Minecraft.getInstance();
            if(client.level!=null) return client.level.registryAccess();
            return client.getConnection()==null?null:client.getConnection().registryAccess();
        });
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
