package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.energy.OriginalAdjacentEnergyRules;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Finite source-control fixtures: placed native converters, real bound ports and explicit native ticks. */
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_adjacent_energy")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class AdjacentEnergySourceTests {
    private static BlockPos origin(GameTestHelper h) {return h.absolutePos(new BlockPos(2,0,2)).atY(250);}
    private static DistillationTowerControllerBlockEntity tower(GameTestHelper h,BlockPos pos,boolean cold,Direction front) {
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:"+(cold?"cryo_distillation_main":"distillation_tower_main")));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState().setValue(BasicMachineBlock.FACING,front));
        return (DistillationTowerControllerBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static BlockPos sourcePosition(BlockPos pos,Direction front,OriginalAdjacentEnergyRules.Position cell) {
        return pos.relative(front.getClockWise(),cell.right()).above(cell.up()).relative(front.getOpposite(),cell.back());
    }
    private static EnergyNodeBlockEntity source(GameTestHelper h,BlockPos pos,boolean cold,Direction front) {
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:electric_"+(cold?"cooler_lv":"heater_lv")));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState().setValue(EnergyNodeBlock.FACING,front));
        return (EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h,BasicMachineBlockEntity be) {
        BasicMachineBlockEntity.serverTick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);
    }
    private static void tick(GameTestHelper h,EnergyNodeBlockEntity be) {
        EnergyNodeBlockEntity.serverTick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void nineRealSourcesAllTowerFacingsAndBreakRelease(GameTestHelper h) {
        var pos=origin(h);var cells=LargeMachineLayouts.fromShared(SharedDistillationTowerStructure.CELLS);
        for(boolean cold:new boolean[]{false,true})for(var front:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
            var machine=tower(h,pos,cold,front);
            for(var cell:cells)h.getLevel().setBlock(cell.at(pos,front),cell.block().defaultBlockState(),3);
            h.assertTrue(machine.isStructureOk(),"source tower formed for "+cold+"/"+front);
            var sources=new ArrayList<EnergyNodeBlockEntity>();
            for(var cell:OriginalAdjacentEnergyRules.TOWER_SOURCES)sources.add(source(h,sourcePosition(pos,front,cell),cold,Direction.UP));
            tick(h,machine);machine.machineControl(null).setEnabled(false);
            for(var source:sources)h.assertTrue(!source.machineControl(null).enabled()
                    && source.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,64,1,true)==0,
                    "source stop reaches all nine below base and rejects real energy");
            machine.machineControl(null).setEnabled(true);
            for(var source:sources) {
                h.assertTrue(source.machineControl(null).enabled()
                        && source.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,64,1,true)==1,"source resumes all nine");
                tick(h,source);
            }
            h.assertTrue(machine.getEnergyTick()==9*(cold?16:32),"actual nine EU converters transmit source CU/HU into bound base ports");
            var first=sources.get(0);first.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,64,1,true);
            machine.machineControl(null).setEnabled(false);
            h.assertTrue(first.stored()==64,"adjacent stop preserves existing converter buffer");
            tick(h,first);h.assertTrue(first.stored()==0,"stopped source converts/wastes its saved remainder normally");
            var saved=first.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new EnergyNodeBlockEntity(first.getBlockPos(),first.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(!restored.machineControl(null).enabled(),"adjacent stop shares original saved manual stopped flag; method roundtrip only");
            machine.machineControl(null).setEnabled(true);
            var last=sources.get(sources.size()-1);last.machineControl(null).setEnabled(false);tick(h,machine);
            h.assertTrue(!last.machineControl(null).enabled(),"ordinary ticks do not override later manual source stop");
            h.getLevel().setBlockAndUpdate(pos.relative(front),Blocks.STONE.defaultBlockState());
            tick(h,machine);h.assertTrue(last.machineControl(null).enabled(),"native neighbor-update event reissues source request");
            machine.machineControl(null).setEnabled(false);h.getLevel().removeBlock(pos,false);
            for(var source:sources)h.assertTrue(source.machineControl(null).enabled(),"breaking stopped source main releases all nine");
            h.getLevel().removeBlock(pos.relative(front),false);
            for(var cell:cells)h.getLevel().removeBlock(cell.at(pos,front),false);
            for(var source:sources)h.getLevel().removeBlock(source.getBlockPos(),false);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void sourceContractTypeFaceAndSingleBlockNeighbors(GameTestHelper h) {
        var pos=origin(h);
        for(boolean cold:new boolean[]{false,true}) {
            var machine=tower(h,pos,cold,Direction.NORTH);var sources=new ArrayList<EnergyNodeBlockEntity>();
            for(var cell:OriginalAdjacentEnergyRules.TOWER_SOURCES)sources.add(source(h,sourcePosition(pos,Direction.NORTH,cell),cold,Direction.UP));
            var generic=sources.get(0);
            generic.setSpec(EnergyNodeSpec.builder("fixture_generic_source",Materials.StainlessSteel)
                    .texture("fixture").names("Fixture", "Fixture").capacity(64)
                    .input(GregTechTags.Energy.EU,32).output(cold?GregTechTags.Energy.CU:GregTechTags.Energy.HU,16).build());
            h.assertTrue(generic.adjacentEnergyControl()==null && generic.isEnergyEmittingTo(machine.spec().energyTag(),Direction.UP,true),
                    "generic manual switch and compatible energy output alone do not implement original adjacent contract");
            var wrongType=source(h,sources.get(1).getBlockPos(),!cold,Direction.UP);sources.set(1,wrongType);
            var wrongFace=source(h,sources.get(2).getBlockPos(),cold,Direction.EAST);sources.set(2,wrongFace);
            tick(h,machine);machine.machineControl(null).setEnabled(false);
            for(int i=0;i<sources.size();i++)h.assertTrue(sources.get(i).machineControl(null).enabled()==(i<3),
                    "only source contract, matching energy and upward emitter face receive the request");
            generic.machineControl(null).setEnabled(false);machine.machineControl(null).setEnabled(true);
            h.assertTrue(!generic.machineControl(null).enabled(),"generic switch remains unchanged on source resume");
            h.getLevel().removeBlock(pos,false);
            for(var source:sources)h.getLevel().removeBlock(source.getBlockPos(),false);
        }
        var freezer=java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
                .filter(b->b instanceof BasicMachineBlock).map(b->(BasicMachineBlock)b)
                .filter(b->b.basicSpec().machineName().equals("freezer")&&b.basicSpec().tier()==1).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(pos,freezer.defaultBlockState());
        var machine=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(pos);var neighbors=new LinkedHashMap<Direction,EnergyNodeBlockEntity>();
        for(var side:Direction.values()) {
            h.assertTrue(machine.isEnergyAcceptingFrom(GregTechTags.Energy.CU,side,true)==(side==Direction.SOUTH),"source20561 freezer input is back only");
            neighbors.put(side,source(h,pos.relative(side),true,side.getOpposite()));
        }
        tick(h,machine);machine.machineControl(null).setEnabled(false);
        for(var entry:neighbors.entrySet())h.assertTrue(entry.getValue().machineControl(null).enabled()==(entry.getKey()!=Direction.SOUTH),"single-block source request reaches only original accepted back face; other five unchanged");
        machine.machineControl(null).setEnabled(true);
        for(var source:neighbors.values())h.assertTrue(source.machineControl(null).enabled(),"single-block source resume uses theoretical emitter check even after stop");
        h.getLevel().removeBlock(pos,false);for(var source:neighbors.values())h.getLevel().removeBlock(source.getBlockPos(),false);
        h.succeed();
    }
}
