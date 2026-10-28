package gregtech.client;

import gregtech.content.registry.GTRemainingDecor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class PanelClientSetup {
    private PanelClientSetup() {}

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block e) {
        for (var b : GTRemainingDecor.PANELS)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item e) {
        for (var b : GTRemainingDecor.PANELS)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
    }
}
