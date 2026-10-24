package gregtech.platform.neoforge.logistics;

@net.neoforged.fml.common.EventBusSubscriber(
        modid = gregtech.internal.mod.GregTechIdentity.MOD_ID,
        bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class LongDistanceCapabilities {
    private LongDistanceCapabilities() {}

    @net.neoforged.bus.api.SubscribeEvent
    public static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LONG_DIST_ENDPOINT.get(),
                (be, side) -> be.itemHandler(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LONG_DIST_ENDPOINT.get(),
                (be, side) -> be.fluidHandler(side));
    }
}
