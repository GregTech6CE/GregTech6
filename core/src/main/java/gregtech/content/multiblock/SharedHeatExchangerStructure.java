/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.List;

public final class SharedHeatExchangerStructure {

    private SharedHeatExchangerStructure() {}

    public static final List<SharedLargeMachineLayouts.Cell> CELLS = create();

    private static List<SharedLargeMachineLayouts.Cell> create() {
        return MultiblockPatterns.cells(StructureLayout.pattern("heatexchanger")
                .layer(0, "AAA", "ASA", "AAA")
                .layer(1, "BBB", "BCB", "BBB")
                .where('A', CellMatch.part("dense_tungsten_wall"), StructureRole.FLUID_INPUT)
                .where('B', CellMatch.part("heat_transmitter"), StructureRole.CASING)
                .where('C', CellMatch.part("dense_tungsten_wall"), StructureRole.CASING));
    }

    public static boolean contains(int dx, int dy, int dz) {
        return dx >= -1 && dx <= 1 && dy >= 0 && dy <= 1 && dz >= -1 && dz <= 1;
    }
}
