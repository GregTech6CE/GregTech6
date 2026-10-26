package gregtech.client;

import gregtech.content.registry.GTItemBarrels;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity;
import gregtech.content.material.Materials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
public final class StorageCrystalClientColors {
    private StorageCrystalClientColors() {}
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var holder : GTItemBarrels.ALL)
            event.register((state, level, pos, layer) -> layer == 0 ? holder.get().material().getColor() : 0xFFFFFF, holder.get());
        for (var holder : GTChemicalBatteries.allRegistered()) if (!holder.get().spec().barIndicator())
            event.register((state, level, pos, layer) -> holder.get().spec().tint(
                    level != null && pos != null && level.getBlockEntity(pos) instanceof ChemicalBatteryBlockEntity cell ? cell.stored() : 0), holder.get());
        for (String id : new String[]{"ender_garbage_dump", "ender_garbage_bin"})
            event.register((state, level, pos, layer) -> layer == 0 ? Materials.Steel.getColor() : 0xFFFFFF,
                    BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id)));
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        for (var holder : GTItemBarrels.ALL) {
//? if >=1.21.1 {
            /*event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? holder.get().material().getColor() : 0xFFFFFF), holder.get().asItem());
*///?} else {
            event.register((stack, layer) -> layer == 0 ? holder.get().material().getColor() : 0xFFFFFF, holder.get().asItem());
//?}
        }
        for (String id : new String[]{"ender_garbage_dump", "ender_garbage_bin"}) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
//? if >=1.21.1 {
            /*event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? Materials.Steel.getColor() : 0xFFFFFF), item);
*///?} else {
            event.register((stack, layer) -> layer == 0 ? Materials.Steel.getColor() : 0xFFFFFF, item);
//?}
        }
    }
}
