package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.misc.SourceExtenderBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.registry.GTRelaysFilters;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Original GregTechClient filter/Source extender colors on the actual Native registrations. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RelayFilterClientSetup {
    private RelayFilterClientSetup() {}

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var holder : GTRelaysFilters.FILTERS) {
            event.register((state, level, pos, layer) -> layer == 0 ? Materials.SteelGalvanized.getColor() : 0xFFFFFF, holder.get());
        }
        for (var holder : GTRelaysFilters.EXTENDERS) {
            if (holder.get() instanceof SourceExtenderBlock extender) {
                int color = extender.spec().material().getColor();
                event.register((state, level, pos, layer) -> layer == 0 ? color : 0xFFFFFF, extender);
            }
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var holder : GTRelaysFilters.FILTERS) {
            event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? Materials.SteelGalvanized.getColor() : 0xFFFFFF), holder.get().asItem());
        }
        for (var holder : GTRelaysFilters.EXTENDERS) {
            if (holder.get() instanceof SourceExtenderBlock extender) {
                int color = extender.spec().material().getColor();
                event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? color : 0xFFFFFF), extender.asItem());
            }
        }
    }
}
