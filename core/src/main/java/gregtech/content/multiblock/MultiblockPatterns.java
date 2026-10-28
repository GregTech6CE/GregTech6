package gregtech.content.multiblock;

import gregtech.api.multiblock.*;
import java.util.List;

/** Built-in part names use the same character-layout API as addon declarations. */
public final class MultiblockPatterns {
    private MultiblockPatterns() {}

    public static List<SharedLargeMachineLayouts.Cell> cells(StructurePattern pattern) {
        return cells(pattern.build());
    }

    public static List<SharedLargeMachineLayouts.Cell> cells(StructureLayout layout) {
        if (!layout.repeats().isEmpty()) throw new IllegalArgumentException("Use a repeated layout: " + layout.name());
        return layout.cells().stream().map(MultiblockPatterns::cell).toList();
    }

    public static SharedLargeMachineLayouts.Layout layout(StructureLayout layout) {
        return new SharedLargeMachineLayouts.Layout(
                layout.cells().stream().map(MultiblockPatterns::cell).toList(),
                layout.repeats().stream()
                        .map(repeat -> new SharedLargeMachineLayouts.Repeat(
                                repeat.key(),
                                repeat.up(),
                                repeat.min(),
                                repeat.max(),
                                repeat.step(),
                                repeat.height(),
                                repeat.cells().stream()
                                        .map(MultiblockPatterns::cell)
                                        .toList()))
                        .toList());
    }

    private static SharedLargeMachineLayouts.Cell cell(LayoutCell cell) {
        int part = 0;
        String plain = null;
        if (cell.match() instanceof CellMatch.Part named) {
            part = OriginalMultiblockPartData.ALL.stream()
                    .filter(p -> p.path().equals(named.name()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown part: " + named.name()))
                    .originalId();
        } else if (!(cell.match() instanceof CellMatch.Air)) {
            plain = cell.match().toString();
        }
        return new SharedLargeMachineLayouts.Cell(
                cell.right(),
                cell.up(),
                cell.back(),
                part,
                StructureGrid.Role.valueOf(cell.role().name()),
                plain,
                cell.design());
    }

    /** Role-only templates are materialized with each controller variant's wall/coil choice. */
    public static StructureGrid grid(StructurePattern pattern) {
        return new StructureGrid(pattern.build().cells().stream()
                .map(cell -> new StructureGrid.Cell(
                        cell.right(),
                        cell.up(),
                        cell.back(),
                        StructureGrid.Role.valueOf(cell.role().name())))
                .toList());
    }
}
