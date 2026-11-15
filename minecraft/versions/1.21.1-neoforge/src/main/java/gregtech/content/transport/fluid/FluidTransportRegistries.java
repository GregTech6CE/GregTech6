package gregtech.content.transport.fluid;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** NeoForge capabilities for the shared tank/pipe catalog. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FluidTransportRegistries {
    private FluidTransportRegistries() {}

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, GTBlockEntities.TANK.get(), TankBlockEntity::capabilityHandler);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                GTBlockEntities.FLUID_PIPE.get(),
                FluidPipeBlockEntity::capabilityHandler);
    }
}
