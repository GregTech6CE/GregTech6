/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.StructureGrid;
import java.util.ArrayList;
import java.util.List;

public final class SharedHeatExchangerStructure {
    private SharedHeatExchangerStructure() {}
    public static final List<SharedLargeMachineLayouts.Cell> CELLS = create();

    private static List<SharedLargeMachineLayouts.Cell> create() {
        var cells = new ArrayList<SharedLargeMachineLayouts.Cell>();
        // Source checkStructure2 order also decides which cells a scarce inventory can build.
        for (int y = 0; y < 2; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0 && z == 0) continue;
            cells.add(new SharedLargeMachineLayouts.Cell(x, y, z,
                    y == 0 || x == 0 && z == 0 ? 18024 : 18101,
                    y == 0 ? StructureGrid.Role.FLUID_INPUT : StructureGrid.Role.CASING));
        }
        return List.copyOf(cells);
    }

    public static boolean contains(int dx, int dy, int dz) {
        return dx >= -1 && dx <= 1 && dy >= 0 && dy <= 1 && dz >= -1 && dz <= 1;
    }
}
