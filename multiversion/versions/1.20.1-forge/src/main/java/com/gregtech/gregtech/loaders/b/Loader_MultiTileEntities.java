package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.*;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registers all GT6 machine blocks, wires, pipes, and tanks — following the original GT6 {@code Loader_MultiTileEntities} pattern. */
public record Loader_MultiTileEntities(IEventBus bus) implements IGTLoader {

    @Override
    public void run() {
        com.gregtech.gregtech.content.energy.WireDefinitions.register();
        com.gregtech.gregtech.registry.GTSignalWires.register();
        com.gregtech.gregtech.content.fluid.FluidPipeDefinitions.register();
        com.gregtech.gregtech.content.transport.ItemPipeDefinitions.register();
        com.gregtech.gregtech.content.fluid.TankDefinitions.register();
        registerMachines();
    }

    private void registerMachines() {
        GTMachines.bootstrap();
        GTBasicMachines.registerAll();
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
        GTMagnets.registerAll();
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
