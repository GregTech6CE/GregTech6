package gregtech.content.registry;

import gregtech.internal.energy.EnergyNodeSpec;
//? if >=1.21.1 {
/*import gregtech.content.block.energy.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
import gregtech.internal.energy.EnergyNodeSpec.Kind;
import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.block.energy.ReactorCasingBlock;
import gregtech.content.block.energy.ReactorCore2x2Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
//?}
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.capabilities.*;
import java.util.*;

/^* Registered current energy nodes; reactors retain their native registry boundary. ^/
*///?} else {
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6 energy-net node devices: electric motors (EU→RU), dynamos (RU→EU),
 * transformers (EU step-down), steam turbines (steam→RU), solar panels and
 * battery boxes / storage cabinets. Rates follow the original
 * {@code Loader_MultiTileEntities} NBT_INPUT/NBT_OUTPUT values.
 */
//?}
public final class GTEnergyNodes {
//? if >=1.21.1 {
    /*private GTEnergyNodes() {
    }
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "gt6ce");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "gt6ce");
    public static final DeferredHolder<Block, ReactorCoreBlock> REACTOR_CORE_BLOCK = BLOCKS.register("reactor_core",
            () -> new ReactorCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8, 12).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ReactorCore2x2Block> REACTOR_CORE_2X2 = BLOCKS.register("reactor_core_2x2", ReactorCore2x2Block::new);
    public static final DeferredHolder<Block, ReactorCasingBlock> REACTOR_CASING = BLOCKS.register("reactor_casing", ReactorCasingBlock::new);
    static {
        ITEMS.register("reactor_core", () -> new BlockItem(REACTOR_CORE_BLOCK.get(), new Item.Properties()));
        ITEMS.register("reactor_core_2x2", () -> new BlockItem(REACTOR_CORE_2X2.get(), new Item.Properties()));
        ITEMS.register("reactor_casing", () -> new BlockItem(REACTOR_CASING.get(), new Item.Properties()));
    }
    private static final List<DeferredHolder<Block, EnergyNodeBlock>> ALL = new ArrayList<>();
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<gregtech.content.blockentity.energy.EnergyNodeBlockEntity>> ENERGY_NODE = ENTITIES.register("energy_node",
            () -> BlockEntityType.Builder.of(gregtech.content.blockentity.energy.EnergyNodeBlockEntity::new, supportedBlocks()).build(null));

    private static Block[] supportedBlocks() {
        var blocks = new ArrayList<Block>();
        ALL.forEach(holder -> blocks.add(holder.get()));
        GTGearboxes.allTransformers().forEach(holder -> blocks.add(holder.get()));
        return blocks.toArray(Block[]::new);
    }

    public static List<DeferredHolder<Block, EnergyNodeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(EnergyNodeSpec spec) {
        var block = BLOCKS.register(spec.id(), () -> createBlock(spec));
        ALL.add(block);
        ITEMS.register(spec.id(), () -> new BlockItem(block.get(), itemProperties(spec)));
    }

    public static void register(IEventBus bus) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Energy nodes registered twice");
        registerSpecifications();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(GTEnergyNodes::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        var type = ENERGY_NODE.get();
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
                gregtech.content.blockentity.energy.EnergyNodeBlockEntity::energyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                gregtech.content.blockentity.energy.EnergyNodeBlockEntity::itemCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
                gregtech.content.blockentity.energy.EnergyNodeBlockEntity::fluidCapability);
    }
*///?} else {
    private static final List<RegistryObject<EnergyNodeBlock>> ALL = new ArrayList<>();

    private GTEnergyNodes() {
    }

    public static List<RegistryObject<EnergyNodeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(EnergyNodeSpec spec) {
        RegistryObject<EnergyNodeBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> createBlock(spec));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(), () -> new BlockItem(block.get(),
                itemProperties(spec)));
    }

    public static net.minecraftforge.registries.RegistryObject<gregtech.content.block.energy.ReactorCoreBlock> REACTOR_CORE_BLOCK;

    public static RegistryObject<ReactorCasingBlock> REACTOR_CASING;
    public static RegistryObject<ReactorCore2x2Block> REACTOR_CORE_2X2;

    public static void registerAll() {
        REACTOR_CORE_BLOCK = GTBlocks.BLOCKS.register("reactor_core", () ->
                new gregtech.content.block.energy.ReactorCoreBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(8.0f, 12.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("reactor_core",
                () -> new BlockItem(REACTOR_CORE_BLOCK.get(), new Item.Properties()));

        REACTOR_CASING = GTBlocks.BLOCKS.register("reactor_casing", ReactorCasingBlock::new);
        GTBlocks.BLOCK_ITEMS.register("reactor_casing",
                () -> new BlockItem(REACTOR_CASING.get(), new Item.Properties()));

        REACTOR_CORE_2X2 = GTBlocks.BLOCKS.register("reactor_core_2x2", ReactorCore2x2Block::new);
        GTBlocks.BLOCK_ITEMS.register("reactor_core_2x2",
                () -> new BlockItem(REACTOR_CORE_2X2.get(), new Item.Properties()));

        registerSpecifications();
    }
//?}

    private static EnergyNodeBlock createBlock(EnergyNodeSpec spec) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0f, 4.0f)
                .requiresCorrectToolForDrops();
        if (spec.kind() == EnergyNodeSpec.Kind.SOLAR) props = props.noOcclusion();
        if (gregtech.content.energy.OriginalSteamTurbines.handles(spec))
            return new gregtech.content.block.energy.OriginalMotorBlock(spec, props);
        if (gregtech.content.energy.OriginalThermalConverter.handles(spec))
            return gregtech.content.energy.OriginalThermalConverter.cooler(spec)
                    ? new gregtech.content.block.energy.RotaryConverterBlock(spec, props)
                    : new gregtech.content.block.energy.OriginalHeaterBlock(spec, props);
        if (gregtech.content.energy.OriginalRotaryConverter.handles(spec))
            return gregtech.content.energy.OriginalRotaryConverter.motor(spec)
                    ? new gregtech.content.block.energy.OriginalMotorBlock(spec, props)
                    : new gregtech.content.block.energy.RotaryConverterBlock(spec, props);
        return spec.kind() == EnergyNodeSpec.Kind.MAGNET
                ? new MagnetMachineBlock(spec, props)
                : spec.batterySlots() > 0
                    ? new gregtech.content.block.energy.BatteryBoxBlock(spec, props)
                    : spec.id().startsWith("transformer_")
                        ? new gregtech.content.block.energy.ElectricTransformerBlock(spec, props)
                        : spec.kind() == EnergyNodeSpec.Kind.SOLAR
                            ? new gregtech.content.block.energy.SolarPanelBlock(spec, props)
                            : new EnergyNodeBlock(spec, props);
    }

    private static Item.Properties itemProperties(EnergyNodeSpec spec) {
        return gregtech.content.energy.OriginalSteamTurbines.handles(spec)
                ? new Item.Properties().stacksTo(16) : new Item.Properties();
    }

    private static void registerSpecifications() {
        gregtech.content.energy.EnergyNodeDefinitions.specifications().forEach(GTEnergyNodes::add);
        gregtech.content.energy.MagnetMachineDefinitions.specifications().forEach(GTEnergyNodes::add);
    }
}
