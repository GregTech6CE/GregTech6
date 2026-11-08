package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

//? if >=1.21.1 {

/*import gregtech.platform.neoforge.fluid.FluidDisplayItem;
*///?} else {
import gregtech.content.item.FluidItem;
//?}
import gregtech.content.registry.GTFluidItems;
import gregtech.platform.Mc;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

//? if >=1.21.1 {
/*/^*
 * Registers the shared fluid_item model and aliases all {@link FluidDisplayItem} models.
 * <p>
 * Each fluid item gets a thin wrapper whose {@link ItemOverrides} returns
 * {@link FluidItemOverrideList}. That list lazily resolves the correct fluid
 * sprite at <em>render time</em> (not bake time), so the atlas is always ready.
 ^/
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
/**
 * Registers the shared fluid_item model and aliases all {@link FluidItem} models.
 * <p>
 * Each fluid item gets a thin wrapper whose {@link ItemOverrides} returns
 * {@link FluidItemOverrideList}. That list lazily resolves the correct fluid
 * sprite at <em>render time</em> (not bake time), so the atlas is always ready.
 */
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class FluidItemClientModels {
    private static final ResourceLocation SHARED_MODEL = Mc.gregtechId("item/fluid_item");

    private FluidItemClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        ClientModelEvents.registerAdditional(event, SHARED_MODEL);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        FluidItemOverrideList.clearCache();
        FluidAppearance.clearCache();
        BakedModelMap models = BakedModelMap.of(event.getModels());
        BakedModel shared = models.find(SHARED_MODEL);
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

//? if >=1.21.1 {
        /*for (var entry : gregtech.platform.neoforge.fluid.FluidRegistries.allItems().toList()) {
            if (!(entry.get() instanceof FluidDisplayItem)) {
*///?} else {
        for (var entry : GTFluidItems.allEntries()) {
            if (!(entry.get() instanceof FluidItem)) {
//?}
                continue;
            }
            ResourceLocation itemId = Mc.itemId(entry.get());
            if (itemId == null) {
                continue;
            }
            models.put(itemId, "inventory", wrapper);
        }
//? if >=1.21.1 {
    /*}

    @SubscribeEvent
    public static void registerColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for(var entry:gregtech.platform.neoforge.fluid.FluidRegistries.allItems().toList()) {
            if(!(entry.get() instanceof FluidDisplayItem item)) continue;
            event.register((stack,layer) -> {
                if(layer!=0) return 0xFFFFFF;
                if(gregtech.engine.fluid.FluidDisplayBinding.hasPayload(stack)) {
                    var actual=gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(stack);
                    if(!actual.isEmpty()) return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(actual.getFluid()).getTintColor(actual);
                }
                return FluidAppearance.appearance(item.fluidEntry()).tint();
            },item);
        }
    }
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        gregtech.engine.fluid.FluidDisplayBinding.setClientLookup(() -> {
            var client=net.minecraft.client.Minecraft.getInstance();
            if(client.level!=null) return client.level.registryAccess();
            return client.getConnection()==null?null:client.getConnection().registryAccess();
        });
*///?} else {
//?}
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
