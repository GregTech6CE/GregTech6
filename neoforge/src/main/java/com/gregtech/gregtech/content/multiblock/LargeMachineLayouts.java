package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Coordinates and port roles translated from the corresponding GT6 MultiTileEntity classes. */
public final class LargeMachineLayouts {
    public record Cell(int right,int up,int back,int part,Role role) {
        public BlockPos at(BlockPos origin,Direction front) { return origin.relative(front.getClockWise(),right).above(up).relative(front.getOpposite(),back); }
        public Block block() { return switch(part) {
            case 18002 -> GTMultiblocks.TANK_WALL.get();
            case 18022 -> GTMultiblocks.TANK_WALL_DENSE.get();
            case 18100 -> GTMultiblocks.CENTRIFUGE_PART.get();
            case 18101 -> GTMultiblocks.HEAT_TRANSMITTER.get();
            case 18102 -> GTMultiblocks.DISTILLATION_TOWER_PART.get();
            case 18105 -> GTMultiblocks.ELECTROLYZER_PART.get();
            case 0 -> net.minecraft.world.level.block.Blocks.AIR;
            default -> LargeMachineParts.block(part);
        }; }
    }
    public static List<Cell> fromShared(List<SharedLargeMachineLayouts.Cell> cells){return cells==null?null:cells.stream().map(cell->new Cell(cell.right(),cell.up(),cell.back(),cell.part(),Role.valueOf(cell.role().name()))).toList();}
    private static final Map<String,List<Cell>> DEFINITIONS=create();public static List<Cell> cells(String machine){return DEFINITIONS.get(machine);}public static Set<String> machines(){return DEFINITIONS.keySet();}
    private static Map<String,List<Cell>> create(){var result=new LinkedHashMap<String,List<Cell>>();for(String machine:SharedLargeMachineLayouts.machines())result.put(machine,fromShared(SharedLargeMachineLayouts.cells(machine)));return Collections.unmodifiableMap(result);}
}
