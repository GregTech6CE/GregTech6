package gregtech.content.transport.fluid;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

/** NeoForge sounds and capabilities for the shared tank/pipe catalog. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FluidTransportRegistries {
    private FluidTransportRegistries() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, REGISTRY_NAMESPACE);
    public static final DeferredHolder<SoundEvent, SoundEvent> WRENCH = SOUNDS.register(
            "wrench",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, "wrench")));

    public static void submit(IEventBus bus) {
        SOUNDS.register(bus);
    }

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
