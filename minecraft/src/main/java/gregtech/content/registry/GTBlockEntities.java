package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.energy.LaserConverterBlock;
import gregtech.content.block.machine.LogisticsTankBlock;
import gregtech.content.block.misc.AdvancedCraftingTableBlockEntity;
import gregtech.content.block.misc.AutoHammerBlockEntity;
import gregtech.content.block.misc.AutoIgniterBlockEntity;
import gregtech.content.block.misc.ChargingCraftingTableBlockEntity;
import gregtech.content.block.misc.DungeonPortalBlockEntity;
import gregtech.content.block.misc.ExtenderBlockEntity;
import gregtech.content.block.misc.FilterBlockEntity;
import gregtech.content.block.misc.LongDistEndpointBlockEntity;
import gregtech.content.block.misc.LongDistanceTransformerBlockEntity;
import gregtech.content.block.sensor.SensorBlock;
import gregtech.content.blockentity.BushBlockEntity;
import gregtech.content.blockentity.CFoamBlockEntity;
import gregtech.content.blockentity.FluidSpringBlockEntity;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.blockentity.SapBagBlockEntity;
import gregtech.content.blockentity.TreeHoleBlockEntity;
import gregtech.content.blockentity.energy.LaserConverterBlockEntity;
import gregtech.content.blockentity.energy.LaserFiberBlockEntity;
import gregtech.content.blockentity.energy.MagicFieldAbsorberBlockEntity;
import gregtech.content.blockentity.energy.PlacedReactorRodBlockEntity;
import gregtech.content.blockentity.energy.PowerCellBlockEntity;
import gregtech.content.blockentity.energy.PumpBlockEntity;
import gregtech.content.blockentity.energy.ReactorCore2x2BlockEntity;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.blockentity.energy.ZpmDischargerBlockEntity;
import gregtech.content.blockentity.energy.ZpmModuleBlockEntity;
import gregtech.content.blockentity.inventory.BookShelfBlockEntity;
import gregtech.content.blockentity.inventory.BottleCrateBlockEntity;
import gregtech.content.blockentity.inventory.DrawerQuadBlockEntity;
import gregtech.content.blockentity.inventory.EnderGarbageBlockEntity;
import gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity;
import gregtech.content.blockentity.inventory.LockerBlockEntity;
import gregtech.content.blockentity.inventory.LogisticsMassStorageBlockEntity;
import gregtech.content.blockentity.inventory.MassStorageBlockEntity;
import gregtech.content.blockentity.inventory.MetalChestBlockEntity;
import gregtech.content.blockentity.inventory.SafeBlockEntity;
import gregtech.content.blockentity.inventory.UsbSwitchBlockEntity;
import gregtech.content.blockentity.machine.AddonEnergyPartBlockEntity;
import gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity;
import gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity;
import gregtech.content.blockentity.machine.BoilerTankBlockEntity;
import gregtech.content.blockentity.machine.BurningBoxBlockEntity;
import gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity;
import gregtech.content.blockentity.machine.CrucibleCrossingBlockEntity;
import gregtech.content.blockentity.machine.CrucibleFaucetBlockEntity;
import gregtech.content.blockentity.machine.CryoDistillationControllerBlockEntity;
import gregtech.content.blockentity.machine.HeatExchangerBlockEntity;
import gregtech.content.blockentity.machine.HopperBlockEntity;
import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
import gregtech.content.blockentity.machine.LargeDynamoControllerBlockEntity;
import gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import gregtech.content.blockentity.machine.LargeHeatExchangerControllerBlockEntity;
import gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity;
import gregtech.content.blockentity.machine.LightningRodControllerBlockEntity;
import gregtech.content.blockentity.machine.LogisticsCoreControllerBlockEntity;
import gregtech.content.blockentity.machine.LogisticsTankBlockEntity;
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import gregtech.content.blockentity.machine.QueueHopperBlockEntity;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.content.blockentity.machine.SolidBurningBoxBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.blockentity.machine.VonDaGraaggControllerBlockEntity;
import gregtech.content.blockentity.misc.BumbleHiveBlockEntity;
import gregtech.content.blockentity.misc.PileBlockEntity;
import gregtech.content.blockentity.misc.SandwichBlockEntity;
import gregtech.content.blockentity.sensor.SensorBlockEntity;
import gregtech.content.blockentity.tool.AdvancedButtonBlockEntity;
import gregtech.content.blockentity.tool.BumbliaryBlockEntity;
import gregtech.content.blockentity.tool.DustFunnelBlockEntity;
import gregtech.content.blockentity.tool.DynamiteBlockEntity;
import gregtech.content.blockentity.tool.ManualToolBlockEntity;
import gregtech.content.blockentity.tool.MaterialAnvilBlockEntity;
import gregtech.content.blockentity.tool.PortableContainerBlockEntity;
import gregtech.content.blockentity.tool.ProcessingToolBlockEntity;
import gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity;
import gregtech.content.transport.pipe.item.ItemPipeBlockEntity;
import gregtech.engine.multiblock.tracking.TrackedMultiblock;
import gregtech.engine.sensor.AddonSensors;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import gregtech.content.block.OreBlock;
import gregtech.content.blockentity.OreBlockEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?}

/** Block entity types shared by both loaders; node-specific types stay in the per-loader branches. */
public final class GTBlockEntities {
//? if >=1.21.1 {
    /*public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, REGISTRY_NAMESPACE);
*///?} else {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, REGISTRY_NAMESPACE);
//?}

    // Shared registrations: both loaders queue into the same deferred register.
    public static final Supplier<BlockEntityType<AddonEnergyPartBlockEntity>> ADDON_ENERGY_PART =
            BLOCK_ENTITIES.register("addon_energy_part", () -> BlockEntityType.Builder.of(
                    AddonEnergyPartBlockEntity::new,
                    GTMultiblocks.energyPartBlocks()).build(null));

    public static final Supplier<BlockEntityType<AddonFunctionalPartBlockEntity>> ADDON_FUNCTIONAL_PART =
            BLOCK_ENTITIES.register("addon_functional_part", () -> BlockEntityType.Builder.of(
                    AddonFunctionalPartBlockEntity::new,
                    GTMultiblocks.functionalPartBlocks()).build(null));

    public static final Supplier<BlockEntityType<AdvancedButtonBlockEntity>> ADVANCED_BUTTON =
            BLOCK_ENTITIES.register("advanced_button", () -> BlockEntityType.Builder.of(
                    AdvancedButtonBlockEntity::new,
                    GTManualStations.ADVANCED_BUTTON.get()).build(null));

    public static final Supplier<BlockEntityType<AdvancedCraftingTableBlockEntity>> ADVANCED_CRAFTING_TABLE =
            BLOCK_ENTITIES.register("advanced_crafting_table", () -> BlockEntityType.Builder.of(
                    AdvancedCraftingTableBlockEntity::new,
                    GTCraftingTables.ADVANCED.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<AutoHammerBlockEntity>> AUTO_HAMMER =
            BLOCK_ENTITIES.register("auto_hammer", () -> BlockEntityType.Builder.of(
                    AutoHammerBlockEntity::new,
                    GTRelaysFilters.HAMMERS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<AutoIgniterBlockEntity>> AUTO_IGNITER =
            BLOCK_ENTITIES.register("auto_igniter", () -> BlockEntityType.Builder.of(
                    AutoIgniterBlockEntity::new,
                    GTRelaysFilters.IGNITERS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<BedrockDrillControllerBlockEntity>> BEDROCK_DRILL =
            registerMultiblock("bedrock_drill");

    public static final Supplier<BlockEntityType<BookShelfBlockEntity>> BOOKSHELF =
            BLOCK_ENTITIES.register("bookshelf", () -> BlockEntityType.Builder.of(
                    BookShelfBlockEntity::new,
                    GTBookShelves.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<BottleCrateBlockEntity>> BOTTLE_CRATE =
            BLOCK_ENTITIES.register("bottle_crate", () -> BlockEntityType.Builder.of(
                    BottleCrateBlockEntity::new,
                    GTStorageContainers.BOTTLE_CRATES.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<BumbleHiveBlockEntity>> BUMBLE_HIVE =
            BLOCK_ENTITIES.register("bumble_hive", () -> BlockEntityType.Builder.of(
                    BumbleHiveBlockEntity::new,
                    GTBumbleBlocks.allHives()).build(null));

    /** GT6's bumbliary (multi-tile 32741) and advanced bumbliary (32007): both share this type. */
    public static final Supplier<BlockEntityType<BumbliaryBlockEntity>> BUMBLIARY =
            BLOCK_ENTITIES.register("bumbliary", () -> BlockEntityType.Builder.of(
                    BumbliaryBlockEntity::new,
                    GTBumbleBlocks.BUMBLIARY.get(),
                    GTBumbleBlocks.ADVANCED_BUMBLIARY.get()).build(null));

    public static final Supplier<BlockEntityType<BurningBoxBlockEntity>> BURNING_BOX =
            BLOCK_ENTITIES.register("burning_box", () -> BlockEntityType.Builder.of(
                    BurningBoxBlockEntity::new,
                    GTBurningBoxes.fuelBoxes().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<BushBlockEntity>> BUSH =
            BLOCK_ENTITIES.register("bush", () -> BlockEntityType.Builder.of(
                    BushBlockEntity::new,
                    GTBushes.allBlocks()).build(null));

    public static final Supplier<BlockEntityType<CFoamBlockEntity>> CFOAM =
            BLOCK_ENTITIES.register("cfoam", () -> BlockEntityType.Builder.of(
                    CFoamBlockEntity::new,
                    GTConstructionVariants.wetFoams()).build(null));

    public static final Supplier<BlockEntityType<ChargingCraftingTableBlockEntity>> CHARGING_CRAFTING_TABLE =
            BLOCK_ENTITIES.register("charging_crafting_table", () -> BlockEntityType.Builder.of(
                    ChargingCraftingTableBlockEntity::new,
                    GTCraftingTables.CHARGING.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<CokeOvenControllerBlockEntity>> COKE_OVEN =
            registerMultiblock("coke_oven");

    public static final Supplier<BlockEntityType<CrucibleCrossingBlockEntity>> CRUCIBLE_CROSSING =
            BLOCK_ENTITIES.register("crucible_crossing", () -> BlockEntityType.Builder.of(
                    CrucibleCrossingBlockEntity::new,
                    GTSmeltery.crossings().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<CrucibleFaucetBlockEntity>> CRUCIBLE_FAUCET =
            BLOCK_ENTITIES.register("crucible_faucet", () -> BlockEntityType.Builder.of(
                    CrucibleFaucetBlockEntity::new,
                    GTSmeltery.faucets().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<CryoDistillationControllerBlockEntity>> CRYO_DISTILLATION =
            registerMultiblock("cryo_distillation");

    public static final Supplier<BlockEntityType<DrawerQuadBlockEntity>> DRAWER_QUAD =
            BLOCK_ENTITIES.register("drawer_quad", () -> BlockEntityType.Builder.of(
                    DrawerQuadBlockEntity::new,
                    GTStorageContainers.DRAWERS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    /**
     * GT6's miniature portals ({@code MultiTileEntityMiniPortalNether}, {@code ...End}) as far as the
     * dungeon rooms need them: the block entity keeps the key id ({@code NBT_KEY}) and the active flag
     * ({@code NBT_ACTIVE}) of GT6's portal.
     */
    public static final Supplier<BlockEntityType<DungeonPortalBlockEntity>> DUNGEON_PORTAL =
            BLOCK_ENTITIES.register("dungeon_portal", () -> BlockEntityType.Builder.of(
                    DungeonPortalBlockEntity::new,
                    GTDungeonBlocks.PORTAL_NETHER.get(),
                    GTDungeonBlocks.PORTAL_END.get()).build(null));

    public static final Supplier<BlockEntityType<DustFunnelBlockEntity>> DUST_FUNNEL =
            BLOCK_ENTITIES.register("dust_funnel", () -> BlockEntityType.Builder.of(
                    DustFunnelBlockEntity::new,
                    GTManualStations.DUST_FUNNEL.get()).build(null));

    public static final Supplier<BlockEntityType<DynamiteBlockEntity>> DYNAMITE =
            BLOCK_ENTITIES.register("dynamite", () -> BlockEntityType.Builder.of(
                    DynamiteBlockEntity::new,
                    GTToolBlocks.DYNAMITE.get(),
                    GTToolBlocks.BOOMSTICK.get(),
                    GTToolBlocks.DYNAMITE_STRONG.get()).build(null));

    public static final Supplier<BlockEntityType<EnderGarbageBlockEntity>> ENDER_GARBAGE =
            BLOCK_ENTITIES.register("ender_garbage", () -> BlockEntityType.Builder.of(
                    EnderGarbageBlockEntity::new,
                    GTStorageContainers.ENDER_GARBAGE.get()).build(null));

    public static final Supplier<BlockEntityType<EnderGarbageDumpBlockEntity>> ENDER_GARBAGE_DUMP =
            BLOCK_ENTITIES.register("ender_garbage_dump", () -> BlockEntityType.Builder.of(
                    EnderGarbageDumpBlockEntity::new,
                    GTStorageContainers.ENDER_GARBAGE_DUMP.get()).build(null));

    public static final Supplier<BlockEntityType<ExtenderBlockEntity>> EXTENDER =
            BLOCK_ENTITIES.register("extender", () -> BlockEntityType.Builder.of(
                    ExtenderBlockEntity::new,
                    GTRelaysFilters.EXTENDERS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<FilterBlockEntity>> FILTER =
            BLOCK_ENTITIES.register("filter", () -> BlockEntityType.Builder.of(
                    FilterBlockEntity::new,
                    GTRelaysFilters.FILTERS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register("fluid_pipe", () -> {
                Block[] blocks = AddonPipeRegistration.fluidPipeBlocks(
                        GTFluidPipes.all().stream().map(RegisteredBlock::get));
                return BlockEntityType.Builder.of(
                        FluidPipeBlockEntity::new, blocks).build(null);
            });

    public static final Supplier<BlockEntityType<FluidSpringBlockEntity>> FLUID_SPRING =
            BLOCK_ENTITIES.register("fluid_spring", () -> BlockEntityType.Builder.of(
                    FluidSpringBlockEntity::new,
                    GTFluidSprings.allBlocks()).build(null));

    public static final Supplier<BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER =
            BLOCK_ENTITIES.register("heat_exchanger", () -> BlockEntityType.Builder.of(
                    HeatExchangerBlockEntity::new,
                    GTHeatExchangers.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<HopperBlockEntity>> HOPPER =
            BLOCK_ENTITIES.register("hopper", () -> BlockEntityType.Builder.of(
                    HopperBlockEntity::new,
                    GTHoppers.hoppers().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITIES.register("item_pipe", () -> BlockEntityType.Builder.of(
                    ItemPipeBlockEntity::new,
                    AddonPipeRegistration.itemPipeBlocks(
                            GTItemPipes.all().stream().map(RegisteredBlock::get))).build(null));

    public static final Supplier<BlockEntityType<LargeCrucibleControllerBlockEntity>> LARGE_CRUCIBLE =
            registerMultiblock("large_crucible");

    public static final Supplier<BlockEntityType<LargeDynamoControllerBlockEntity>> LARGE_DYNAMO =
            registerMultiblock("large_dynamo");

    public static final Supplier<BlockEntityType<LargeGasTurbineControllerBlockEntity>> LARGE_GAS_TURBINE =
            registerMultiblock("large_gas_turbine");

    public static final Supplier<BlockEntityType<LargeHeatExchangerControllerBlockEntity>> LARGE_HEAT_EXCHANGER =
            registerMultiblock("large_heat_exchanger");

    public static final Supplier<BlockEntityType<LargeTurbineControllerBlockEntity>> LARGE_TURBINE =
            registerMultiblock("large_turbine");

    public static final Supplier<BlockEntityType<LaserConverterBlockEntity>> LASER_CONVERTER =
            BLOCK_ENTITIES.register("laser_converter", () -> BlockEntityType.Builder.of(
                    LaserConverterBlockEntity::new,
                    GTLasers.all().stream()
                            .map(Supplier::get)
                            .filter(b -> b instanceof LaserConverterBlock)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<LaserFiberBlockEntity>> LASER_FIBER =
            BLOCK_ENTITIES.register("laser_fiber", () -> BlockEntityType.Builder.of(
                    LaserFiberBlockEntity::new,
                    GTLasers.LASER_FIBER_WIRE.get()).build(null));

    public static final Supplier<BlockEntityType<LightningRodControllerBlockEntity>> LIGHTNING_ROD =
            registerMultiblock("lightning_rod");

    public static final Supplier<BlockEntityType<LockerBlockEntity>> LOCKER =
            BLOCK_ENTITIES.register("locker", () -> BlockEntityType.Builder.of(
                    LockerBlockEntity::new,
                    GTStorageContainers.LOCKERS.stream()
                            .map(Supplier::get)
                            .toArray(Block[]::new)).build(null));

    /** GT6 17997: Logistics Core controller (the walls are existing multiblock port entities). */
    public static final Supplier<BlockEntityType<LogisticsCoreControllerBlockEntity>> LOGISTICS_CORE =
            registerMultiblock("logistics_core");

    public static final Supplier<BlockEntityType<LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE =
            BLOCK_ENTITIES.register("logistics_mass_storage", () -> BlockEntityType.Builder.of(
                    LogisticsMassStorageBlockEntity::new,
                    GTMassStorages.LOGISTICS_MASS_STORAGES.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<LogisticsTankBlockEntity>> LOGISTICS_TANK =
            BLOCK_ENTITIES.register("logistics_tank", () -> BlockEntityType.Builder.of(
                    LogisticsTankBlockEntity::new,
                    GTTanks.LOGISTICS_TANK.get()).build(null));

    /** GT6 24901: six-way cuttable logistics connector. */
    public static final Supplier<BlockEntityType<LogisticsWireBlockEntity>> LOGISTICS_WIRE =
            BLOCK_ENTITIES.register("logistics_wire", () -> BlockEntityType.Builder.of(
                    LogisticsWireBlockEntity::new,
                    GTLogisticsWire.WIRE.get()).build(null));

    public static final Supplier<BlockEntityType<LongDistEndpointBlockEntity>> LONG_DIST_ENDPOINT =
            BLOCK_ENTITIES.register("long_dist_endpoint", () -> BlockEntityType.Builder.of(
                    LongDistEndpointBlockEntity::new,
                    GTLongDistance.block("long_dist_endpoint_item"),
                    GTLongDistance.block("long_dist_endpoint_fluid")).build(null));

    public static final Supplier<BlockEntityType<LongDistanceTransformerBlockEntity>> LONG_DIST_TRANSFORMER =
            BLOCK_ENTITIES.register("long_dist_transformer", () -> BlockEntityType.Builder.of(
                    LongDistanceTransformerBlockEntity::new,
                    GTLongDistance.transformers()).build(null));

    public static final Supplier<BlockEntityType<ManualToolBlockEntity>> MANUAL_TOOL =
            BLOCK_ENTITIES.register("manual_tool", () -> BlockEntityType.Builder.of(
                    ManualToolBlockEntity::new,
                    GTManualStations.MANUAL.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE =
            BLOCK_ENTITIES.register("mass_storage", () -> {
                List<Block> blocks = new ArrayList<>();
                GTMassStorages.MASS_STORAGES.forEach(ro -> blocks.add(ro.get()));
                GTItemBarrels.ALL.forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        MassStorageBlockEntity::new, blocks.toArray(new Block[0])).build(null);
            });

    public static final Supplier<BlockEntityType<MaterialAnvilBlockEntity>> MATERIAL_ANVIL =
            BLOCK_ENTITIES.register("material_anvil", () -> BlockEntityType.Builder.of(
                    MaterialAnvilBlockEntity::new,
                    GTManualStations.ANVILS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<MetalChestBlockEntity>> METAL_CHEST =
            BLOCK_ENTITIES.register("metal_chest", () -> BlockEntityType.Builder.of(
                    MetalChestBlockEntity::new,
                    GTMetalChests.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<MoldBlockEntity>> MOLD =
            BLOCK_ENTITIES.register("mold", () -> BlockEntityType.Builder.of(
                    MoldBlockEntity::new,
                    GTSmeltery.molds().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<MoldBasinBlockEntity>> MOLD_BASIN =
            BLOCK_ENTITIES.register("mold_basin", () -> BlockEntityType.Builder.of(
                    MoldBasinBlockEntity::new,
                    GTSmeltery.basins().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<MultiblockPortBlockEntity>> MULTIBLOCK_PORT =
            BLOCK_ENTITIES.register("multiblock_port", () -> BlockEntityType.Builder.of(
                    MultiblockPortBlockEntity::new,
                    GTMultiblocks.portBlocks()).build(null));

    public static final Supplier<BlockEntityType<MultiblockTankControllerBlockEntity>> MULTIBLOCK_TANK =
            registerMultiblock("multiblock_tank");

    public static final Supplier<BlockEntityType<OriginalLargeBoilerControllerBlockEntity>> ORIGINAL_LARGE_BOILER =
            registerMultiblock("original_large_boiler");

    public static final Supplier<BlockEntityType<PileBlockEntity>> PILE =
            BLOCK_ENTITIES.register("pile", () -> BlockEntityType.Builder.of(
                    PileBlockEntity::new,
                    GTMaterialPiles.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<PlacedReactorRodBlockEntity>> PLACED_REACTOR_ROD =
            BLOCK_ENTITIES.register("placed_reactor_rod", () -> BlockEntityType.Builder.of(
                    PlacedReactorRodBlockEntity::new,
                    GTFuelRods.BLOCKS.values().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<PortableContainerBlockEntity>> PORTABLE_CONTAINER =
            BLOCK_ENTITIES.register("portable_container", () -> BlockEntityType.Builder.of(
                    PortableContainerBlockEntity::new,
                    GTToolBlocks.PORTABLE.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<ProcessingToolBlockEntity>> PROCESSING_TOOL =
            BLOCK_ENTITIES.register("processing_tool", () -> BlockEntityType.Builder.of(
                    ProcessingToolBlockEntity::new,
                    GTManualStations.VESSELS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump", () -> BlockEntityType.Builder.of(
                    PumpBlockEntity::new,
                    GTPumps.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<QueueHopperBlockEntity>> QUEUE_HOPPER =
            BLOCK_ENTITIES.register("queue_hopper", () -> BlockEntityType.Builder.of(
                    QueueHopperBlockEntity::new,
                    GTHoppers.queues().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<ReactorCoreBlockEntity>> REACTOR_CORE =
            BLOCK_ENTITIES.register("reactor_core", () -> BlockEntityType.Builder.of(
                    ReactorCoreBlockEntity::new,
                    GTEnergyNodes.REACTOR_CORE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<ReactorCore2x2BlockEntity>> REACTOR_CORE_2X2 =
            BLOCK_ENTITIES.register("reactor_core_2x2", () -> BlockEntityType.Builder.of(
                    ReactorCore2x2BlockEntity::new,
                    GTEnergyNodes.REACTOR_CORE_2X2.get()).build(null));

    public static final Supplier<BlockEntityType<RockBlockEntity>> ROCK =
            BLOCK_ENTITIES.register("rock", () -> BlockEntityType.Builder.of(
                    RockBlockEntity::new,
                    GTBlocks.ROCK.get()).build(null));

    public static final Supplier<BlockEntityType<SafeBlockEntity>> SAFE =
            BLOCK_ENTITIES.register("safe", () -> BlockEntityType.Builder.of(
                    SafeBlockEntity::new,
                    GTSafes.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<SandwichBlockEntity>> SANDWICH_BLOCK =
            BLOCK_ENTITIES.register("sandwich_block", () -> BlockEntityType.Builder.of(
                    SandwichBlockEntity::new,
                    GTSandwich.SANDWICH_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<SapBagBlockEntity>> SAP_BAG =
            BLOCK_ENTITIES.register("sap_bag", () -> BlockEntityType.Builder.of(
                    SapBagBlockEntity::new,
                    GTManualStations.SAP_BAG.get()).build(null));

    public static final Supplier<BlockEntityType<SensorBlockEntity>> SENSOR =
            BLOCK_ENTITIES.register("sensor", () -> {
                var blocks = new ArrayList<SensorBlock>();
                for (var holder : GTSensors.all()) if (holder.isPresent()) blocks.add(holder.get());
                for (var holder : AddonSensors.blocks()) {
                    if (holder.get() instanceof SensorBlock sensor)
                        blocks.add(sensor);
                }
                return BlockEntityType.Builder.of(
                        SensorBlockEntity::new,
                        blocks.toArray(SensorBlock[]::new)).build(null);
            });

    public static final Supplier<BlockEntityType<SmeltingCrucibleBlockEntity>> SMELTING_CRUCIBLE =
            BLOCK_ENTITIES.register("smelting_crucible", () -> BlockEntityType.Builder.of(
                    SmeltingCrucibleBlockEntity::new,
                    GTSmeltery.crucibles().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<SolidBurningBoxBlockEntity>> SOLID_BURNING_BOX =
            BLOCK_ENTITIES.register("solid_burning_box", () -> BlockEntityType.Builder.of(
                    SolidBurningBoxBlockEntity::new,
                    GTBurningBoxes.solidBoxes().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<BoilerTankBlockEntity>> STEAM_BOILER =
            BLOCK_ENTITIES.register("steam_boiler", () -> BlockEntityType.Builder.of(
                    BoilerTankBlockEntity::new,
                    GTBoilers.all().stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<TankBlockEntity>> TANK =
            BLOCK_ENTITIES.register("tank", () -> {
                Block[] blocks = GTTanks.all().stream()
                        .map(RegisteredBlock::get)
                        .filter(block -> !(block instanceof LogisticsTankBlock))
                        .toArray(Block[]::new);
                return BlockEntityType.Builder.of(
                        TankBlockEntity::new, blocks).build(null);
            });

    public static final Supplier<BlockEntityType<TreeHoleBlockEntity>> TREE_HOLE =
            BLOCK_ENTITIES.register("tree_hole", () -> BlockEntityType.Builder.of(
                    TreeHoleBlockEntity::new,
                    GTTreeHoles.blocks().toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<UsbSwitchBlockEntity>> USB_SWITCH =
            BLOCK_ENTITIES.register("usb_switch", () -> BlockEntityType.Builder.of(
                    UsbSwitchBlockEntity::new,
                    GTStorageContainers.USB_SWITCH.get(),
                    GTStorageContainers.HDD_SWITCH.get()).build(null));

    public static final Supplier<BlockEntityType<VonDaGraaggControllerBlockEntity>> VON_DA_GRAAGG =
            registerMultiblock("von_da_graagg");

    public static final Supplier<BlockEntityType<ZpmModuleBlockEntity>> ZPM_MODULE =
            BLOCK_ENTITIES.register("zpm_module", () -> BlockEntityType.Builder.of(
                    ZpmModuleBlockEntity::new,
                    GTLasers.ZPM.get()).build(null));

    public static final Supplier<BlockEntityType<ZpmDischargerBlockEntity>> ZPM_DISCHARGER =
            BLOCK_ENTITIES.register("zpm_discharger", () -> BlockEntityType.Builder.of(
                    ZpmDischargerBlockEntity::new,
                    GTLasers.ZPM_DISCHARGER_BASIC.get(),
                    GTLasers.ZPM_DISCHARGER_ADVANCED.get(),
                    GTLasers.ZPM_DISCHARGER_ELITE.get()).build(null));

//? if >=1.21.1 {
//?} else {
    // 1.20.1-only legacy save migration: writes the old host stone back into OreBlock.STONE, then removes itself.
    public static final RegistryObject<BlockEntityType<OreBlockEntity>> ORE =
            BLOCK_ENTITIES.register("ore", () -> {
                Block[] blocks = GTBlocks.allEntries().stream()
                        .filter(RegisteredBlock::isPresent)
                        .map(RegisteredBlock::get)
                        .filter(b -> b instanceof OreBlock)
                        .toArray(Block[]::new);
                return BlockEntityType.Builder.of(OreBlockEntity::new, blocks).build(null);
            });
//?}

    private static final Map<String, DeferredRegister<BlockEntityType<?>>> ADDON_ENTITIES = new LinkedHashMap<>();

    private GTBlockEntities() {}

    public static DeferredRegister<BlockEntityType<?>> entities(String namespace) {
        if (REGISTRY_NAMESPACE.equals(namespace)) return BLOCK_ENTITIES;
//? if >=1.21.1 {
        return ADDON_ENTITIES.computeIfAbsent(
                namespace, ns -> DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ns));
//?} else {
        return ADDON_ENTITIES.computeIfAbsent(
                namespace, ns -> DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ns));
//?}
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        for (var register : ADDON_ENTITIES.values()) register.register(bus);
        ADDON_ENTITIES.clear();
    }

    public static final Supplier<BlockEntityType<PowerCellBlockEntity>> POWER_CELL =
            BLOCK_ENTITIES.register("power_cell", () -> BlockEntityType.Builder.of(
                    PowerCellBlockEntity::new,
                    GTLegacyEnergyDevices.POWER_CELLS.stream()
                            .map(RegisteredBlock::get)
                            .toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<MagicFieldAbsorberBlockEntity>> MAGIC_FIELD_ABSORBER =
            BLOCK_ENTITIES.register("magic_field_absorber", () -> BlockEntityType.Builder.of(
                    MagicFieldAbsorberBlockEntity::new,
                    GTLegacyEnergyDevices.MAGIC_FIELD_ABSORBER.get()).build(null));

    private static <E extends BlockEntity & TrackedMultiblock> Supplier<BlockEntityType<E>> registerMultiblock(
            String path) {
        return BLOCK_ENTITIES.register(path, () -> BlockEntityType.Builder.of(
                MultiblockRegistration.<E>entityFactory(REGISTRY_NAMESPACE, path),
                MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, path)).build(null));
    }
}
