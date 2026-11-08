package gregtech.client;

import gregtech.content.transport.fluid.FluidTransportRegistries;
import java.util.Locale;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Original dynamic thin-to-thick pipe geometry, cover plates and material tint, client only. */
@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class FluidTransportClientSetup {
    private FluidTransportClientSetup() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FluidTransportRegistries.FLUID_PIPE.get(), PipeCoverRenderer::new);
        event.registerBlockEntityRenderer(FluidTransportRegistries.TANK.get(), TankCoverRenderer::new);
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : FluidTransportRegistries.pipes()) {
            var block = entry.get();
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        }
        for (var block : gregtech.content.registry.AddonPipeRegistration.fluidBlocks())
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        for (var entry : FluidTransportRegistries.tanks()) {
            var block = entry.get();
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : FluidTransportRegistries.pipes()) {
            var block = entry.get();
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        }
        for (var block : gregtech.content.registry.AddonPipeRegistration.fluidBlocks())
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        for (var entry : FluidTransportRegistries.tanks()) {
            var block = entry.get();
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        }
    }

    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        ItemTransforms transforms = BlockItemModelHelper.blockItemTransforms();
        for (var entry : FluidTransportRegistries.pipes()) {
            var block = entry.get();
            var spec = block.spec();
            String set = spec.material().getTextureSet().name().toLowerCase(Locale.ROOT);
            var model = new PipeWireBakedModel(
                            Math.max(1, spec.diameter() * 8),
                            id("block/material_icons/" + set + "/pipeside"),
                            0,
                            id("block/material_icons/" + set + "/pipe"
                                    + spec.size().name().toLowerCase(Locale.ROOT)),
                            0,
                            0,
                            null)
                    .itemTransforms(transforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
        for (var block : gregtech.content.registry.AddonPipeRegistration.fluidBlocks()) {
            var spec = block.spec();
            String set = spec.material().getTextureSet().name().toLowerCase(Locale.ROOT);
            var model = new PipeWireBakedModel(
                            Math.max(1, spec.diameter() * 8),
                            id("block/material_icons/" + set + "/pipeside"),
                            0,
                            id("block/material_icons/" + set + "/pipe"
                                    + spec.size().name().toLowerCase(Locale.ROOT)),
                            0,
                            0,
                            null)
                    .itemTransforms(transforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
    }
}
