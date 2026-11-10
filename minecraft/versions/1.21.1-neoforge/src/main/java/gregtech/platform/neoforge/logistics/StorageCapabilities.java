package gregtech.platform.neoforge.logistics;

import gregtech.content.registry.GTBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class StorageCapabilities {
    private StorageCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.MASS_STORAGE.get(),
                (storage, side) -> storage.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.LOGISTICS_MASS_STORAGE.get(),
                (storage, side) -> storage.itemHandler(side));
    }
}
