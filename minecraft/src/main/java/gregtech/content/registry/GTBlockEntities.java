package gregtech.content.registry;
import net.minecraft.resources.ResourceLocation;
import gregtech.engine.mod.GregTechIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import gregtech.content.blockentity.RockBlockEntity;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
*///?} else {
import gregtech.content.blockentity.machine.CrucibleCrossingBlockEntity;
import gregtech.content.blockentity.machine.CrucibleFaucetBlockEntity;
import gregtech.content.blockentity.machine.BurningBoxBlockEntity;
import gregtech.content.blockentity.machine.HopperBlockEntity;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import gregtech.content.blockentity.machine.QueueHopperBlockEntity;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.content.blockentity.machine.SolidBurningBoxBlockEntity;
import gregtech.content.block.machine.BurningBoxBlock;
import gregtech.content.block.machine.CrucibleCrossingBlock;
import gregtech.content.block.machine.CrucibleFaucetBlock;
import gregtech.content.block.machine.HopperBlock;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.block.machine.MoldBasinBlock;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.QueueHopperBlock;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.content.block.machine.SolidBurningBoxBlock;
import net.minecraft.world.level.block.Block;
//?}
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
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
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GregTechIdentity.REGISTRY_NAMESPACE);
//?}

    // Shared registrations: both loaders queue into the same deferred register.
    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.AddonEnergyPartBlockEntity>> ADDON_ENERGY_PART =
            BLOCK_ENTITIES.register("addon_energy_part",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.AddonEnergyPartBlockEntity::new,GTMultiblocks.energyPartBlocks()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity>> ADDON_FUNCTIONAL_PART =
            BLOCK_ENTITIES.register("addon_functional_part",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity::new,GTMultiblocks.functionalPartBlocks()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity>> BEDROCK_DRILL =
            BLOCK_ENTITIES.register("bedrock_drill",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "bedrock_drill"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "bedrock_drill")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.BurningBoxBlockEntity>> BURNING_BOX =
            BLOCK_ENTITIES.register("burning_box",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.BurningBoxBlockEntity::new,GTBurningBoxes.fuelBoxes().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.BushBlockEntity>> BUSH =
            BLOCK_ENTITIES.register("bush",()->BlockEntityType.Builder.of(gregtech.content.blockentity.BushBlockEntity::new,GTBushes.allBlocks()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.CFoamBlockEntity>> CFOAM =
            BLOCK_ENTITIES.register("cfoam",()->BlockEntityType.Builder.of(gregtech.content.blockentity.CFoamBlockEntity::new,GTConstructionVariants.wetFoams()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity>> COKE_OVEN =
            BLOCK_ENTITIES.register("coke_oven",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "coke_oven"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "coke_oven")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.CrucibleCrossingBlockEntity>> CRUCIBLE_CROSSING =
            BLOCK_ENTITIES.register("crucible_crossing",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.CrucibleCrossingBlockEntity::new,GTSmeltery.crossings().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.CrucibleFaucetBlockEntity>> CRUCIBLE_FAUCET =
            BLOCK_ENTITIES.register("crucible_faucet",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.CrucibleFaucetBlockEntity::new,GTSmeltery.faucets().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.CryoDistillationControllerBlockEntity>> CRYO_DISTILLATION =
            BLOCK_ENTITIES.register("cryo_distillation",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.CryoDistillationControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "cryo_distillation"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "cryo_distillation")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.block.misc.DungeonPortalBlockEntity>> DUNGEON_PORTAL =
            BLOCK_ENTITIES.register("dungeon_portal",()->BlockEntityType.Builder.of(gregtech.content.block.misc.DungeonPortalBlockEntity::new,GTDungeonBlocks.PORTAL_NETHER.get(),GTDungeonBlocks.PORTAL_END.get()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.tool.DynamiteBlockEntity>> DYNAMITE =
            BLOCK_ENTITIES.register("dynamite",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.DynamiteBlockEntity::new,GTToolBlocks.DYNAMITE.get(),GTToolBlocks.BOOMSTICK.get(),GTToolBlocks.DYNAMITE_STRONG.get()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register("fluid_pipe", () -> {
                net.minecraft.world.level.block.Block[] blocks = AddonPipeRegistration.fluidPipeBlocks(
                        GTFluidPipes.all().stream().map(RegisteredBlock::get));
                return BlockEntityType.Builder.of(
                        gregtech.content.blockentity.machine.FluidPipeBlockEntity::new, blocks).build(null);
            });

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.FluidSpringBlockEntity>> FLUID_SPRING =
            BLOCK_ENTITIES.register("fluid_spring",()->BlockEntityType.Builder.of(gregtech.content.blockentity.FluidSpringBlockEntity::new,GTFluidSprings.allBlocks()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.HeatExchangerBlockEntity>> HEAT_EXCHANGER =
            BLOCK_ENTITIES.register("heat_exchanger",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.HeatExchangerBlockEntity::new,GTHeatExchangers.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.HopperBlockEntity>> HOPPER =
            BLOCK_ENTITIES.register("hopper",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.HopperBlockEntity::new,GTHoppers.hoppers().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITIES.register("item_pipe",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.ItemPipeBlockEntity::new,AddonPipeRegistration.itemPipeBlocks(GTItemPipes.all().stream().map(RegisteredBlock::get))).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity>> LARGE_CRUCIBLE =
            BLOCK_ENTITIES.register("large_crucible",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "large_crucible"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "large_crucible")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.LargeDynamoControllerBlockEntity>> LARGE_DYNAMO =
            BLOCK_ENTITIES.register("large_dynamo",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LargeDynamoControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "large_dynamo"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "large_dynamo")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity>> LARGE_GAS_TURBINE =
            BLOCK_ENTITIES.register("large_gas_turbine",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "large_gas_turbine"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "large_gas_turbine")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.LargeHeatExchangerControllerBlockEntity>> LARGE_HEAT_EXCHANGER =
            BLOCK_ENTITIES.register("large_heat_exchanger",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LargeHeatExchangerControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "large_heat_exchanger"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "large_heat_exchanger")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity>> LARGE_TURBINE =
            BLOCK_ENTITIES.register("large_turbine",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "large_turbine"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "large_turbine")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.energy.LaserConverterBlockEntity>> LASER_CONVERTER =
            BLOCK_ENTITIES.register("laser_converter",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.LaserConverterBlockEntity::new,GTLasers.all().stream().map(java.util.function.Supplier::get).filter(b->b instanceof gregtech.content.block.energy.LaserConverterBlock).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.energy.LaserFiberBlockEntity>> LASER_FIBER =
            BLOCK_ENTITIES.register("laser_fiber",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.LaserFiberBlockEntity::new,GTLasers.LASER_FIBER_WIRE.get()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.LogisticsCoreControllerBlockEntity>> LOGISTICS_CORE =
            BLOCK_ENTITIES.register("logistics_core",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LogisticsCoreControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "logistics_core"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "logistics_core")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.MoldBasinBlockEntity>> MOLD_BASIN =
            BLOCK_ENTITIES.register("mold_basin",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.MoldBasinBlockEntity::new,GTSmeltery.basins().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.MultiblockPortBlockEntity>> MULTIBLOCK_PORT =
            BLOCK_ENTITIES.register("multiblock_port",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.MultiblockPortBlockEntity::new,GTMultiblocks.portBlocks()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity>> MULTIBLOCK_TANK =
            BLOCK_ENTITIES.register("multiblock_tank",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "multiblock_tank"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "multiblock_tank")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity>> ORIGINAL_LARGE_BOILER =
            BLOCK_ENTITIES.register("original_large_boiler",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "original_large_boiler"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "original_large_boiler")).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.energy.PlacedReactorRodBlockEntity>> PLACED_REACTOR_ROD =
            BLOCK_ENTITIES.register("placed_reactor_rod",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.PlacedReactorRodBlockEntity::new,GTFuelRods.BLOCKS.values().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.tool.PortableContainerBlockEntity>> PORTABLE_CONTAINER =
            BLOCK_ENTITIES.register("portable_container",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.PortableContainerBlockEntity::new,GTToolBlocks.PORTABLE.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.energy.PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.PumpBlockEntity::new,GTPumps.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.QueueHopperBlockEntity>> QUEUE_HOPPER =
            BLOCK_ENTITIES.register("queue_hopper",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.QueueHopperBlockEntity::new,GTHoppers.queues().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.energy.ReactorCoreBlockEntity>> REACTOR_CORE =
            BLOCK_ENTITIES.register("reactor_core",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ReactorCoreBlockEntity::new,GTEnergyNodes.REACTOR_CORE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.energy.ReactorCore2x2BlockEntity>> REACTOR_CORE_2X2 =
            BLOCK_ENTITIES.register("reactor_core_2x2",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ReactorCore2x2BlockEntity::new,GTEnergyNodes.REACTOR_CORE_2X2.get()).build(null));

    public static final Supplier<BlockEntityType<RockBlockEntity>> ROCK =
            BLOCK_ENTITIES.register("rock", () -> BlockEntityType.Builder.of(RockBlockEntity::new, GTBlocks.ROCK.get()).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.sensor.SensorBlockEntity>> SENSOR =
            BLOCK_ENTITIES.register("sensor", () -> {
                var blocks = new java.util.ArrayList<gregtech.content.block.sensor.SensorBlock>();
                for (var holder : GTSensors.all()) if (holder.isPresent()) blocks.add(holder.get());
                for (var holder : gregtech.engine.sensor.AddonSensors.blocks()) {
                    if (holder.get() instanceof gregtech.content.block.sensor.SensorBlock sensor)
                        blocks.add(sensor);
                }
                return BlockEntityType.Builder.of(
                        gregtech.content.blockentity.sensor.SensorBlockEntity::new,
                        blocks.toArray(gregtech.content.block.sensor.SensorBlock[]::new)).build(null);
            });

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.BoilerTankBlockEntity>> STEAM_BOILER =
            BLOCK_ENTITIES.register("steam_boiler",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.BoilerTankBlockEntity::new,GTBoilers.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.TankBlockEntity>> TANK =
            BLOCK_ENTITIES.register("tank", () -> {
                net.minecraft.world.level.block.Block[] blocks = GTTanks.all().stream()
                        .map(RegisteredBlock::get)
                        .filter(block -> !(block instanceof gregtech.content.block.machine.LogisticsTankBlock))
                        .toArray(net.minecraft.world.level.block.Block[]::new);
                return BlockEntityType.Builder.of(
                        gregtech.content.blockentity.machine.TankBlockEntity::new, blocks).build(null);
            });

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.TreeHoleBlockEntity>> TREE_HOLE =
            BLOCK_ENTITIES.register("tree_hole",()->BlockEntityType.Builder.of(gregtech.content.blockentity.TreeHoleBlockEntity::new,GTTreeHoles.blocks().toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<gregtech.content.blockentity.machine.VonDaGraaggControllerBlockEntity>> VON_DA_GRAAGG =
            BLOCK_ENTITIES.register("von_da_graagg",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.VonDaGraaggControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "von_da_graagg"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "von_da_graagg")).build(null));

//? if >=1.21.1 {
    /*
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.machine.LightningRodControllerBlockEntity>> LIGHTNING_ROD=BLOCK_ENTITIES.register("lightning_rod",()->BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LightningRodControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "lightning_rod"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "lightning_rod")).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.platform.neoforge.smeltery.MoldEntity>> MOLD=gregtech.platform.neoforge.smeltery.SmelteryRegistries.MOLD;
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.platform.neoforge.smeltery.SolidBurningBoxEntity>> SOLID_BURNING_BOX=gregtech.platform.neoforge.smeltery.SmelteryRegistries.SOLID_BURNING_BOX;
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.BumbliaryBlockEntity>> BUMBLIARY=BLOCK_ENTITIES.register("bumbliary",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.BumbliaryBlockEntity::new,GTBumbleBlocks.BUMBLIARY.get(),GTBumbleBlocks.ADVANCED_BUMBLIARY.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.misc.BumbleHiveBlockEntity>> BUMBLE_HIVE=BLOCK_ENTITIES.register("bumble_hive",()->BlockEntityType.Builder.of(gregtech.content.blockentity.misc.BumbleHiveBlockEntity::new,GTBumbleBlocks.allHives()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.misc.PileBlockEntity>> PILE=BLOCK_ENTITIES.register("pile",()->BlockEntityType.Builder.of(gregtech.content.blockentity.misc.PileBlockEntity::new,GTMaterialPiles.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.misc.CoinPileBlockEntity>> COIN_PILE=BLOCK_ENTITIES.register("coin_pile",()->BlockEntityType.Builder.of(gregtech.content.blockentity.misc.CoinPileBlockEntity::new,GTCoinPiles.COIN_PILE.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.BookShelfBlockEntity>> BOOKSHELF=BLOCK_ENTITIES.register("bookshelf",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.BookShelfBlockEntity::new,GTBookShelves.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.SafeBlockEntity>> SAFE=BLOCK_ENTITIES.register("safe",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.SafeBlockEntity::new,GTSafes.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.AdvancedCraftingTableBlockEntity>> ADVANCED_CRAFTING_TABLE=BLOCK_ENTITIES.register("advanced_crafting_table",()->BlockEntityType.Builder.of(gregtech.content.block.misc.AdvancedCraftingTableBlockEntity::new,GTCraftingTables.ADVANCED.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.ChargingCraftingTableBlockEntity>> CHARGING_CRAFTING_TABLE=BLOCK_ENTITIES.register("charging_crafting_table",()->BlockEntityType.Builder.of(gregtech.content.block.misc.ChargingCraftingTableBlockEntity::new,GTCraftingTables.CHARGING.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.DrawerQuadBlockEntity>> DRAWER_QUAD=BLOCK_ENTITIES.register("drawer_quad",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.DrawerQuadBlockEntity::new,GTStorageContainers.DRAWERS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.BottleCrateBlockEntity>> BOTTLE_CRATE=BLOCK_ENTITIES.register("bottle_crate",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.BottleCrateBlockEntity::new,GTStorageContainers.BOTTLE_CRATES.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.LockerBlockEntity>> LOCKER=BLOCK_ENTITIES.register("locker",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.LockerBlockEntity::new,GTStorageContainers.LOCKERS.stream().map(java.util.function.Supplier::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.MetalChestBlockEntity>> METAL_CHEST=BLOCK_ENTITIES.register("metal_chest",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.MetalChestBlockEntity::new,GTMetalChests.all().stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.EnderGarbageBlockEntity>> ENDER_GARBAGE=BLOCK_ENTITIES.register("ender_garbage",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.EnderGarbageBlockEntity::new,GTStorageContainers.ENDER_GARBAGE.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity>> ENDER_GARBAGE_DUMP=BLOCK_ENTITIES.register("ender_garbage_dump",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity::new,GTStorageContainers.ENDER_GARBAGE_DUMP.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.inventory.UsbSwitchBlockEntity>> USB_SWITCH=BLOCK_ENTITIES.register("usb_switch",()->BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.UsbSwitchBlockEntity::new,GTStorageContainers.USB_SWITCH.get(),GTStorageContainers.HDD_SWITCH.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.ExtenderBlockEntity>> EXTENDER=BLOCK_ENTITIES.register("extender",()->BlockEntityType.Builder.of(gregtech.content.block.misc.ExtenderBlockEntity::new,GTRelaysFilters.EXTENDERS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.FilterBlockEntity>> FILTER=BLOCK_ENTITIES.register("filter",()->BlockEntityType.Builder.of(gregtech.content.block.misc.FilterBlockEntity::new,GTRelaysFilters.FILTERS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.AutoIgniterBlockEntity>> AUTO_IGNITER=BLOCK_ENTITIES.register("auto_igniter",()->BlockEntityType.Builder.of(gregtech.content.block.misc.AutoIgniterBlockEntity::new,GTRelaysFilters.IGNITERS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.AutoHammerBlockEntity>> AUTO_HAMMER=BLOCK_ENTITIES.register("auto_hammer",()->BlockEntityType.Builder.of(gregtech.content.block.misc.AutoHammerBlockEntity::new,GTRelaysFilters.HAMMERS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.MaterialAnvilBlockEntity>> MATERIAL_ANVIL=BLOCK_ENTITIES.register("material_anvil",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.MaterialAnvilBlockEntity::new,GTManualStations.ANVILS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.ManualToolBlockEntity>> MANUAL_TOOL=BLOCK_ENTITIES.register("manual_tool",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.ManualToolBlockEntity::new,GTManualStations.MANUAL.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.ProcessingToolBlockEntity>> PROCESSING_TOOL=BLOCK_ENTITIES.register("processing_tool",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.ProcessingToolBlockEntity::new,GTManualStations.VESSELS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.DustFunnelBlockEntity>> DUST_FUNNEL=BLOCK_ENTITIES.register("dust_funnel",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.DustFunnelBlockEntity::new,GTManualStations.DUST_FUNNEL.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.SapBagBlockEntity>> SAP_BAG=BLOCK_ENTITIES.register("sap_bag",()->BlockEntityType.Builder.of(gregtech.content.blockentity.SapBagBlockEntity::new,GTManualStations.SAP_BAG.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.AdvancedButtonBlockEntity>> ADVANCED_BUTTON=BLOCK_ENTITIES.register("advanced_button",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.AdvancedButtonBlockEntity::new,GTManualStations.ADVANCED_BUTTON.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.tool.CoinMoldBlockEntity>> COIN_MOLD=BLOCK_ENTITIES.register("coin_mold",()->BlockEntityType.Builder.of(gregtech.content.blockentity.tool.CoinMoldBlockEntity::new,GTManualStations.COIN_MOLD.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.ZpmModuleBlockEntity>> ZPM_MODULE=BLOCK_ENTITIES.register("zpm_module",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ZpmModuleBlockEntity::new,GTZpmArtifacts.ZPM.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.ZpmDischargerBlockEntity>> ZPM_DISCHARGER=BLOCK_ENTITIES.register("zpm_discharger",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ZpmDischargerBlockEntity::new,GTZpmArtifacts.BASIC.get(),GTZpmArtifacts.ADVANCED.get(),GTZpmArtifacts.ELITE.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.LongDistEndpointBlockEntity>> LONG_DIST_ENDPOINT=BLOCK_ENTITIES.register("long_dist_endpoint",()->BlockEntityType.Builder.of(gregtech.content.block.misc.LongDistEndpointBlockEntity::new,GTLongDistance.block("long_dist_endpoint_item"),GTLongDistance.block("long_dist_endpoint_fluid")).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.block.misc.LongDistanceTransformerBlockEntity>> LONG_DIST_TRANSFORMER=BLOCK_ENTITIES.register("long_dist_transformer",()->BlockEntityType.Builder.of(gregtech.content.block.misc.LongDistanceTransformerBlockEntity::new,GTLongDistance.transformers()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.misc.SandwichBlockEntity>> SANDWICH_BLOCK=BLOCK_ENTITIES.register("sandwich_block",()->BlockEntityType.Builder.of(gregtech.content.blockentity.misc.SandwichBlockEntity::new,GTSandwich.SANDWICH_BLOCK.get()).build(null));
public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.machine.LogisticsTankBlockEntity>> LOGISTICS_TANK=BLOCK_ENTITIES.register("logistics_tank",()->BlockEntityType.Builder.of(gregtech.content.blockentity.machine.LogisticsTankBlockEntity::new,GTLogisticsTank.BLOCK.get()).build(null));
*///?} else {
public static final RegistryObject<BlockEntityType<SolidBurningBoxBlockEntity>> SOLID_BURNING_BOX =
            BLOCK_ENTITIES.register("solid_burning_box", () -> {
                SolidBurningBoxBlock[] blocks = GTBurningBoxes.solidBoxes().stream()
                        .filter(RegisteredBlock::isPresent)
                        .map(RegisteredBlock::get)
                        .toArray(SolidBurningBoxBlock[]::new);
                return BlockEntityType.Builder.of(SolidBurningBoxBlockEntity::new, blocks).build(null);
            });
public static final RegistryObject<BlockEntityType<SmeltingCrucibleBlockEntity>> SMELTING_CRUCIBLE =
            BLOCK_ENTITIES.register("smelting_crucible", () -> {
                SmeltingCrucibleBlock[] blocks = GTSmeltery.crucibles().stream()
                        .filter(RegisteredBlock::isPresent)
                        .map(RegisteredBlock::get)
                        .toArray(SmeltingCrucibleBlock[]::new);
                return BlockEntityType.Builder.of(SmeltingCrucibleBlockEntity::new, blocks).build(null);
            });
public static final RegistryObject<BlockEntityType<MoldBlockEntity>> MOLD =
            BLOCK_ENTITIES.register("mold", () -> {
                MoldBlock[] blocks = GTSmeltery.molds().stream()
                        .filter(RegisteredBlock::isPresent)
                        .map(RegisteredBlock::get)
                        .toArray(MoldBlock[]::new);
                return BlockEntityType.Builder.of(MoldBlockEntity::new, blocks).build(null);
            });
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.ProcessingToolBlockEntity>> PROCESSING_TOOL =
            BLOCK_ENTITIES.register("processing_tool", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.tool.ProcessingToolBlockEntity::new,
                    GTManualStations.VESSELS.stream().map(RegisteredBlock::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.MaterialAnvilBlockEntity>> MATERIAL_ANVIL =
            BLOCK_ENTITIES.register("material_anvil", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.tool.MaterialAnvilBlockEntity::new,
                    GTManualStations.ANVILS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.UsbSwitchBlockEntity>> USB_SWITCH =
            BLOCK_ENTITIES.register("usb_switch", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.UsbSwitchBlockEntity::new,
                    GTStorage.USB_SWITCH.get(), GTStorage.HDD_SWITCH.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.BottleCrateBlockEntity>> BOTTLE_CRATE =
            BLOCK_ENTITIES.register("bottle_crate", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.BottleCrateBlockEntity::new,
                    GTStorage.BOTTLE_CRATES.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.MassStorageBlockEntity>> MASS_STORAGE =
            BLOCK_ENTITIES.register("mass_storage", () -> {
                java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
                blocks.add(GTStorage.MASS_STORAGE.get());
                GTStorage.MASS_STORAGES.forEach(ro -> blocks.add(ro.get()));
                GTItemBarrels.ALL.forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        gregtech.content.blockentity.inventory.MassStorageBlockEntity::new,
                        blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE =
            BLOCK_ENTITIES.register("logistics_mass_storage", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.LogisticsMassStorageBlockEntity::new,
                    GTStorage.LOGISTICS_MASS_STORAGES.stream().map(RegisteredBlock::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.MetalChestBlockEntity>> METAL_CHEST =
            BLOCK_ENTITIES.register("metal_chest", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.MetalChestBlockEntity::new,
                    java.util.stream.Stream.of(GTStorage.METAL_CHESTS, GTStorage.REINFORCED_WOOD_CHESTS, GTLootChests.LOOT_CHESTS)
                            .flatMap(java.util.Collection::stream).map(RegisteredBlock::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.DrawerQuadBlockEntity>> DRAWER_QUAD =
            BLOCK_ENTITIES.register("drawer_quad", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.DrawerQuadBlockEntity::new,
                    GTStorage.DRAWERS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.SafeBlockEntity>> SAFE =
            BLOCK_ENTITIES.register("safe", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.SafeBlockEntity::new,
                    java.util.stream.Stream.concat(GTStorage.SAFES.stream(), GTStorage.KEY_SAFES.stream())
                            .map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.LockerBlockEntity>> LOCKER =
            BLOCK_ENTITIES.register("locker", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.LockerBlockEntity::new,
                    GTStorage.LOCKERS.stream().map(java.util.function.Supplier::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.EnderGarbageBlockEntity>> ENDER_GARBAGE =
            BLOCK_ENTITIES.register("ender_garbage", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.EnderGarbageBlockEntity::new,
                    GTStorage.ENDER_GARBAGE.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity>> ENDER_GARBAGE_DUMP =
            BLOCK_ENTITIES.register("ender_garbage_dump", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity::new,
                    GTStorage.ENDER_GARBAGE_DUMP.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.DustFunnelBlockEntity>> DUST_FUNNEL =
            BLOCK_ENTITIES.register("dust_funnel", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.tool.DustFunnelBlockEntity::new,
                    GTManualStations.DUST_FUNNEL.get()).build(null));

    /** GT6's bumbliary (multi-tile 32741) and advanced bumbliary (32007): both share this type. */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.BumbliaryBlockEntity>> BUMBLIARY =
            BLOCK_ENTITIES.register("bumbliary", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.tool.BumbliaryBlockEntity::new,
                    GTBumbleBlocks.BUMBLIARY.get(), GTBumbleBlocks.ADVANCED_BUMBLIARY.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.ManualToolBlockEntity>> MANUAL_TOOL =
            BLOCK_ENTITIES.register("manual_tool", () -> {
                gregtech.content.block.tool.ManualToolBlock[] blocks = GTManualStations.MANUAL.stream()
                        .filter(RegisteredBlock::isPresent)
                        .map(RegisteredBlock::get)
                        .toArray(gregtech.content.block.tool.ManualToolBlock[]::new);
                return BlockEntityType.Builder.of(
                        gregtech.content.blockentity.tool.ManualToolBlockEntity::new, blocks).build(null);
            });
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.machine.LogisticsTankBlockEntity>> LOGISTICS_TANK =
            BLOCK_ENTITIES.register("logistics_tank", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.machine.LogisticsTankBlockEntity::new,
                    GTTanks.LOGISTICS_TANK.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.OreBlockEntity>> ORE =
            BLOCK_ENTITIES.register("ore", () -> {
                Block[] blocks = GTBlocks.allEntries().stream()
                        .filter(RegisteredBlock::isPresent)
                        .map(RegisteredBlock::get)
                        .filter(b -> b instanceof gregtech.content.block.OreBlock)
                        .toArray(Block[]::new);
                return BlockEntityType.Builder.of(gregtech.content.blockentity.OreBlockEntity::new, blocks).build(null);
            });
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.SapBagBlockEntity>> SAP_BAG =
            BLOCK_ENTITIES.register("sap_bag", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.SapBagBlockEntity::new,
                            GTManualStations.SAP_BAG.get()).build(null));

    /**
     * GT6's miniature portals ({@code MultiTileEntityMiniPortalNether}, {@code ...End}) as far as the
     * dungeon rooms need them: the block entity keeps the key id ({@code NBT_KEY}) and the active flag
     * ({@code NBT_ACTIVE}) of GT6's portal.
     */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.inventory.BookShelfBlockEntity>> BOOKSHELF =
            BLOCK_ENTITIES.register("bookshelf", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.inventory.BookShelfBlockEntity::new,
                            GTDecorBlocks.allBookshelves().stream().map(RegistryObject::get)
                                    .toArray(Block[]::new)).build(null));

    /**
     * GT6's loose ingot, plate and gem-plate piles (multi-tiles 32084/32085/32086), which all share
     * {@code gregapi.tileentity.misc.MultiTileEntityPlaceable}: one stored stack ({@code NBT_VALUE}).
     */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.misc.PileBlockEntity>> PILE =
            BLOCK_ENTITIES.register("pile", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.misc.PileBlockEntity::new,
                            GTDecorBlocks.INGOT_PILE.get(), GTDecorBlocks.PLATE_PILE.get(),
                            GTDecorBlocks.PLATE_GEM_PILE.get()).build(null));

    /** GT6's coin pile (multi-tile 32700): sixteen faces of up to sixteen coins each. */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.misc.CoinPileBlockEntity>> COIN_PILE =
            BLOCK_ENTITIES.register("coin_pile", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.misc.CoinPileBlockEntity::new,
                            GTDecorBlocks.COIN_PILE.get()).build(null));

    /** GT6's sixteen-layer edible sandwich ({@code MultiTileEntitySandwich}). */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.misc.SandwichBlockEntity>> SANDWICH_BLOCK =
            BLOCK_ENTITIES.register("sandwich_block", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.misc.SandwichBlockEntity::new,
                    GTDecorBlocks.SANDWICH_BLOCK.get()).build(null));

    /** GT6's drying construction foam ({@code MultiTileEntityCFoam}). */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.misc.BumbleHiveBlockEntity>> BUMBLE_HIVE =
            BLOCK_ENTITIES.register("bumble_hive", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.misc.BumbleHiveBlockEntity::new,
                            GTDecorBlocks.allHives()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.machine.LightningRodControllerBlockEntity>> LIGHTNING_ROD =
            BLOCK_ENTITIES.register("lightning_rod", () -> BlockEntityType.Builder.of(MultiblockRegistration.<gregtech.content.blockentity.machine.LightningRodControllerBlockEntity>entityFactory(REGISTRY_NAMESPACE, "lightning_rod"), MultiblockRegistration.blocksForEntityType(REGISTRY_NAMESPACE, "lightning_rod")).build(null));

    /** GT6 17997: Logistics Core controller (the walls are existing multiblock port entities). */
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.machine.LogisticsWireBlockEntity>> LOGISTICS_WIRE =
            BLOCK_ENTITIES.register("logistics_wire", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.machine.LogisticsWireBlockEntity::new,
                    GTIconSetBlocks.LOGISTICS_WIRE.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.AdvancedButtonBlockEntity>> ADVANCED_BUTTON =
            BLOCK_ENTITIES.register("advanced_button", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.tool.AdvancedButtonBlockEntity::new,
                    ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("advanced_button").toLowerCase()))).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.tool.CoinMoldBlockEntity>> COIN_MOLD =
            BLOCK_ENTITIES.register("coin_mold", () -> BlockEntityType.Builder.of(
                    gregtech.content.blockentity.tool.CoinMoldBlockEntity::new,
                    ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("coin_mold").toLowerCase()))).build(null));

    // Wave 45 BE types
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.AdvancedCraftingTableBlockEntity>> ADVANCED_CRAFTING_TABLE =
            BLOCK_ENTITIES.register("advanced_crafting_table", () -> BlockEntityType.Builder.of(
                    gregtech.content.block.misc.AdvancedCraftingTableBlockEntity::new,
                    GTMiscBlocks.ADVANCED_CRAFTING_TABLES.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.ChargingCraftingTableBlockEntity>> CHARGING_CRAFTING_TABLE =
            BLOCK_ENTITIES.register("charging_crafting_table", () -> BlockEntityType.Builder.of(
                    gregtech.content.block.misc.ChargingCraftingTableBlockEntity::new,
                    GTMiscBlocks.CHARGING_CRAFTING_TABLES.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.AutoIgniterBlockEntity>> AUTO_IGNITER =
            BLOCK_ENTITIES.register("auto_igniter", () -> {
                java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
                GTMiscBlocks.autoTools().stream()
                        .filter(ro -> ro.isPresent() && ro.get() instanceof gregtech.content.block.misc.AutoIgniterBlock)
                        .forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        gregtech.content.block.misc.AutoIgniterBlockEntity::new,
                        blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.AutoHammerBlockEntity>> AUTO_HAMMER =
            BLOCK_ENTITIES.register("auto_hammer", () -> {
                java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>();
                GTMiscBlocks.autoTools().stream()
                        .filter(ro -> ro.isPresent() && ro.get() instanceof gregtech.content.block.misc.AutoHammerBlock)
                        .forEach(ro -> blocks.add(ro.get()));
                return BlockEntityType.Builder.of(
                        gregtech.content.block.misc.AutoHammerBlockEntity::new,
                        blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.LongDistEndpointBlockEntity>> LONG_DIST_ENDPOINT =
            BLOCK_ENTITIES.register("long_dist_endpoint", () -> BlockEntityType.Builder.of(
                    gregtech.content.block.misc.LongDistEndpointBlockEntity::new,
                    GTMiscBlocks.LONG_DIST_ENDPOINT_ITEM.get(), GTMiscBlocks.LONG_DIST_ENDPOINT_FLUID.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.LongDistanceTransformerBlockEntity>> LONG_DIST_TRANSFORMER =
            BLOCK_ENTITIES.register("long_dist_transformer", () -> BlockEntityType.Builder.of(
                    gregtech.content.block.misc.LongDistanceTransformerBlockEntity::new,
                    GTMiscBlocks.LONG_DIST_TRANSFORMER_ULV.get(), GTMiscBlocks.LONG_DIST_TRANSFORMER_LV.get(),
                    GTMiscBlocks.LONG_DIST_TRANSFORMER_MV.get(), GTMiscBlocks.LONG_DIST_TRANSFORMER_ZPM.get(),
                    GTMiscBlocks.LONG_DIST_TRANSFORMER_UV.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.ExtenderBlockEntity>> EXTENDER =
            BLOCK_ENTITIES.register("extender", () -> BlockEntityType.Builder.of(
                    gregtech.content.block.misc.ExtenderBlockEntity::new,
                    GTMiscBlocks.extenderBlocks()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.block.misc.FilterBlockEntity>> FILTER = BLOCK_ENTITIES.register("filter",()->BlockEntityType.Builder.of(gregtech.content.block.misc.FilterBlockEntity::new,GTMiscBlocks.FILTER_ITEMS.get(),GTMiscBlocks.FILTER_FLUIDS.get(),GTMiscBlocks.FILTER_ITEMS_FLUIDS.get(),GTMiscBlocks.FILTER_OREDICT.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.energy.ZpmModuleBlockEntity>> ZPM_MODULE =
            BLOCK_ENTITIES.register("zpm_module", () -> BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ZpmModuleBlockEntity::new,GTLasers.ZPM.get()).build(null));
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.energy.ZpmDischargerBlockEntity>> ZPM_DISCHARGER =
            BLOCK_ENTITIES.register("zpm_discharger", () -> BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ZpmDischargerBlockEntity::new,GTLasers.ZPM_DISCHARGER_BASIC.get(),GTLasers.ZPM_DISCHARGER_ADVANCED.get(),GTLasers.ZPM_DISCHARGER_ELITE.get()).build(null));
//?}

    private static final Map<String, DeferredRegister<BlockEntityType<?>>> ADDON_ENTITIES = new LinkedHashMap<>();

    private GTBlockEntities() {}

    public static DeferredRegister<BlockEntityType<?>> entities(String namespace) {
        if (REGISTRY_NAMESPACE.equals(namespace)) return BLOCK_ENTITIES;
//? if >=1.21.1 {
        return ADDON_ENTITIES.computeIfAbsent(namespace, ns -> DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ns));
//?} else {
        return ADDON_ENTITIES.computeIfAbsent(namespace, ns -> DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ns));
//?}
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        for (var register : ADDON_ENTITIES.values()) register.register(bus);
        ADDON_ENTITIES.clear();
    }

    public static final java.util.function.Supplier<BlockEntityType<gregtech.content.blockentity.energy.PowerCellBlockEntity>> POWER_CELL =
            BLOCK_ENTITIES.register("power_cell", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.energy.PowerCellBlockEntity::new,
                            GTLegacyEnergyDevices.POWER_CELLS.stream().map(RegisteredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final java.util.function.Supplier<BlockEntityType<gregtech.content.blockentity.energy.MagicFieldAbsorberBlockEntity>> MAGIC_FIELD_ABSORBER =
            BLOCK_ENTITIES.register("magic_field_absorber", () ->
                    BlockEntityType.Builder.of(gregtech.content.blockentity.energy.MagicFieldAbsorberBlockEntity::new,
                            GTLegacyEnergyDevices.MAGIC_FIELD_ABSORBER.get()).build(null));
}
