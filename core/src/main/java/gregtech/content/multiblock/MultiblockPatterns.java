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
        if (!layout.constraints().isEmpty())
            throw new IllegalArgumentException("Use a constrained layout: " + layout.name());
        return layout.cells().stream().map(MultiblockPatterns::cell).toList();
    }

    public static SharedLargeMachineLayouts.Layout layout(StructurePattern pattern) {
        return layout(pattern.build());
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
                        .toList(),
                layout.constraints());
    }

    private static SharedLargeMachineLayouts.Cell cell(LayoutCell cell) {
        var matches = cell.match() instanceof CellMatch.OneOf one ? one.members() : List.of(cell.match());
        var choices = matches.stream().map(MultiblockPatterns::choice).toList();
        var primary = choices.get(0);
        return new SharedLargeMachineLayouts.Cell(
                cell.right(),
                cell.up(),
                cell.back(),
                primary.part(),
                cell.role(),
                primary.plain(),
                cell.design(),
                choices.subList(1, choices.size()),
                cell.ports());
    }

    private static SharedLargeMachineLayouts.Choice choice(CellMatch match) {
        if (match instanceof CellMatch.Part part) {
            int id = SharedLargeMachineParts.builtinId(part.name());
            if (id < 0) throw new IllegalArgumentException("Unknown built-in part: " + part.name());
            return new SharedLargeMachineLayouts.Choice(id, null);
        }
        return new SharedLargeMachineLayouts.Choice(0, match instanceof CellMatch.Air ? null : match.toString());
    }

    /** Role-only templates are materialized with each controller variant's wall/coil choice. */
    public static StructureGrid grid(StructurePattern pattern) {
        var layout = pattern.build();
        if (layout.cells().stream().anyMatch(cell -> !cell.ports().rules().isEmpty()))
            throw new IllegalArgumentException("Use a full layout for per-cell ports: " + layout.name());
        return new StructureGrid(layout.cells().stream()
                .map(cell -> new StructureGrid.Cell(cell.right(), cell.up(), cell.back(), cell.role()))
                .toList());
    }
}
