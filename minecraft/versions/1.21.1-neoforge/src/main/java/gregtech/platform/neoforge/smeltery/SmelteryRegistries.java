package gregtech.platform.neoforge.smeltery;

import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTSmeltery;
import net.neoforged.bus.api.IEventBus;

/** NeoForge capabilities for the shared smeltery catalogs. */
public final class SmelteryRegistries {
    private SmelteryRegistries() {}

    public static void register(ContentRegistrar registrar) {
        GTBurningBoxes.registerAll(registrar);
        GTSmeltery.register(registrar);
    }

    public static void submit(IEventBus bus) {
        bus.addListener(SmelteryRegistries::capabilities);
    }

    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.SOLID_BURNING_BOX.get(),
                (be, side) -> be.itemCapability(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.BURNING_BOX.get(),
                gregtech.content.blockentity.machine.BurningBoxBlockEntity::itemCapability);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                GTBlockEntities.BURNING_BOX.get(),
                gregtech.content.blockentity.machine.BurningBoxBlockEntity::fluidCapability);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                GTBlockEntities.HEAT_EXCHANGER.get(),
                gregtech.content.blockentity.machine.HeatExchangerBlockEntity::fluidCapability);
    }
}
