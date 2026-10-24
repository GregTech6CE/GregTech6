package gregtech.platform.neoforge.smeltery;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Item automation for ordinary crucibles; large crucibles retain their own port rules. */
@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SmelteryItemCapabilities {
    private SmelteryItemCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SmelteryRegistries.SMELTING_CRUCIBLE.get(),
                (crucible, side) -> crucible.itemInputHandler(side));
    }
}
