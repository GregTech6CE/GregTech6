package gregtech.platform.neoforge.smeltery;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Finite casting automation; large crucibles retain their own port rules. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SmelteryItemCapabilities {
    private SmelteryItemCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                SmelteryRegistries.SMELTING_CRUCIBLE.get(),
                (crucible, side) -> crucible.itemInputHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, SmelteryRegistries.MOLD.get(), (mold, side) -> mold.automation(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, SmelteryRegistries.MOLD.get(), (mold, side) -> mold.automation(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.MOLD_BASIN.get(),
                (mold, side) -> mold.automation(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.MOLD_BASIN.get(),
                (mold, side) -> mold.automation(side));
    }
}
