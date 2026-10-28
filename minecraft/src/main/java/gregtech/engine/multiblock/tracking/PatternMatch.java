package gregtech.engine.multiblock.tracking;

import gregtech.content.multiblock.LargeMachineLayouts;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** A complete incremental match, in the same order the full scan body builds its claim lists. */
public final class PatternMatch {
    public record FixedCell(LargeMachineLayouts.Cell cell, BlockPos pos) {}

    private final List<FixedCell> fixedCells;
    private final List<LargeMachineLayouts.Placed> repeatPlaced;
    private final Map<String, Integer> counts;

    PatternMatch(
            List<FixedCell> fixedCells, List<LargeMachineLayouts.Placed> repeatPlaced, Map<String, Integer> counts) {
        this.fixedCells = List.copyOf(fixedCells);
        this.repeatPlaced = List.copyOf(repeatPlaced);
        this.counts = Map.copyOf(counts);
    }

    public List<FixedCell> fixedCells() {
        return fixedCells;
    }

    public List<LargeMachineLayouts.Placed> repeatPlaced() {
        return repeatPlaced;
    }

    public Map<String, Integer> counts() {
        return counts;
    }
}
