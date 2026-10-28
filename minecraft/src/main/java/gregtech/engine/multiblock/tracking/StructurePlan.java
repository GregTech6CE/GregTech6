package gregtech.engine.multiblock.tracking;

import gregtech.content.multiblock.LargeMachineLayouts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Read-only projection of the runtime pattern. Preview and tools never own another geometry. */
public final class StructurePlan {
    private StructurePlan() {}

    public record Cell(BlockPos offset, LargeMachineLayouts.Cell specification, CompiledPattern.Matcher matcher) {}

    /** Reuse the live controller's compiled definition without evaluating or forming a structure. */
    public static CompiledPattern compile(TrackedMultiblock machine) {
        var entity = machine.trackedEntity();
        var front = LayoutStructures.facing(entity);
        var tracker = StructureTracking.tracker(entity);
        if (tracker != null
                && tracker.pattern() != null
                && tracker.patternFront() == front
                && tracker.patternEpoch() == StructureEpoch.get()) return tracker.pattern();
        return machine.structureDefinition() instanceof StructureDefinition.Declarative definition
                ? definition.compile(front)
                : null;
    }

    /** Minimum valid repeat counts; optional extensions are left to the player. */
    public static List<Cell> minimum(CompiledPattern pattern) {
        if (pattern == null) return List.of();
        var cells = new ArrayList<Cell>();
        for (int i = 0; i < pattern.fixedCount(); i++)
            cells.add(new Cell(
                    new BlockPos(pattern.dx(i), pattern.dy(i), pattern.dz(i)),
                    pattern.declaredCell(i),
                    pattern.air(i) ? null : pattern.matcher(i)));
        for (int group = 0; group < pattern.groupCount(); group++) {
            int first = pattern.groupFirstCell(group);
            for (int copy = 0; copy < pattern.groupMinimum(group); copy++)
                for (int i = first; i < first + pattern.groupCellCount(group); i++)
                    cells.add(new Cell(
                            new BlockPos(pattern.repeatDx(i), pattern.repeatDy(i, copy), pattern.repeatDz(i)),
                            pattern.declaredRepeatCell(i),
                            pattern.repeatAir(i) ? null : pattern.repeatMatcher(i)));
        }
        return List.copyOf(cells);
    }
}
