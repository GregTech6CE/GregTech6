package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.CrucibleCrossingBlockEntity;
import com.gregtech.gregtech.blockentity.machine.CrucibleFaucetBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BurningBoxBlockEntity;
import com.gregtech.gregtech.blockentity.machine.HopperBlockEntity;
import com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.machine.QueueHopperBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.block.machine.BurningBoxBlock;
import com.gregtech.gregtech.block.machine.CrucibleCrossingBlock;
import com.gregtech.gregtech.block.machine.CrucibleFaucetBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.block.machine.HopperBlock;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.machine.MoldBasinBlock;
import com.gregtech.gregtech.block.machine.MoldBlock;
import com.gregtech.gregtech.block.machine.QueueHopperBlock;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GTBlockEntities {
    private GTBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GregTech.MODID);

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity>> SIGNAL_WIRE =
            BLOCK_ENTITY_TYPES.register("signal_wire", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity::new,
                    GTSignalWires.all().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<ElectricWireBlockEntity>> ELECTRIC_WIRE =
            BLOCK_ENTITY_TYPES.register("electric_wire", () -> BlockEntityType.Builder
                    .of(ElectricWireBlockEntity::new, GTWires.all().stream()
                            .filter(RegistryObject::isPresent)
                            .map(RegistryObject::get)
                            .toArray(Block[]::new))
                    .build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.AxleBlockEntity>> AXLE =
            BLOCK_ENTITY_TYPES.register("axle", () -> BlockEntityType.Builder
                    .of(com.gregtech.gregtech.blockentity.energy.AxleBlockEntity::new,
                            GTAxles.all().stream()
                                    .filter(RegistryObject::isPresent)
                                    .map(RegistryObject::get)
                                    .toArray(Block[]::new))
                    .build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity>> GEARBOX =
            BLOCK_ENTITY_TYPES.register("gearbox", () -> BlockEntityType.Builder
                    .of(com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity::new,
                            GTGearboxes.allGearboxes().stream()
                                    .filter(RegistryObject::isPresent)
                                    .map(RegistryObject::get)
                                    .toArray(Block[]::new))
                    .build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.PumpBlockEntity>> PUMP =
            BLOCK_ENTITY_TYPES.register("pump", () -> BlockEntityType.Builder
                    .of(com.gregtech.gregtech.blockentity.energy.PumpBlockEntity::new,
                            GTPumps.all().stream()
                                    .filter(RegistryObject::isPresent)
                                    .map(RegistryObject::get)
                                    .toArray(Block[]::new))
                    .build(null));

    public static final RegistryObject<BlockEntityType<SolidBurningBoxBlockEntity>> SOLID_BURNING_BOX =
            BLOCK_ENTITY_TYPES.register("solid_burning_box", () -> {
                SolidBurningBoxBlock[] blocks = MachineRegistry.solidBurningBoxes().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(SolidBurningBoxBlock[]::new);
                return BlockEntityType.Builder.of(SolidBurningBoxBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<BurningBoxBlockEntity>> BURNING_BOX =
            BLOCK_ENTITY_TYPES.register("burning_box", () -> {
                BurningBoxBlock[] blocks = MachineRegistry.burningBoxes().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(BurningBoxBlock[]::new);
                return BlockEntityType.Builder.of(BurningBoxBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<SmeltingCrucibleBlockEntity>> SMELTING_CRUCIBLE =
            BLOCK_ENTITY_TYPES.register("smelting_crucible", () -> {
                SmeltingCrucibleBlock[] blocks = MachineRegistry.smeltingCrucibles().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(SmeltingCrucibleBlock[]::new);
                return BlockEntityType.Builder.of(SmeltingCrucibleBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<MoldBlockEntity>> MOLD =
            BLOCK_ENTITY_TYPES.register("mold", () -> {
                MoldBlock[] blocks = MachineRegistry.molds().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(MoldBlock[]::new);
                return BlockEntityType.Builder.of(MoldBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<MoldBasinBlockEntity>> MOLD_BASIN =
            BLOCK_ENTITY_TYPES.register("mold_basin", () -> {
                MoldBasinBlock[] blocks = MachineRegistry.moldBasins().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(MoldBasinBlock[]::new);
                return BlockEntityType.Builder.of(MoldBasinBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<CrucibleCrossingBlockEntity>> CRUCIBLE_CROSSING =
            BLOCK_ENTITY_TYPES.register("crucible_crossing", () -> {
                CrucibleCrossingBlock[] blocks = MachineRegistry.crucibleCrossings().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(CrucibleCrossingBlock[]::new);
                return BlockEntityType.Builder.of(CrucibleCrossingBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<CrucibleFaucetBlockEntity>> CRUCIBLE_FAUCET =
            BLOCK_ENTITY_TYPES.register("crucible_faucet", () -> {
                CrucibleFaucetBlock[] blocks = MachineRegistry.crucibleFaucets().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(CrucibleFaucetBlock[]::new);
                return BlockEntityType.Builder.of(CrucibleFaucetBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITY_TYPES.register("fluid_pipe", () -> {
                FluidPipeBlock[] blocks = GTFluidPipes.all().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(FluidPipeBlock[]::new);
                return BlockEntityType.Builder.of(FluidPipeBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity>> CHEMICAL_BATTERY =
            BLOCK_ENTITY_TYPES.register("chemical_battery", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity::new,
                    GTChemicalBatteries.allRegistered().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity>> ENERGY_NODE =
            BLOCK_ENTITY_TYPES.register("energy_node", () -> {
                net.minecraft.world.level.block.Block[] blocks = java.util.stream.Stream.concat(
                                java.util.stream.Stream.concat(GTEnergyNodes.all().stream(), GTGearboxes.allTransformers().stream()),GTChemicalBatteries.legacy().stream())
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(net.minecraft.world.level.block.Block[]::new);
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity>> STEAM_BOILER =
            BLOCK_ENTITY_TYPES.register("steam_boiler", () -> {
                com.gregtech.gregtech.block.machine.BoilerTankBlock[] blocks = GTBoilers.all().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(com.gregtech.gregtech.block.machine.BoilerTankBlock[]::new);
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LargeBoilerControllerBlockEntity>> LARGE_BOILER =
            BLOCK_ENTITY_TYPES.register("large_boiler", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LargeBoilerControllerBlockEntity::new,
                    GTMultiblocks.LARGE_BOILER_MAIN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity>> ORIGINAL_LARGE_BOILER =
            BLOCK_ENTITY_TYPES.register("original_large_boiler", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity::new,
                    com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs.all().stream()
                            .map(variant -> com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(variant.originalId()))
                            .toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity>> LARGE_TURBINE =
            BLOCK_ENTITY_TYPES.register("large_turbine", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity::new,
                    com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions.registeredBlocks(true)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.PlacedReactorRodBlockEntity>> PLACED_REACTOR_ROD =
            BLOCK_ENTITY_TYPES.register("placed_reactor_rod", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.energy.PlacedReactorRodBlockEntity::new,
                    GTFuelRods.BLOCKS.values().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity>> REACTOR_CORE =
            BLOCK_ENTITY_TYPES.register("reactor_core", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity::new,
                    GTEnergyNodes.REACTOR_CORE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.ReactorCore2x2BlockEntity>> REACTOR_CORE_2X2 =
            BLOCK_ENTITY_TYPES.register("reactor_core_2x2", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.energy.ReactorCore2x2BlockEntity::new,
                    GTEnergyNodes.REACTOR_CORE_2X2.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity>> MULTIBLOCK_PORT =
            BLOCK_ENTITY_TYPES.register("multiblock_port", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity::new,
                    GTMultiblocks.parts().stream().map(RegistryObject::get)
                            .filter(block -> block instanceof com.gregtech.gregtech.block.machine.MultiblockPortBlock)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity>> VON_DA_GRAAGG =
            BLOCK_ENTITY_TYPES.register("von_da_graagg", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity::new,
                    com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17996)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity>> PROCESSING_TOOL =
            BLOCK_ENTITY_TYPES.register("processing_tool", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity::new,
                    GTToolBlocks.simpleBlocks().stream().map(RegistryObject::get).filter(b -> b instanceof com.gregtech.gregtech.block.tool.ProcessingToolBlock)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity>> MATERIAL_ANVIL =
            BLOCK_ENTITY_TYPES.register("material_anvil", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity::new,
                    GTToolBlocks.ANVILS.stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity>> USB_SWITCH =
            BLOCK_ENTITY_TYPES.register("usb_switch", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity::new,
                    GTStorage.USB_SWITCH.get(), GTStorage.HDD_SWITCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity>> BOTTLE_CRATE =
            BLOCK_ENTITY_TYPES.register("bottle_crate", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity::new,
                    GTStorage.BOTTLE_CRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity>> PORTABLE_CONTAINER =
            BLOCK_ENTITY_TYPES.register("portable_container", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity::new,
                    GTToolBlocks.PORTABLE.stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity>> MASS_STORAGE =
            BLOCK_ENTITY_TYPES.register("mass_storage", () -> {
                java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
                blocks.add(GTStorage.MASS_STORAGE.get());
                GTStorage.MASS_STORAGES.forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity::new,
                        blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE =
            BLOCK_ENTITY_TYPES.register("logistics_mass_storage", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity::new,
                    GTStorage.LOGISTICS_MASS_STORAGES.stream().map(RegistryObject::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity>> METAL_CHEST =
            BLOCK_ENTITY_TYPES.register("metal_chest", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity::new,
                    java.util.stream.Stream.of(GTStorage.METAL_CHESTS, GTStorage.REINFORCED_WOOD_CHESTS, GTLootChests.LOOT_CHESTS)
                            .flatMap(java.util.Collection::stream).map(RegistryObject::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity>> DRAWER_QUAD =
            BLOCK_ENTITY_TYPES.register("drawer_quad", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity::new,
                    GTStorage.DRAWERS.stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity>> SAFE =
            BLOCK_ENTITY_TYPES.register("safe", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity::new,
                    java.util.stream.Stream.concat(GTStorage.SAFES.stream(), GTStorage.KEY_SAFES.stream())
                            .map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.LockerBlockEntity>> LOCKER =
            BLOCK_ENTITY_TYPES.register("locker", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.LockerBlockEntity::new,
                    GTStorage.LOCKER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.EnderGarbageBlockEntity>> ENDER_GARBAGE =
            BLOCK_ENTITY_TYPES.register("ender_garbage", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.EnderGarbageBlockEntity::new,
                    GTStorage.ENDER_GARBAGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.EnderGarbageDumpBlockEntity>> ENDER_GARBAGE_DUMP =
            BLOCK_ENTITY_TYPES.register("ender_garbage_dump", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.inventory.EnderGarbageDumpBlockEntity::new,
                    GTStorage.ENDER_GARBAGE_DUMP.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity>> SENSOR =
            BLOCK_ENTITY_TYPES.register("sensor", () -> {
                com.gregtech.gregtech.block.sensor.SensorBlock[] blocks = GTSensors.all().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(com.gregtech.gregtech.block.sensor.SensorBlock[]::new);
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.DustFunnelBlockEntity>> DUST_FUNNEL =
            BLOCK_ENTITY_TYPES.register("dust_funnel", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.DustFunnelBlockEntity::new,
                    GTToolBlocks.DUST_FUNNEL.get()).build(null));

    /** GT6's bumbliary (multi-tile 32741) and advanced bumbliary (32007): both share this type. */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity>> DYNAMITE =
            BLOCK_ENTITY_TYPES.register("dynamite", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity::new,
                    GTToolBlocks.DYNAMITE.get(), GTToolBlocks.BOOMSTICK.get(), GTToolBlocks.DYNAMITE_STRONG.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity>> BUMBLIARY =
            BLOCK_ENTITY_TYPES.register("bumbliary", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity::new,
                    GTToolBlocks.BUMBLIARY.get(), GTToolBlocks.ADVANCED_BUMBLIARY.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity>> MANUAL_TOOL =
            BLOCK_ENTITY_TYPES.register("manual_tool", () -> {
                com.gregtech.gregtech.block.tool.ManualToolBlock[] blocks = GTToolBlocks.manual().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(com.gregtech.gregtech.block.tool.ManualToolBlock[]::new);
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITY_TYPES.register("item_pipe", () -> {
                ItemPipeBlock[] blocks = GTItemPipes.all().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(ItemPipeBlock[]::new);
                return BlockEntityType.Builder.of(ItemPipeBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<TankBlockEntity>> TANK =
            BLOCK_ENTITY_TYPES.register("tank", () -> {
                TankBlock[] blocks = GTTanks.all().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .filter(block -> !(block instanceof com.gregtech.gregtech.block.machine.LogisticsTankBlock))
                        .toArray(TankBlock[]::new);
                return BlockEntityType.Builder.of(TankBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LogisticsTankBlockEntity>> LOGISTICS_TANK =
            BLOCK_ENTITY_TYPES.register("logistics_tank", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LogisticsTankBlockEntity::new,
                    GTTanks.LOGISTICS_TANK.get()).build(null));

    public static final RegistryObject<BlockEntityType<HopperBlockEntity>> HOPPER =
            BLOCK_ENTITY_TYPES.register("hopper", () -> {
                HopperBlock[] blocks = MachineRegistry.hoppers().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(HopperBlock[]::new);
                return BlockEntityType.Builder.of(HopperBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<QueueHopperBlockEntity>> QUEUE_HOPPER =
            BLOCK_ENTITY_TYPES.register("queue_hopper", () -> {
                QueueHopperBlock[] blocks = MachineRegistry.queueHoppers().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .toArray(QueueHopperBlock[]::new);
                return BlockEntityType.Builder.of(QueueHopperBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.OreBlockEntity>> ORE =
            BLOCK_ENTITY_TYPES.register("ore", () -> {
                Block[] blocks = GTBlocks.allEntries().stream()
                        .filter(RegistryObject::isPresent)
                        .map(RegistryObject::get)
                        .filter(b -> b instanceof com.gregtech.gregtech.block.OreBlock)
                        .toArray(Block[]::new);
                return BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.OreBlockEntity::new, blocks).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.RockBlockEntity>> ROCK =
            BLOCK_ENTITY_TYPES.register("rock", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.RockBlockEntity::new,
                            GTBlocks.ROCK.get()).build(null));

    /** GT6's tree holes (rubber resin hole, tapped maple, tapped rainbowood). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.TreeHoleBlockEntity>> TREE_HOLE =
            BLOCK_ENTITY_TYPES.register("tree_hole", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.TreeHoleBlockEntity::new,
                            GTTreeHoles.blocks().toArray(Block[]::new)).build(null));

    /** GT6's sap bag: hangs on a tree hole and drains it. */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.SapBagBlockEntity>> SAP_BAG =
            BLOCK_ENTITY_TYPES.register("sap_bag", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.SapBagBlockEntity::new,
                            GTToolBlocks.SAP_BAG.get()).build(null));

    /**
     * GT6's miniature portals ({@code MultiTileEntityMiniPortalNether}, {@code ...End}) as far as the
     * dungeon rooms need them: the block entity keeps the key id ({@code NBT_KEY}) and the active flag
     * ({@code NBT_ACTIVE}) of GT6's portal.
     */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.DungeonPortalBlockEntity>> DUNGEON_PORTAL =
            BLOCK_ENTITY_TYPES.register("dungeon_portal", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.block.misc.DungeonPortalBlockEntity::new,
                            GTDungeonBlocks.PORTAL_NETHER.get(), GTDungeonBlocks.PORTAL_END.get()).build(null));

    /** GT6's book shelf family (MultiTileEntityBookShelf): 28 display slots per block. */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity>> BOOKSHELF =
            BLOCK_ENTITY_TYPES.register("bookshelf", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity::new,
                            GTDecorBlocks.allBookshelves().stream().map(RegistryObject::get)
                                    .toArray(Block[]::new)).build(null));

    /**
     * GT6's loose ingot, plate and gem-plate piles (multi-tiles 32084/32085/32086), which all share
     * {@code gregapi.tileentity.misc.MultiTileEntityPlaceable}: one stored stack ({@code NBT_VALUE}).
     */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.misc.PileBlockEntity>> PILE =
            BLOCK_ENTITY_TYPES.register("pile", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.misc.PileBlockEntity::new,
                            GTDecorBlocks.INGOT_PILE.get(), GTDecorBlocks.PLATE_PILE.get(),
                            GTDecorBlocks.PLATE_GEM_PILE.get()).build(null));

    /** GT6's coin pile (multi-tile 32700): sixteen faces of up to sixteen coins each. */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity>> COIN_PILE =
            BLOCK_ENTITY_TYPES.register("coin_pile", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity::new,
                            GTDecorBlocks.COIN_PILE.get()).build(null));

    /** GT6's sixteen-layer edible sandwich ({@code MultiTileEntitySandwich}). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity>> SANDWICH_BLOCK =
            BLOCK_ENTITY_TYPES.register("sandwich_block", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity::new,
                    GTDecorBlocks.SANDWICH_BLOCK.get()).build(null));

    /** GT6's drying construction foam ({@code MultiTileEntityCFoam}). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.CFoamBlockEntity>> CFOAM =
            BLOCK_ENTITY_TYPES.register("cfoam", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.CFoamBlockEntity::new,
                            GTDecorBlocks.CFOAM_FRESH.get()).build(null));

    /** GT6's fluid springs at bedrock (WorldgenFluidSpring). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.FluidSpringBlockEntity>> FLUID_SPRING =
            BLOCK_ENTITY_TYPES.register("fluid_spring", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.FluidSpringBlockEntity::new,
                            GTDecorBlocks.FLUID_SPRING.get()).build(null));

    /** GT6's berry bush (WorldgenBushes places it, players set its berry type). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.BushBlockEntity>> BUSH =
            BLOCK_ENTITY_TYPES.register("bush", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.BushBlockEntity::new,
                            GTBushes.BUSH.get()).build(null));

    /** GT6's wild bumblebee hive (WorldgenHives fills it with a comb, a princess and drones). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity>> BUMBLE_HIVE =
            BLOCK_ENTITY_TYPES.register("bumble_hive", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity::new,
                            GTDecorBlocks.BUMBLE_HIVE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.CokeOvenControllerBlockEntity>> COKE_OVEN =
            BLOCK_ENTITY_TYPES.register("coke_oven", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.CokeOvenControllerBlockEntity::new,
                    GTMultiblocks.COKE_OVEN_MAIN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity>> MULTIBLOCK_TANK =
            BLOCK_ENTITY_TYPES.register("multiblock_tank", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity::new,
                    java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(GTMultiblocks.TANK_3X3.get(), GTMultiblocks.TANK_5X5.get()),
                            com.gregtech.gregtech.content.multiblock.TankValveSpec.all().stream().map(
                                    spec -> com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(spec.originalId())))
                            .toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity>> LARGE_CRUCIBLE =
            BLOCK_ENTITY_TYPES.register("large_crucible", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity::new,
                    java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(GTMultiblocks.LARGE_CRUCIBLE_MAIN.get()),
                            com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.all().stream().map(
                                    spec -> com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(spec.originalId())))
                            .toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.CryoDistillationControllerBlockEntity>> CRYO_DISTILLATION =
            BLOCK_ENTITY_TYPES.register("cryo_distillation", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.CryoDistillationControllerBlockEntity::new,
                    GTMultiblocks.CRYO_DISTILLATION_MAIN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity>> LARGE_GAS_TURBINE =
            BLOCK_ENTITY_TYPES.register("large_gas_turbine", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity::new,
                    com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.registeredBlocks()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LargeDynamoControllerBlockEntity>> LARGE_DYNAMO =
            BLOCK_ENTITY_TYPES.register("large_dynamo", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LargeDynamoControllerBlockEntity::new,
                    com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions.registeredBlocks(false)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity>> LARGE_HEAT_EXCHANGER =
            BLOCK_ENTITY_TYPES.register("large_heat_exchanger", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity::new,
                    GTMultiblocks.HEAT_EXCHANGER_MAIN.get()).build(null));

    // Wave 49 BE types: F6.9-12 multiblocks
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity>> BEDROCK_DRILL =
            BLOCK_ENTITY_TYPES.register("bedrock_drill", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity::new,
                    GTMultiblocks.BEDROCK_DRILL_MAIN.get(),
                    com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17999)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LightningRodControllerBlockEntity>> LIGHTNING_ROD =
            BLOCK_ENTITY_TYPES.register("lightning_rod", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LightningRodControllerBlockEntity::new,
                    GTMultiblocks.LIGHTNING_ROD_MAIN.get(),
                    com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(17998)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity>> IMPLOSION_COMPRESSOR =
            BLOCK_ENTITY_TYPES.register("implosion_compressor", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity::new,
                    GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity>> FUSION_REACTOR =
            BLOCK_ENTITY_TYPES.register("fusion_reactor", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity::new,
                    GTMultiblocks.FUSION_REACTOR_MAIN.get()).build(null));

    /** GT6 17997: Logistics Core controller (the walls are existing multiblock port entities). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity>> LOGISTICS_CORE =
            BLOCK_ENTITY_TYPES.register("logistics_core", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity::new,
                    GTLasers.LOGISTICS_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity>> LOGISTICS_WIRE =
            BLOCK_ENTITY_TYPES.register("logistics_wire", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity::new,
                    GTIconSetBlocks.LOGISTICS_WIRE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.AdvancedButtonBlockEntity>> ADVANCED_BUTTON =
            BLOCK_ENTITY_TYPES.register("advanced_button", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.AdvancedButtonBlockEntity::new,
                    ForgeRegistries.BLOCKS.getValue(GregTech.id("advanced_button"))).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity>> COIN_MOLD =
            BLOCK_ENTITY_TYPES.register("coin_mold", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity::new,
                    ForgeRegistries.BLOCKS.getValue(GregTech.id("coin_mold"))).build(null));

    // Wave 45 BE types
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlockEntity>> ADVANCED_CRAFTING_TABLE =
            BLOCK_ENTITY_TYPES.register("advanced_crafting_table", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlockEntity::new,
                    GTMiscBlocks.ADVANCED_CRAFTING_TABLES.stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.ChargingCraftingTableBlockEntity>> CHARGING_CRAFTING_TABLE =
            BLOCK_ENTITY_TYPES.register("charging_crafting_table", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.block.misc.ChargingCraftingTableBlockEntity::new,
                    GTMiscBlocks.CHARGING_CRAFTING_TABLES.stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.AutoIgniterBlockEntity>> AUTO_IGNITER =
            BLOCK_ENTITY_TYPES.register("auto_igniter", () -> {
                java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
                GTMiscBlocks.autoTools().stream()
                        .filter(ro -> ro.isPresent() && ro.get() instanceof com.gregtech.gregtech.block.misc.AutoIgniterBlock)
                        .forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.block.misc.AutoIgniterBlockEntity::new,
                        blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.AutoHammerBlockEntity>> AUTO_HAMMER =
            BLOCK_ENTITY_TYPES.register("auto_hammer", () -> {
                java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
                GTMiscBlocks.autoTools().stream()
                        .filter(ro -> ro.isPresent() && ro.get() instanceof com.gregtech.gregtech.block.misc.AutoHammerBlock)
                        .forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        com.gregtech.gregtech.block.misc.AutoHammerBlockEntity::new,
                        blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.LongDistEndpointBlockEntity>> LONG_DIST_ENDPOINT =
            BLOCK_ENTITY_TYPES.register("long_dist_endpoint", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.block.misc.LongDistEndpointBlockEntity::new,
                    GTMiscBlocks.LONG_DIST_ENDPOINT_ITEM.get(), GTMiscBlocks.LONG_DIST_ENDPOINT_FLUID.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.LongDistanceTransformerBlockEntity>> LONG_DIST_TRANSFORMER =
            BLOCK_ENTITY_TYPES.register("long_dist_transformer", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.block.misc.LongDistanceTransformerBlockEntity::new,
                    GTMiscBlocks.LONG_DIST_TRANSFORMER_ULV.get(), GTMiscBlocks.LONG_DIST_TRANSFORMER_LV.get(),
                    GTMiscBlocks.LONG_DIST_TRANSFORMER_MV.get(), GTMiscBlocks.LONG_DIST_TRANSFORMER_ZPM.get(),
                    GTMiscBlocks.LONG_DIST_TRANSFORMER_UV.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.ExtenderBlockEntity>> EXTENDER =
            BLOCK_ENTITY_TYPES.register("extender", () -> BlockEntityType.Builder.of(
                    com.gregtech.gregtech.block.misc.ExtenderBlockEntity::new,
                    GTMiscBlocks.extenderBlocks()).build(null));
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.block.misc.FilterBlockEntity>> FILTER = BLOCK_ENTITY_TYPES.register("filter",()->BlockEntityType.Builder.of(com.gregtech.gregtech.block.misc.FilterBlockEntity::new,GTMiscBlocks.FILTER_ITEMS.get(),GTMiscBlocks.FILTER_FLUIDS.get(),GTMiscBlocks.FILTER_ITEMS_FLUIDS.get(),GTMiscBlocks.FILTER_OREDICT.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.LaserConverterBlockEntity>> LASER_CONVERTER =
            BLOCK_ENTITY_TYPES.register("laser_converter", () -> BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.LaserConverterBlockEntity::new,
                    GTLasers.all().stream().map(RegistryObject::get).filter(block -> block instanceof com.gregtech.gregtech.block.energy.LaserConverterBlock)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.LaserFiberBlockEntity>> LASER_FIBER =
            BLOCK_ENTITY_TYPES.register("laser_fiber", () -> BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.LaserFiberBlockEntity::new,GTLasers.LASER_FIBER_WIRE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.ZpmModuleBlockEntity>> ZPM_MODULE =
            BLOCK_ENTITY_TYPES.register("zpm_module", () -> BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.ZpmModuleBlockEntity::new,GTLasers.ZPM.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity>> ZPM_DISCHARGER =
            BLOCK_ENTITY_TYPES.register("zpm_discharger", () -> BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity::new,GTLasers.ZPM_DISCHARGER_BASIC.get(),GTLasers.ZPM_DISCHARGER_ADVANCED.get(),GTLasers.ZPM_DISCHARGER_ELITE.get()).build(null));
}
