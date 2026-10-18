package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.mod.GregTechIdentity;
import com.gregtech.gregtech.registry.GTManualStations;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Original rope material colors; the detail overlay keeps its own color. */
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RopeClientColors {
    private RopeClientColors() {}

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var holder : GTManualStations.ROPES) {
            var rope = holder.get();
            event.register((state, level, pos, tint) -> tint == 0 ? rope.tintRgb() : 0xFFFFFF, rope);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var holder : GTManualStations.ROPES) {
            var rope = holder.get();
            event.register(ItemColorARGB.opaque((stack, tint) -> tint == 0 ? rope.tintRgb() : 0xFFFFFF), rope.asItem());
        }
    }
}
