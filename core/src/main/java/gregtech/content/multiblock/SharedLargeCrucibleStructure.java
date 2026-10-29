package gregtech.content.multiblock;

import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureRole;
import java.util.*;

/** Controller at 0,0,0; 24 walls and two required air cells of the original open vessel. */
public final class SharedLargeCrucibleStructure {
    private SharedLargeCrucibleStructure() {}

    public record Cell(int x, int y, int z, StructureRole role, boolean air) {}

    public static final List<Cell> CELLS;

    static {
        var cells = new ArrayList<Cell>();
        for (int y = 0; y < 3; y++)
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        if (y > 0) cells.add(new Cell(x, y, z, StructureRole.CASING, true));
                        continue;
                    }
                    cells.add(new Cell(
                            x,
                            y,
                            z,
                            y == 0
                                    ? StructureRole.ENERGY_INPUT
                                    : y == 1 ? StructureRole.CRUCIBLE : StructureRole.ITEM_FLUID_IO,
                            false));
                }
        CELLS = List.copyOf(cells);
    }
}
