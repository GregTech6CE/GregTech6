package gregtech.content.multiblock;

import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

/** Writes a cell's design index onto the part when a structure claims it. */
public final class StructureDesigns {
    private StructureDesigns() {}

    public static void apply(Level level, BlockPos origin, Direction front, List<LargeMachineLayouts.Cell> cells) {
        gregtech.engine.multiblock.tracking.StructureClaims.runQuiet(() -> applyNow(level, origin, front, cells));
    }

    private static void applyNow(Level level, BlockPos origin, Direction front, List<LargeMachineLayouts.Cell> cells) {
        if (level == null || cells == null || origin == null || front == null) return;
        for (LargeMachineLayouts.Cell cell : cells) {
            if (cell.design() < 0 || cell.plainBlock()) continue;
            write(level, cell.at(origin, front), cell.design());
        }
    }

    /** Writes designs on repeat copies that the structure check already resolved. */
    public static void apply(Level level, List<LargeMachineLayouts.Placed> placed) {
        gregtech.engine.multiblock.tracking.StructureClaims.runQuiet(() -> applyNow(level, placed));
    }

    private static void applyNow(Level level, List<LargeMachineLayouts.Placed> placed) {
        if (level == null || placed == null) return;
        for (LargeMachineLayouts.Placed cell : placed) {
            if (cell.cell().design() < 0 || cell.cell().plainBlock()) continue;
            write(level, cell.pos(), cell.cell().design());
        }
    }

    /** Only the cells whose index actually differs are written, so an unchanged match saves nothing. */
    private static void write(Level level, BlockPos pos, int design) {
        if (level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part && part.structureDesign() != design)
            part.applyStructureDesign(design);
    }
}
