package gregtech.client;

import gregtech.registry.GTLegacyEnergyDevices;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
//?}

@EventBusSubscriber(modid = "gt6ce", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LegacyEnergyDeviceClientColors {
    private LegacyEnergyDeviceClientColors() {}
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, layer) -> layer == 0 ? 0x808080 : 0xFFFFFF, GTLegacyEnergyDevices.MAGIC_FIELD_ABSORBER.get());
        for (var entry : GTLegacyEnergyDevices.POWER_CELLS)
            event.register((state, level, pos, layer) -> layer == 0 ? entry.get().spec().color() : 0xFFFFFF, entry.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
//? if >=1.21.1 {
        /*event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? 0x808080 : 0xFFFFFF), GTLegacyEnergyDevices.MAGIC_FIELD_ABSORBER.get().asItem());
        for (var entry : GTLegacyEnergyDevices.POWER_CELLS)
            event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? entry.get().spec().color() : 0xFFFFFF), entry.get().asItem());
*///?} else {
        event.register((stack, layer) -> layer == 0 ? 0x808080 : 0xFFFFFF, GTLegacyEnergyDevices.MAGIC_FIELD_ABSORBER.get().asItem());
        for (var entry : GTLegacyEnergyDevices.POWER_CELLS)
            event.register((stack, layer) -> layer == 0 ? entry.get().spec().color() : 0xFFFFFF, entry.get().asItem());
//?}
    }
}
