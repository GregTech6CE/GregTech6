package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.block.machine.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}
import java.util.*;

/** GT6 main housings 17211–17224. Steam quantities are litres, energy quantities are RU/EU. */
public final class AxialGeneratorDefinitions {
    public record Grade(String id, GasTurbineDefinitions.Grade materials, boolean steam, int input, int output) {
        public int inputMaximum() { return input * 2; }
        public int outputMaximum() { return output * 2; }
        public Block wall() { return materials.wall(); }
        public int tint() { return materials.tint(); }
        public Map<BlockPos, Block> cells() { return LAYOUTS.computeIfAbsent(id, key -> createCells()); }
        private Map<BlockPos, Block> createCells() {
            var cells = new LinkedHashMap<BlockPos, Block>();
            for(var cell:SharedTurbineStructure.LAYOUT.cells()){var pos=new BlockPos(cell.right(),cell.up(),cell.back());cells.put(pos,OriginalGeneratorParameters.usesCopperCoil(steam,cell.back())?LargeMachineParts.block(18040):wall());}
            return Collections.unmodifiableMap(cells);
        }
        public boolean accepts(BlockPos cell, Block block) {
            if (OriginalGeneratorParameters.usesCopperCoil(steam,cell.getZ())) return com.gregtech.gregtech.registry.LegacyContentBinding.samePart(block, LargeMachineParts.block(18040));
            return materials.accepts(block) || steam && materials.wallId()==18022 && block==GTMultiblocks.TURBINE_WALL.get();
        }
    }
    public static final List<Grade> STEAM = grades(true), DYNAMO = grades(false);
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block,Block>> STEAM_BLOCKS = new ArrayList<>(), DYNAMO_BLOCKS = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<Block>> STEAM_BLOCKS = new ArrayList<>(), DYNAMO_BLOCKS = new ArrayList<>();
//?}
    private static List<Grade> grades(boolean steam){return (steam?OriginalGeneratorParameters.STEAM:OriginalGeneratorParameters.DYNAMO).stream().map(p->new Grade(p.id(),GasTurbineDefinitions.GRADES.get(p.materialIndex()),p.steam(),p.input(),p.output())).toList();}
    private static final Map<String,Map<BlockPos,Block>> LAYOUTS = new java.util.concurrent.ConcurrentHashMap<>();
    private AxialGeneratorDefinitions() {}
    public static void registerSteam() {
//? if >=1.21.1 {
        /*for(var grade:STEAM) STEAM_BLOCKS.add(GTMultiblocks.registerController(grade.id(),()->new LargeTurbineControllerBlock(grade,net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(grade.materials().hardness(),grade.materials().hardness()))));
*///?} else {
        for(var grade:STEAM) STEAM_BLOCKS.add(MachineBlockRegistration.block(grade.id(),p->new LargeTurbineControllerBlock(grade,p)).strength(grade.materials().hardness(),grade.materials().hardness()).register());
//?}
        GTMultiblocks.LARGE_TURBINE_MAIN=STEAM_BLOCKS.get(0);
    }
    public static void registerDynamos() {
//? if >=1.21.1 {
        /*for(var grade:DYNAMO) DYNAMO_BLOCKS.add(GTMultiblocks.registerController(grade.id(),()->new LargeDynamoControllerBlock(grade,net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(grade.materials().hardness(),grade.materials().hardness()))));
*///?} else {
        for(var grade:DYNAMO) DYNAMO_BLOCKS.add(MachineBlockRegistration.block(grade.id(),p->new LargeDynamoControllerBlock(grade,p)).strength(grade.materials().hardness(),grade.materials().hardness()).register());
//?}
        GTMultiblocks.LARGE_DYNAMO_MAIN=DYNAMO_BLOCKS.get(0);
    }
//? if >=1.21.1 {
    /*public static List<DeferredHolder<Block,Block>> blocks(boolean steam) { return List.copyOf(steam?STEAM_BLOCKS:DYNAMO_BLOCKS); }
*///?} else {
    public static List<RegistryObject<Block>> blocks(boolean steam) { return List.copyOf(steam?STEAM_BLOCKS:DYNAMO_BLOCKS); }
//?}
    public static Block[] registeredBlocks(boolean steam) {
        int first = steam ? 17211 : 17221;
        return java.util.stream.Stream.concat(
//? if >=1.21.1 {
                /*blocks(steam).stream().map(DeferredHolder::get),
*///?} else {
                blocks(steam).stream().map(RegistryObject::get),
//?}
                java.util.stream.IntStream.range(first, first + 4).mapToObj(LargeMachineParts::block))
                .distinct().toArray(Block[]::new);
    }
    public static Grade grade(Block block) { return ((AxialGeneratorBlock)block).grade(); }
}
