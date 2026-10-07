package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.MaterialChemistry.WeightedMaterial;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

import static com.gregtech.gregtech.api.energy.FaceConfig.*;

import com.gregtech.gregtech.registry.GTBlocks;
import static com.gregtech.gregtech.registry.GTMultiblocks.*;

/** Built-in controller/part registration. Structure and processing remain in block entities. */
public final class MultiblockDefinitions {
    private MultiblockDefinitions() {}
    private static RegistryObject<Block> part(String id) {
        var source = OriginalMultiblockPartData.byPath(id);
        return com.gregtech.gregtech.registry.MachineBlockRegistration.block(id, props ->
                java.util.Set.of("large_niobium_titanium_coil", "coke_oven_wall", "distillation_tower_part", "large_gas_turbine_wall", "bedrock_drill_wall", "heat_transmitter", "boiler_wall", "tank_wall", "tank_wall_dense", "implosion_compressor_wall", "centrifuge_part", "electrolyzer_part", "turbine_wall", "large_dynamo_wall", "large_crucible_wall").contains(id)
                        ? new com.gregtech.gregtech.block.machine.MultiblockPortBlock(props) : new Block(props))
                .strength(source.map(OriginalMultiblockPartData.Part::hardness).orElse(6.0f),
                        source.map(OriginalMultiblockPartData.Part::resistance).orElse(6.0f)).register();
    }

    public static void registerAll() {
        LargeMachineParts.register();
        CENTRIFUGE_PART = part("centrifuge_part");
        ELECTROLYZER_PART = part("electrolyzer_part");
        HEAT_TRANSMITTER = part("heat_transmitter");
        DISTILLATION_TOWER_PART = part("distillation_tower_part");
        BOILER_WALL = part("boiler_wall");
        TURBINE_WALL = part("turbine_wall");
        AxialGeneratorDefinitions.registerSteam();
        LARGE_BOILER_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("large_boiler_main", com.gregtech.gregtech.block.machine.LargeBoilerControllerBlock::new)
                .strength(6.0f, 6.0f).register();

        // GT6 17101 registers the Distillation Tower as HU 512 with NBT_INPUT_MIN 1 /
        // NBT_INPUT_MAX 1024 — an explicit range, not the derived input/2..input*2.
        var original=OriginalMultiblockMachineParameters.distillationTower();
        BasicMachineSpec spec=new BasicMachineSpec(original.id(),original.material(),original.machineName(),original.energyType(),original.tier(),original.energyIn(),original.energyOut(),original.hardness(),original.blastResistance(),FaceConfig.from(original.faceConfig()),original.constructionMaterials(),MachineRecipeMaps.DistillationTower,original.parallelLimit(),original.energyInMin(),original.energyInMax());
        DISTILLATION_TOWER_MAIN = MachineRegistry.registerBasicMachine(spec,
                props -> new com.gregtech.gregtech.block.machine.MultiblockControllerBlock(spec, props));

        // Wave 40 multiblock parts
        TANK_WALL = part("tank_wall");
        TANK_WALL_DENSE = part("tank_wall_dense");
        COKE_OVEN_WALL = part("coke_oven_wall");

        // Wave 40 multiblock controllers
        COKE_OVEN_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("coke_oven_main", com.gregtech.gregtech.block.machine.CokeOvenControllerBlock::new)
                .strength(6.0f, 6.0f).register();

        TANK_3X3 = GTBlocks.BLOCKS.register("tank_3x3", () ->
                new com.gregtech.gregtech.block.machine.TankControllerBlock(3,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5.0f, 10.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("tank_3x3",
                () -> new BlockItem(TANK_3X3.get(), new Item.Properties()));

        TANK_5X5 = GTBlocks.BLOCKS.register("tank_5x5", () ->
                new com.gregtech.gregtech.block.machine.TankControllerBlock(5,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5.0f, 10.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("tank_5x5",
                () -> new BlockItem(TANK_5X5.get(), new Item.Properties()));

        // Large Crucible multiblock
        LARGE_CRUCIBLE_WALL = part("large_crucible_wall");
        LARGE_CRUCIBLE_MAIN = GTBlocks.BLOCKS.register("large_crucible_main", () ->
                new com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.STONE)
                                .strength(5.0f, 10.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("large_crucible_main",
                () -> new BlockItem(LARGE_CRUCIBLE_MAIN.get(), new Item.Properties()));

        // Wave 44 multiblock walls
        CRYO_DISTILLATION_WALL = part("cryo_distillation_wall");
        LARGE_GAS_TURBINE_WALL = part("large_gas_turbine_wall");
        LARGE_DYNAMO_WALL = part("large_dynamo_wall");
        HEAT_EXCHANGER_WALL = part("heat_exchanger_wall");

        // Wave 44 multiblock controllers
        CRYO_DISTILLATION_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("cryo_distillation_main", com.gregtech.gregtech.block.machine.CryoDistillationControllerBlock::new)
                .strength(6.0f, 6.0f).register();

        GasTurbineDefinitions.register();

        AxialGeneratorDefinitions.registerDynamos();

        HEAT_EXCHANGER_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("heat_exchanger_main", com.gregtech.gregtech.block.machine.LargeHeatExchangerControllerBlock::new)
                .strength(6.0f, 6.0f).register();

        // Wave 49: F6.9-12 multiblock parts and controllers
        BEDROCK_DRILL_WALL = part("bedrock_drill_wall");
        BEDROCK_DRILL_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("bedrock_drill_main", com.gregtech.gregtech.block.machine.BedrockDrillControllerBlock::new)
                .strength(10.0f, 10.0f).register();

        LIGHTNING_ROD_WALL = part("lightning_rod_wall");
        LIGHTNING_ROD_PILLAR = part("lightning_rod_pillar");
        LARGE_NIOBIUM_TITANIUM_COIL = part("large_niobium_titanium_coil");
        LIGHTNING_ROD_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("lightning_rod_main", com.gregtech.gregtech.block.machine.LightningRodControllerBlock::new)
                .strength(10.0f, 10.0f).register();

        IMPLOSION_COMPRESSOR_WALL = part("implosion_compressor_wall");
        IMPLOSION_COMPRESSOR_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("implosion_compressor_main", com.gregtech.gregtech.block.machine.ImplosionCompressorControllerBlock::new)
                .strength(12.5f, 12.5f).register();

        FUSION_REACTOR_WALL = part("fusion_reactor_wall");
        FUSION_REACTOR_MAIN = com.gregtech.gregtech.registry.MachineBlockRegistration.block("fusion_reactor_main", com.gregtech.gregtech.block.machine.FusionReactorControllerBlock::new)
                .strength(12.5f, 12.5f).register();
    }
}
