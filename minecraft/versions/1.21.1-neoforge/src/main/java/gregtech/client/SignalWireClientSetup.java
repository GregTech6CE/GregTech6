package gregtech.client;

import gregtech.content.registry.GTSignalWires;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class SignalWireClientSetup {

    private SignalWireClientSetup() {}

    private static int color(gregtech.content.block.energy.SignalWireBlock block, int layer) {
        return gregtech.content.energy.SignalWireColors.tint(block.material(), layer);
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var wireEntry : GTSignalWires.all()) {
            var block = wireEntry.get();
            event.register((wireState, level, pos, layer) -> color(block, layer), block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var wireEntry : GTSignalWires.all()) {
            var block = wireEntry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> color(block, layer)), block.asItem());
        }
    }

    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        ItemTransforms fallbackTransforms = BlockItemModelHelper.blockItemTransforms();
        var wireTexture = ResourceLocation.fromNamespaceAndPath("gt6ce", "block/material_icons/metallic/wire");
        var rubberTexture = ResourceLocation.fromNamespaceAndPath("gt6ce", "block/material_icons/rubber/pipeside");
        for (var wireEntry : GTSignalWires.all()) {
            var block = wireEntry.get();
            var model = (block.insulated()
                            ? new PipeWireBakedModel(
                                    2,
                                    ResourceLocation.fromNamespaceAndPath("gt6ce", "block/iconsets/insulation_full"),
                                    1,
                                    wireTexture,
                                    0,
                                    1,
                                    null)
                            : new PipeWireBakedModel(1, wireTexture, 0, wireTexture, 0, 0, null))
                    .itemTransforms(fallbackTransforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
    }
}
