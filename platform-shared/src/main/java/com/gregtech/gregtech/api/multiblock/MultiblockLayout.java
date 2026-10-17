package com.gregtech.gregtech.api.multiblock;

import net.minecraft.core.*;
import java.util.*;
import java.util.function.*;

/** Immutable local-coordinate structure definition; validation never mutates the world. */
public final class MultiblockLayout {
    public enum Role { AIR, CASING, LOGISTICS, HEAT_INPUT, CRUCIBLE, FLUID_INPUT, FLUID_OUTPUT, FLUID_IO, ITEM_IO, ITEM_FLUID_IO, ITEM_FLUID_INPUT, ITEM_FLUID_OUTPUT, ENERGY_INPUT, ITEM_FLUID_ENERGY_INPUT, ITEM_FLUID_ENERGY, ENERGY_OUTPUT }
    public record Cell(int right, int up, int back, Role role) {
        public BlockPos at(BlockPos controller, Direction front) {
            return controller.relative(front.getClockWise(),right).above(up).relative(front.getOpposite(),back);
        }
    }
    private final List<Cell> cells;
    public MultiblockLayout(List<Cell> cells) {
        var positions=new HashSet<BlockPos>();
        for(var cell:cells) if(!positions.add(new BlockPos(cell.right(),cell.up(),cell.back()))) throw new IllegalArgumentException("Duplicate structure cell");
        this.cells=List.copyOf(cells);
    }
    public static MultiblockLayout fromShared(StructureGrid layout){return new MultiblockLayout(layout.cells().stream().map(cell->new Cell(cell.right(),cell.up(),cell.back(),Role.valueOf(cell.role().name()))).toList());}
    public List<Cell> cells() { return cells; }
    public boolean matches(BlockPos controller, Direction front, Predicate<BlockPos> loaded, BiPredicate<BlockPos,Role> matches) {
        if(!front.getAxis().isHorizontal()) return false;
        for(var cell:cells) {
            BlockPos pos=cell.at(controller,front);
            if(!loaded.test(pos) || !matches.test(pos,cell.role())) return false;
        }
        return true;
    }
}
