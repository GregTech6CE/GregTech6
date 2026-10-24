package gregtech.bootstrap.registration;

import gregtech.internal.machine.MachineRegistry;
import gregtech.bootstrap.InitializationStep;
import gregtech.registry.*;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registers all GT6 machine blocks, wires, pipes, and tanks — following the original GT6 {@code Loader_MultiTileEntities} pattern. */
public record MachineBlockBootstrap(IEventBus bus) implements InitializationStep {

    @Override
    public void run() {
        gregtech.content.energy.WireDefinitions.register();
        gregtech.registry.GTSignalWires.register();
        gregtech.content.fluid.FluidPipeDefinitions.register();
        gregtech.content.transport.ItemPipeDefinitions.register();
        gregtech.content.fluid.TankDefinitions.register();
        registerMachines();
    }

    private void registerMachines() {
        GTMachines.bootstrap();
        GTBasicMachines.registerAll(bus);
        GTEngines.registerAll();
        GTEnergyNodes.registerAll();
        GTBoilers.registerAll();
        GTAxles.registerAll();
        GTGearboxes.registerAll();
        GTPumps.registerAll();
        GTMultiblocks.registerAll();
        GTStorage.registerAll();
        GTLootChests.registerAll();
        GTSensors.registerAll();
        gregtech.registry.AddonSensorRegistration.register(bus);
        GTToolBlocks.registerAll();
        GTMiscBlocks.registerAll();
        GTWoods.registerAll();
        GTTreeHoles.registerAll();
        GTBushes.registerAll();
        GTElectricItems.registerAll();
        GTChemicalBatteries.registerAll();
        GTDecorBlocks.registerAll();
        GTTrackBlocks.registerAll();
        GTLasers.registerAll();
        // The dungeon: GT6's ten keys (MultiItemRandomTools:589-598) and the portal blocks of its two
        // portal rooms (DungeonChunkRoomPortalNether/End).
        GTDungeonKeys.registerAll();
        GTDungeonBlocks.registerAll();
        MachineRegistry.registerBasicMachineBeTypes(GTBlockEntities.BLOCK_ENTITY_TYPES);
        MachineRegistry.registerEngineBeTypes(GTBlockEntities.BLOCK_ENTITY_TYPES);
        GTBlockEntities.BLOCK_ENTITY_TYPES.register(bus);
        GTEnchantments.register(bus);
    }
}
