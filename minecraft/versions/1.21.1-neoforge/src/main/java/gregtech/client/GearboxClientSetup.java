package gregtech.client;

import gregtech.content.registry.GTGearboxes;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class GearboxClientSetup {
    private GearboxClientSetup() {}

    private static int color(gregtech.content.block.energy.GearboxBlock block, int layer) {
        return layer == 0 ? block.spec().material().getColor() : 0xFFFFFF;
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTGearboxes.allGearboxes()) {
            var block = entry.get();
            event.register((state, level, pos, layer) -> color(block, layer), block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTGearboxes.allGearboxes()) {
            var block = entry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> color(block, layer)), block.asItem());
        }
    }

    @SubscribeEvent
    public static void transformerBlocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTGearboxes.allTransformers()) {
            var block = entry.get();
            event.register(
                    (state, level, pos, layer) ->
                            layer == 0 ? block.spec().material().getColor() : 0xFFFFFF,
                    block);
        }
    }

    @SubscribeEvent
    public static void transformerItems(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTGearboxes.allTransformers()) {
            var block = entry.get();
            event.register(
                    ItemColorARGB.opaque((stack, layer) ->
                            layer == 0 ? block.spec().material().getColor() : 0xFFFFFF),
                    block.asItem());
        }
    }

    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        ItemTransforms transforms = BlockItemModelHelper.blockItemTransforms();
        var wire = ResourceLocation.fromNamespaceAndPath("gt6ce", "block/material_icons/metallic/wire");
        var rubber = ResourceLocation.fromNamespaceAndPath("gt6ce", "block/material_icons/rubber/pipeside");
        for (var entry : GTGearboxes.allGearboxes()) {
            var block = entry.get();
            var model = new GearboxBakedModel().itemTransforms(transforms);
            // Keep the baked static housing for item rendering.
            BlockItemClientModels.aliasWorldStates(models, block, model);
        }
    }
}
