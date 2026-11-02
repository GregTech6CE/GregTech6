package gregtech.content.multiblock;

import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.engine.multiblock.tracking.PatternMatch;
import gregtech.engine.multiblock.tracking.StructureClaims;
import gregtech.engine.multiblock.tracking.TrackedMultiblock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** Derived appearance of a formed match. Only state transitions visit these cached part positions. */
public final class StructureDesigns {
    private Map<BlockPos, LargeMachineLayouts.Cell> cells = Map.of();

    public void apply(TrackedMultiblock machine, PatternMatch match) {
        var next = new LinkedHashMap<BlockPos, LargeMachineLayouts.Cell>();
        for (var fixed : match.fixedCells()) add(next, fixed.pos(), fixed.cell());
        for (var repeated : match.repeatPlaced()) add(next, repeated.pos(), repeated.cell());
        StructureClaims.runQuiet(() -> {
            for (var pos : cells.keySet()) if (!next.containsKey(pos)) restore(machine, pos);
        });
        cells = Map.copyOf(next);
        refresh(machine);
    }

    private static void add(
            Map<BlockPos, LargeMachineLayouts.Cell> cells, BlockPos pos, LargeMachineLayouts.Cell cell) {
        if (cell.design() >= 0 && !cell.plainBlock()) cells.put(pos.immutable(), cell);
    }

    public void refresh(TrackedMultiblock machine) {
        StructureClaims.runQuiet(() -> cells.forEach((pos, cell) -> {
            var part = ownedPart(machine, pos);
            if (part == null) return;
            int design = machine.structureDesign(cell, pos);
            if (design < 0) part.restoreStructureDesign();
            else if (part.structureDesign() != design) part.applyStructureDesign(design);
        }));
    }

    public void clear(TrackedMultiblock machine) {
        StructureClaims.runQuiet(() -> cells.keySet().forEach(pos -> restore(machine, pos)));
        cells = Map.of();
    }

    private static void restore(TrackedMultiblock machine, BlockPos pos) {
        var part = ownedPart(machine, pos);
        if (part != null) part.restoreStructureDesign();
    }

    private static MultiblockPortBlockEntity ownedPart(TrackedMultiblock machine, BlockPos pos) {
        var entity = machine.trackedEntity();
        var level = entity.getLevel();
        if (level == null || level.isClientSide || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part
                        && part.isBoundTo(entity.getBlockPos())
                ? part
                : null;
    }
}
