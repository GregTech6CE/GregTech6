package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.*;

/** Controller at 0,0,0; 24 walls and two required air cells of the original open vessel. */
public final class SharedLargeCrucibleStructure {
    private SharedLargeCrucibleStructure() {}

    public record Cell(int x, int y, int z, StructureGrid.Role role, boolean air) {}

    public static final List<Cell> CELLS;

    static {
        CELLS = MultiblockPatterns.grid(StructureLayout.pattern("largecrucible")
                        .layer(0, "AAA", "ASA", "AAA")
                        .layer(1, "BBB", "B B", "BBB")
                        .layer(2, "CCC", "C C", "CCC")
                        .where('A', CellMatch.part("wall"), StructureRole.ENERGY_INPUT)
                        .where('B', CellMatch.part("wall"), StructureRole.CRUCIBLE)
                        .where(' ', CellMatch.air(), StructureRole.AIR)
                        .where('C', CellMatch.part("wall"), StructureRole.ITEM_FLUID_IO))
                .cells()
                .stream()
                .map(c -> new Cell(
                        c.right(),
                        c.up(),
                        c.back(),
                        c.role() == StructureGrid.Role.AIR ? StructureGrid.Role.CASING : c.role(),
                        c.role() == StructureGrid.Role.AIR))
                .toList();
    }
}
