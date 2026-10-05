package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

/** Existing controller geometry shared by validation and JEI while their GT6 behavior is being completed.
 * Unspecified cells retain their existing unconstrained semantics, rather than pretending they require air. */
public final class ControllerStructureLayouts {
    private ControllerStructureLayouts() {}
    private static final class Registered {
        static final Map<Block,Map<BlockPos,Block>> LAYOUTS=create();
    }
    public static Map<Block,Map<BlockPos,Block>> all(){return Registered.LAYOUTS;}
    public static Map<BlockPos,Block> cells(Block controller){return all().get(controller);}
    public static boolean matches(BlockEntity controller){var level=controller.getLevel();if(level==null)return false;var state=controller.getBlockState();boolean coke=state.is(GTMultiblocks.COKE_OVEN_MAIN.get());if(!coke&&!state.is(GTMultiblocks.CRYO_DISTILLATION_MAIN.get()))return false;var wall=coke?GTMultiblocks.COKE_OVEN_WALL.get():GTMultiblocks.CRYO_DISTILLATION_WALL.get();var front=state.getValue(HorizontalDirectionalBlock.FACING);for(var cell:SharedHollowControllerGeometry.cells(1,coke?-1:0,coke?1:4)){var pos=controller.getBlockPos().relative(front.getClockWise(),cell.x()).above(cell.y()).relative(front.getOpposite(),cell.z());if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(wall))return false;}return true;}
    private static Map<Block,Map<BlockPos,Block>> create(){
        var result=new LinkedHashMap<Block,Map<BlockPos,Block>>();
        var coke=new LinkedHashMap<BlockPos,Block>();
        for(var cell:LargeMachineLayouts.fromShared(OriginalCokeOvenRules.cells())) coke.put(new BlockPos(cell.right(),cell.up(),cell.back()),cell.block());
        result.put(GTMultiblocks.COKE_OVEN_MAIN.get(),Map.copyOf(coke));
        for (var valve : TankValveSpec.all())
            result.put(LargeMachineParts.block(valve.originalId()), tank(valve));
        for (var boiler : OriginalLargeBoilerSpecs.all())
            result.put(LargeMachineParts.block(boiler.originalId()), boiler.previewCells());
        result.put(GTMultiblocks.LARGE_CRUCIBLE_MAIN.get(),crucible(GTMultiblocks.LARGE_CRUCIBLE_WALL.get()));
        for (var variant : LargeCrucibleSpecs.all())
            result.put(LargeMachineParts.block(variant.originalId()), crucible(variant.wall()));
        result.put(LargeMachineParts.block(17996), vonDaGraagg());
        result.put(LargeMachineParts.block(17998), OriginalHighTechControllerLayouts.lightningRod());
        result.put(LargeMachineParts.block(17999), OriginalHighTechControllerLayouts.bedrockDrill());
        result.put(GTMultiblocks.CRYO_DISTILLATION_MAIN.get(),hollow(GTMultiblocks.CRYO_DISTILLATION_WALL.get(),1,0,4));
        for(boolean steam:new boolean[]{true,false})for(var entry:AxialGeneratorDefinitions.blocks(steam))
            result.put(entry.get(),AxialGeneratorDefinitions.grade(entry.get()).cells());
        for (int grade = 0; grade < 4; grade++) {
            var originalSteam = LargeMachineParts.block(17211 + grade);
            var originalDynamo = LargeMachineParts.block(17221 + grade);
            result.put(originalSteam, AxialGeneratorDefinitions.grade(originalSteam).cells());
            result.put(originalDynamo, AxialGeneratorDefinitions.grade(originalDynamo).cells());
        }
        var tower=new LinkedHashMap<BlockPos,Block>();
        for(var cell:SharedDistillationTowerStructure.CELLS)tower.put(new BlockPos(cell.right(),cell.up(),cell.back()),cell.part()==18101?GTMultiblocks.HEAT_TRANSMITTER.get():GTMultiblocks.DISTILLATION_TOWER_PART.get());
        result.put(com.gregtech.gregtech.jei.RecipeMachines.machine("distillation_tower_main"),Map.copyOf(tower));
        return Collections.unmodifiableMap(result);
    }
    private static Map<BlockPos,Block> hollow(Block wall,int radius,int minY,int maxY){
        var cells=new LinkedHashMap<BlockPos,Block>();
        for(var cell:SharedHollowControllerGeometry.cells(radius,minY,maxY))cells.put(new BlockPos(cell.x(),cell.y(),cell.z()),wall);
        return Map.copyOf(cells);
    }
    /** Exact GT6 170xx hollow vessel: the valve is the centre of its front wall. */
    private static Map<BlockPos,Block> tank(TankValveSpec valve) {
        int radius = valve.size() / 2;
        var cells = new LinkedHashMap<BlockPos,Block>();
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++)
            for (int z = -radius; z <= radius; z++) {
                BlockPos local = new BlockPos(x, y, z + radius);
                if (local.equals(BlockPos.ZERO)) continue;
                boolean shell = Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius;
                cells.put(local, shell ? valve.wall() : Blocks.AIR);
            }
        return Map.copyOf(cells);
    }
    /** GT6 MultiTileEntityCrucible: main at bottom centre, 24 wall cells, two open inner cells. */
    private static Map<BlockPos,Block> crucible(Block wall) {
        var cells = new LinkedHashMap<BlockPos,Block>();
        for (int y = 0; y < 3; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) {
                if (y != 0) cells.put(new BlockPos(0, y, 0), Blocks.AIR);
            } else cells.put(new BlockPos(x, y, z), wall);
        }
        return Map.copyOf(cells);
    }

    /** GT6 17996: 41 base walls, a five-coil pole and the dense steel crown. */
    private static Map<BlockPos,Block> vonDaGraagg() {
        var cells = new LinkedHashMap<BlockPos,Block>();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x * z) >= 4) continue;
            for (int y = 0; y <= 1; y++) {
                if (x == 0 && y == 0 && z == 0) continue;
                cells.put(new BlockPos(x, y, z), LargeMachineParts.block(18028));
            }
        }
        for (int y = 2; y <= 6; y++) cells.put(new BlockPos(0, y, 0), LargeMachineParts.block(18040));
        cells.put(new BlockPos(0, 7, 0), LargeMachineParts.block(18029));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            cells.put(new BlockPos(x, 6, z), LargeMachineParts.block(18029));
            if (x * z == 0) {
                cells.put(new BlockPos(x, 5, z), LargeMachineParts.block(18029));
                cells.put(new BlockPos(x, 7, z), LargeMachineParts.block(18029));
            }
        }
        return Map.copyOf(cells);
    }
}
