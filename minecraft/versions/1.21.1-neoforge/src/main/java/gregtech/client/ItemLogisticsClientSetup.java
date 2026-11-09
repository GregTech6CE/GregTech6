package gregtech.client;

import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTHoppers;
import gregtech.content.registry.GTItemPipes;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Original material tint, six-facing hopper geometry, pipe shape and covers. */
@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ItemLogisticsClientSetup {
    private ItemLogisticsClientSetup() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GTBlockEntities.ITEM_PIPE.get(), ItemPipeCoverRenderer::new);
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTItemPipes.all()) {
            var block = entry.get();
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        }
        for (var block : gregtech.content.registry.AddonPipeRegistration.itemBlocks())
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        for (var entry : GTHoppers.hoppers()) {
            var block = entry.get();
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        }
        for (var entry : GTHoppers.queues()) {
            var block = entry.get();
            event.register(
                    (state, level, pos, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF, block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTItemPipes.all()) {
            var block = entry.get();
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        }
        for (var block : gregtech.content.registry.AddonPipeRegistration.itemBlocks())
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        for (var entry : GTHoppers.hoppers()) {
            var block = entry.get();
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        }
        for (var entry : GTHoppers.queues()) {
            var block = entry.get();
            event.register(
                    ItemColorARGB.opaque(
                            (stack, layer) -> layer == 0 ? block.spec().tintRgb() : 0xFFFFFF),
                    block.asItem());
        }
    }

    public static void additional(ModelEvent.RegisterAdditional event) {
        for (String type : new String[] {"hopper", "queuehopper"})
            for (Direction face : Direction.values())
                ClientModelEvents.registerAdditional(
                        event, id("block/machine/" + type + "_" + face.getSerializedName()));
    }

    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        ItemTransforms transforms = BlockItemModelHelper.blockItemTransforms();
        for (var entry : GTItemPipes.all()) {
            var block = entry.get();
            var spec = block.spec();
            String set = spec.material().getTextureSet().name().toLowerCase(java.util.Locale.ROOT);
            String size = spec.size().name().toLowerCase(java.util.Locale.ROOT).replace("restrictive_", "");
            var overlay = spec.size().name().startsWith("RESTRICTIVE_") ? id("block/iconsets/pipe_restrictor") : null;
            var model = new PipeWireBakedModel(
                            Math.max(1, spec.diameter() * 8),
                            id("block/material_icons/" + set + "/pipeside"),
                            0,
                            id("block/material_icons/" + set + "/pipe" + size),
                            0,
                            0,
                            overlay)
                    .itemTransforms(transforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
        for (var block : gregtech.content.registry.AddonPipeRegistration.itemBlocks()) {
            var spec = block.spec();
            String set = spec.material().getTextureSet().name().toLowerCase(java.util.Locale.ROOT);
            String size = spec.size().name().toLowerCase(java.util.Locale.ROOT).replace("restrictive_", "");
            var overlay = spec.size().name().startsWith("RESTRICTIVE_") ? id("block/iconsets/pipe_restrictor") : null;
            var model = new PipeWireBakedModel(
                            Math.max(1, spec.diameter() * 8),
                            id("block/material_icons/" + set + "/pipeside"),
                            0,
                            id("block/material_icons/" + set + "/pipe" + size),
                            0,
                            0,
                            overlay)
                    .itemTransforms(transforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
        for (String type : new String[] {"hopper", "queuehopper"}) {
            java.util.Map<Direction, BakedModel> faces = new java.util.EnumMap<>(Direction.class);
            for (Direction face : Direction.values()) {
                var model = models.find(id("block/machine/" + type + "_" + face.getSerializedName()));
                if (model != null) faces.put(face, model);
            }
            if (faces.size() != 6) {
                com.mojang.logging.LogUtils.getLogger()
                        .warn("[gregtech] Missing hopper facing models: {} {}/6", type, faces.size());
                continue;
            }
            var model = new HopperBakedModel(faces, faces.get(Direction.DOWN));
            java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
            if (type.equals("hopper")) GTHoppers.hoppers().forEach(e -> blocks.add(e.get()));
            else GTHoppers.queues().forEach(e -> blocks.add(e.get()));
            for (var block : blocks) {
                BlockItemClientModels.alias(models, block, model, model);
            }
        }
    }
}
