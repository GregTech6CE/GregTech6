package gregtech.client;

import gregtech.registry.GTHeatExchangers;
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
public final class HeatExchangerClientColors {
    private HeatExchangerClientColors() {}
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTHeatExchangers.all()) event.register((state, level, pos, layer) -> layer == 0 ? entry.get().spec().material().getColor() : 0xffffff, entry.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTHeatExchangers.all()) {
//? if >=1.21.1 {
            /*event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? entry.get().spec().material().getColor() : 0xffffff), entry.get().asItem());
*///?} else {
            event.register((stack, layer) -> layer == 0 ? entry.get().spec().material().getColor() : 0xffffff, entry.get().asItem());
//?}
        }
    }
}
