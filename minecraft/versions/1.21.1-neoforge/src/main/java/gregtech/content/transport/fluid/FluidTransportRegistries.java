package gregtech.content.transport.fluid;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.TankBlock;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.machine.PipeSpec;
import gregtech.engine.machine.TankSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge registration and capabilities for the shared ordinary tank/pipe catalog. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FluidTransportRegistries {
    private FluidTransportRegistries() {}

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, REGISTRY_NAMESPACE);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, REGISTRY_NAMESPACE);
    public static final DeferredHolder<SoundEvent, SoundEvent> WRENCH = SOUNDS.register(
            "wrench",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, "wrench")));
    private static final List<RegisteredBlock<TankBlock>> TANKS = new ArrayList<>();
    private static final List<RegisteredBlock<FluidPipeBlock>> PIPES = new ArrayList<>();
    public static RegisteredBlock<TankBlock> WOOD_BARREL, DRUM_BRONZE, DRUM_STEEL;
    public static RegisteredBlock<FluidPipeBlock> PIPE_MEDIUM_STEEL;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK = ENTITIES.register(
            "tank",
            () -> BlockEntityType.Builder.of(
                            TankBlockEntity::new,
                            TANKS.stream().map(RegisteredBlock::get).toArray(Block[]::new))
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            ENTITIES.register(
                    "fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                                    FluidPipeBlockEntity::new,
                                    gregtech.content.registry.AddonPipeRegistration.fluidPipeBlocks(
                                            PIPES.stream().map(RegisteredBlock::get)))
                            .build(null));

    public static List<RegisteredBlock<TankBlock>> tanks() {
        return List.copyOf(TANKS);
    }

    public static List<RegisteredBlock<FluidPipeBlock>> pipes() {
        return List.copyOf(PIPES);
    }

    public static void register(ContentRegistrar registrar) {
        gregtech.engine.addon.AddonMachines.collectContributors();
        for (TankSpec spec : FluidTransportDefinitions.tanks()) {
            var block = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new TankBlock(spec, TankBlock.defaultProperties(spec)),
                    tank -> new FluidTransportBlockItem(tank, new Item.Properties().stacksTo(16)));
            TANKS.add(block);
            if (spec.id().equals("wood_barrel")) WOOD_BARREL = block;
            if (spec.id().equals("drum_bronze")) DRUM_BRONZE = block;
            if (spec.id().equals("drum_steel")) DRUM_STEEL = block;
        }
        for (PipeSpec spec : FluidTransportDefinitions.pipes()) {
            String owner = gregtech.engine.addon.AddonPipes.owner(spec.id());
            if (owner != null) {
                gregtech.content.registry.AddonPipeRegistration.queueFluid(registrar, owner, spec);
                continue;
            }
            var block = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new FluidPipeBlock(spec, FluidPipeBlock.defaultProperties(spec)),
                    pipe -> new FluidTransportBlockItem(pipe, new Item.Properties().stacksTo(stackSize(spec.size()))));
            PIPES.add(block);
            if (spec.id().equals("pipe_medium_steel")) PIPE_MEDIUM_STEEL = block;
        }
    }

    public static void submit(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
    }

    private static int stackSize(PipeSpec.PipeSize size) {
        return switch (size) {
            case TINY, SMALL -> 64;
            case MEDIUM -> 32;
            default -> 16;
        };
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TANK.get(), TankBlockEntity::capabilityHandler);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, FLUID_PIPE.get(), FluidPipeBlockEntity::capabilityHandler);
    }
}
