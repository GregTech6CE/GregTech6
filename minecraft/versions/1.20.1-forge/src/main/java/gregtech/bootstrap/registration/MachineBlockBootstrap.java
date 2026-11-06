package gregtech.bootstrap.registration;

import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.*;
import gregtech.engine.machine.MachineRegistry;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registers all GT6 machine blocks, wires, pipes, and tanks — following the original GT6 {@code Loader_MultiTileEntities} pattern. */
public record MachineBlockBootstrap(IEventBus bus) implements InitializationStep {

    @Override
    public void run() {
        gregtech.content.energy.WireDefinitions.register();
        gregtech.content.registry.GTSignalWires.register(new gregtech.platform.forge.ForgeContentRegistrar());
        gregtech.content.fluid.FluidPipeDefinitions.register(new gregtech.platform.forge.ForgeContentRegistrar());
        gregtech.content.transport.ItemPipeDefinitions.register(new gregtech.platform.forge.ForgeContentRegistrar());
        gregtech.content.fluid.TankDefinitions.register();
        registerMachines();
    }

    private void registerMachines() {
        GTMachines.bootstrap();
        GTBasicMachines.registerAll(bus);
        GTEngines.registerAll();
        var registrar = new gregtech.platform.forge.ForgeContentRegistrar();
        GTEnergyNodes.registerAll(registrar);
        GTBoilers.registerAll();
        GTAxles.registerAll(registrar);
        GTGearboxes.registerAll(registrar);
        GTPumps.registerAll();
        GTMultiblocks.registerAll();
        GTStorage.registerAll();
        GTLootChests.registerAll();
        GTSensors.registerAll();
        gregtech.content.registry.AddonSensorRegistration.register(bus);
        GTToolBlocks.registerAll(new gregtech.platform.forge.ForgeContentRegistrar());
        GTMiscBlocks.registerAll();
        GTWoods.registerAll();
        GTTreeHoles.registerAll();
        GTBushes.registerAll();
        GTElectricItems.register(new gregtech.platform.forge.ForgeContentRegistrar());
        GTChemicalBatteries.registerAll(registrar);
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
