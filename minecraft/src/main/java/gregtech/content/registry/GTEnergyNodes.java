package gregtech.content.registry;

import gregtech.engine.energy.EnergyNodeSpec;
//? if >=1.21.1 {
/*import gregtech.content.block.energy.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
import gregtech.engine.energy.EnergyNodeSpec.Kind;
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
    private static final List<RegisteredBlock<EnergyNodeBlock>> ALL = new ArrayList<>();

//? if >=1.21.1 {
    /*private GTEnergyNodes() {
    }
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<gregtech.content.blockentity.energy.EnergyNodeBlockEntity>> ENERGY_NODE = ENTITIES.register("energy_node",
            () -> BlockEntityType.Builder.of(gregtech.content.blockentity.energy.EnergyNodeBlockEntity::new, supportedBlocks()).build(null));

    private static Block[] supportedBlocks() {
        var blocks = new ArrayList<Block>();
        ALL.forEach(holder -> blocks.add(holder.get()));
        GTGearboxes.allTransformers().forEach(holder -> blocks.add(holder.get()));
        return blocks.toArray(Block[]::new);
    }

    public static void submit(IEventBus bus) {
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
    private GTEnergyNodes() {
    }
//?}

    public static RegisteredBlock<gregtech.content.block.energy.ReactorCoreBlock> REACTOR_CORE_BLOCK;
    public static RegisteredBlock<ReactorCasingBlock> REACTOR_CASING;
    public static RegisteredBlock<ReactorCore2x2Block> REACTOR_CORE_2X2;

    public static void registerAll(ContentRegistrar registrar) {
        REACTOR_CORE_BLOCK = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "reactor_core",
                () -> new gregtech.content.block.energy.ReactorCoreBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(8.0f, 12.0f)
                                .requiresCorrectToolForDrops()),
                block -> new BlockItem(block, new Item.Properties()));

        REACTOR_CASING = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "reactor_casing",
                ReactorCasingBlock::new,
                block -> new BlockItem(block, new Item.Properties()));

        REACTOR_CORE_2X2 = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "reactor_core_2x2",
                ReactorCore2x2Block::new,
                block -> new BlockItem(block, new Item.Properties()));

        registerSpecifications(registrar);
    }

    public static List<RegisteredBlock<EnergyNodeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(ContentRegistrar registrar, EnergyNodeSpec spec) {
        var block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                spec.id(),
                () -> createBlock(spec),
                node -> new BlockItem(node, itemProperties(spec)));
        ALL.add(block);
    }

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

    private static void registerSpecifications(ContentRegistrar registrar) {
        gregtech.content.energy.EnergyNodeDefinitions.specifications().forEach(spec -> add(registrar, spec));
        gregtech.content.energy.MagnetMachineDefinitions.specifications().forEach(spec -> add(registrar, spec));
    }
}
