package gregtech.content.registry;

import gregtech.content.block.energy.BatteryBoxBlock;
import gregtech.content.block.energy.ElectricTransformerBlock;
import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.block.energy.OriginalHeaterBlock;
import gregtech.content.block.energy.OriginalMotorBlock;
import gregtech.content.block.energy.ReactorCasingBlock;
import gregtech.content.block.energy.ReactorCore2x2Block;
import gregtech.content.block.energy.RotaryConverterBlock;
import gregtech.content.block.energy.SolarPanelBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
*///?}

/**
 * GT6 energy-net node devices: electric motors (EU→RU), dynamos (RU→EU),
 * transformers (EU step-down), steam turbines (steam→RU), solar panels and
 * battery boxes / storage cabinets. Rates follow the original
 * {@code Loader_MultiTileEntities} NBT_INPUT/NBT_OUTPUT values.
 */
public final class GTEnergyNodes {
    private static final List<RegisteredBlock<EnergyNodeBlock>> ALL = new ArrayList<>();

    public static RegisteredBlockEntity<EnergyNodeBlockEntity> ENERGY_NODE;

    private GTEnergyNodes() {
    }

    private static Block[] supportedBlocks() {
        var blocks = new ArrayList<Block>();
        ALL.forEach(holder -> blocks.add(holder.get()));
        GTGearboxes.allTransformers().forEach(holder -> blocks.add(holder.get()));
        return blocks.toArray(Block[]::new);
    }

//? if >=1.21.1 {
    /*public static void submit(IEventBus bus) {
        bus.addListener(GTEnergyNodes::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        var type = ENERGY_NODE.get();
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
                EnergyNodeBlockEntity::energyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                EnergyNodeBlockEntity::itemCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
                EnergyNodeBlockEntity::fluidCapability);
    }
*///?}

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
        ENERGY_NODE = registrar.blockEntity(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "energy_node",
                () -> BlockEntityType.Builder.of(EnergyNodeBlockEntity::new, supportedBlocks()).build(null));
    }

    public static List<RegisteredBlock<EnergyNodeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(ContentRegistrar registrar, EnergyNodeSpec spec) {
        var block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                spec.id(),
                () -> createBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(4.0f, 4.0f)
                        .requiresCorrectToolForDrops()),
                node -> new BlockItem(node, itemProperties(spec)));
        ALL.add(block);
    }

    static EnergyNodeBlock createBlock(EnergyNodeSpec spec, BlockBehaviour.Properties props) {
        return switch (spec.deviceType()) {
            case MOTOR, STEAM_TURBINE -> new OriginalMotorBlock(spec, props);
            case DYNAMO, COOLER -> new RotaryConverterBlock(spec, props);
            case HEATER -> new OriginalHeaterBlock(spec, props);
            case MAGNET -> new MagnetMachineBlock(spec, props);
            case BATTERY_BOX -> new BatteryBoxBlock(spec, props);
            case ELECTRIC_TRANSFORMER -> new ElectricTransformerBlock(spec, props);
            case SOLAR -> new SolarPanelBlock(spec, props.noOcclusion());
            case ROTATION_TRANSFORMER -> new EnergyNodeBlock(spec, props);
            case BUFFERED -> spec.kind() == EnergyNodeSpec.Kind.MAGNET
                    ? new MagnetMachineBlock(spec, props) : new EnergyNodeBlock(spec, props);
        };
    }

    private static Item.Properties itemProperties(EnergyNodeSpec spec) {
        return spec.deviceType() == DeviceType.STEAM_TURBINE
                ? new Item.Properties().stacksTo(16) : new Item.Properties();
    }

    private static void registerSpecifications(ContentRegistrar registrar) {
        gregtech.content.energy.EnergyNodeDefinitions.specifications().forEach(spec -> add(registrar, spec));
        gregtech.content.energy.MagnetMachineDefinitions.specifications().forEach(spec -> add(registrar, spec));
    }
}
