package gregtech.client;

import gregtech.content.registry.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class SafeClientSetup {
    @SubscribeEvent
    public static void blocks(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) {
        for (var b : GTSafes.all())
            e.register((s, l, p, t) -> t == 0 ? b.get().material().getColor() : 0xFFFFFF, b.get());
    }

    @SubscribeEvent
    public static void items(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) {
        for (var b : GTSafes.all())
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().material().getColor() : 0xFFFFFF),
                    b.get().asItem());
    }
}
