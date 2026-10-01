package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

import static com.gregtech.gregtech.api.energy.FaceConfig.*;

/**
 * GT6 multiblock machines: part blocks plus the Distillation Tower controller
 * (3×3 heat transmitter base + 3×3×8 tower, controller front-center bottom).
 */
public final class GTMultiblocks {

    public static RegistryObject<Block> CENTRIFUGE_PART;
    public static RegistryObject<Block> ELECTROLYZER_PART;
    public static RegistryObject<Block> HEAT_TRANSMITTER;
    public static RegistryObject<Block> DISTILLATION_TOWER_PART;
    public static RegistryObject<Block> BOILER_WALL;
    public static RegistryObject<Block> TURBINE_WALL;
    public static RegistryObject<Block> LARGE_TURBINE_MAIN;
    public static RegistryObject<BasicMachineBlock> DISTILLATION_TOWER_MAIN;
    public static RegistryObject<Block> LARGE_BOILER_MAIN;
    public static RegistryObject<Block> TANK_WALL;
    public static RegistryObject<Block> TANK_WALL_DENSE;
    public static RegistryObject<Block> COKE_OVEN_WALL;
    public static RegistryObject<Block> COKE_OVEN_MAIN;
    public static RegistryObject<Block> TANK_3X3;
    public static RegistryObject<Block> TANK_5X5;
    public static RegistryObject<Block> LARGE_CRUCIBLE_WALL;
    public static RegistryObject<Block> LARGE_CRUCIBLE_MAIN;
    public static RegistryObject<Block> CRYO_DISTILLATION_WALL;
    public static RegistryObject<Block> CRYO_DISTILLATION_MAIN;
    public static RegistryObject<Block> LARGE_GAS_TURBINE_WALL;
    public static RegistryObject<Block> LARGE_GAS_TURBINE_MAIN;
    public static RegistryObject<Block> LARGE_DYNAMO_WALL;
    public static RegistryObject<Block> LARGE_DYNAMO_MAIN;
    public static RegistryObject<Block> HEAT_EXCHANGER_WALL;
    public static RegistryObject<Block> HEAT_EXCHANGER_MAIN;
    // Wave 49: F6.9-12 multiblock parts and controllers
    public static RegistryObject<Block> BEDROCK_DRILL_WALL;
    public static RegistryObject<Block> BEDROCK_DRILL_MAIN;
    public static RegistryObject<Block> LIGHTNING_ROD_WALL;
    public static RegistryObject<Block> LIGHTNING_ROD_PILLAR;
    public static RegistryObject<Block> LARGE_NIOBIUM_TITANIUM_COIL;
    public static RegistryObject<Block> LIGHTNING_ROD_MAIN;
    public static RegistryObject<Block> IMPLOSION_COMPRESSOR_WALL;
    public static RegistryObject<Block> IMPLOSION_COMPRESSOR_MAIN;
    public static RegistryObject<Block> FUSION_REACTOR_WALL;
    public static RegistryObject<Block> FUSION_REACTOR_MAIN;

    /** Part block tints (tintindex 0 on the colored layer). */
    public static final int PART_TINT = MTColor.STAINLESS;

    private static final class MTColor {
        static final int STAINLESS = 0xC8C8DC;
    }

    private GTMultiblocks() {}
    public static int tintOf(Block block) {
        if(block instanceof com.gregtech.gregtech.block.machine.LargeGasTurbineControllerBlock turbine) return turbine.grade().tint();
        if(block instanceof com.gregtech.gregtech.block.machine.AxialGeneratorBlock generator) return generator.grade().tint();
        Integer extra=com.gregtech.gregtech.content.multiblock.LargeMachineParts.tint(block);
        if(extra!=null)return extra;
        if(block instanceof BasicMachineBlock machine) return machine.basicSpec().material().getColor();
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) return 0xFFFFFF;
        return com.gregtech.gregtech.api.material.GTMaterialRegistry.get(com.gregtech.gregtech.content.multiblock.OriginalMultiblockTintPolicy.material(id.getPath())).getColor();
    }

    public static List<RegistryObject<? extends Block>> texturedBlocks() {
        var result=new java.util.ArrayList<RegistryObject<? extends Block>>(parts());
        result.addAll(List.of(DISTILLATION_TOWER_MAIN,LARGE_BOILER_MAIN,LARGE_TURBINE_MAIN,COKE_OVEN_MAIN,
                TANK_3X3,TANK_5X5,LARGE_CRUCIBLE_MAIN,CRYO_DISTILLATION_MAIN,LARGE_GAS_TURBINE_MAIN,
                LARGE_DYNAMO_MAIN,HEAT_EXCHANGER_MAIN,BEDROCK_DRILL_MAIN,LIGHTNING_ROD_MAIN,
                IMPLOSION_COMPRESSOR_MAIN,FUSION_REACTOR_MAIN));
        result.addAll(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.blocks().subList(1,4));
        for(boolean steam:new boolean[]{true,false})result.addAll(com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions.blocks(steam).subList(1,4));
        return result;
    }

    public static List<RegistryObject<Block>> parts() {
        var result=new java.util.ArrayList<RegistryObject<Block>>(List.of(CENTRIFUGE_PART, ELECTROLYZER_PART, HEAT_TRANSMITTER, DISTILLATION_TOWER_PART, BOILER_WALL, TURBINE_WALL,
                TANK_WALL, TANK_WALL_DENSE, COKE_OVEN_WALL, LARGE_CRUCIBLE_WALL,
                CRYO_DISTILLATION_WALL, LARGE_GAS_TURBINE_WALL, LARGE_DYNAMO_WALL, HEAT_EXCHANGER_WALL,
                BEDROCK_DRILL_WALL, LIGHTNING_ROD_WALL, LIGHTNING_ROD_PILLAR, LARGE_NIOBIUM_TITANIUM_COIL, IMPLOSION_COMPRESSOR_WALL, FUSION_REACTOR_WALL));
        result.addAll(com.gregtech.gregtech.content.multiblock.LargeMachineParts.blocks());
        return List.copyOf(result);
    }

    public static void registerAll() {
        com.gregtech.gregtech.content.multiblock.MultiblockDefinitions.registerAll();
    }
}
