package gregtech.client;

import gregtech.content.registry.GTAxles;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class AxleClientSetup {
    private AxleClientSetup() {}

    private static int color(gregtech.content.block.energy.AxleBlock block, int layer) {
        return layer == 0 ? block.spec().material().getColor() : 0xFFFFFF;
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTAxles.all()) {
            var block = entry.get();
            event.register((state, level, pos, layer) -> color(block, layer), block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTAxles.all()) {
            var block = entry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> color(block, layer)), block.asItem());
        }
    }

    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        ItemTransforms transforms = BlockItemModelHelper.blockItemTransforms();
        for (var entry : GTAxles.all()) {
            var block = entry.get();
            var model = new AxleBakedModel(block.spec().halfThickness(), transforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
    }
}
