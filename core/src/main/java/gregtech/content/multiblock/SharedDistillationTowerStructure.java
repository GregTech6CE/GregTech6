package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.*;

/** Original 17101 front-bottom tower and heat-transmitter base, preserving role gates. */
public final class SharedDistillationTowerStructure {
    private SharedDistillationTowerStructure() {}

    public static final List<SharedLargeMachineLayouts.Cell> CELLS = create();

    private static List<SharedLargeMachineLayouts.Cell> create() {
        return MultiblockPatterns.cells(StructureLayout.pattern("distillationtower")
                .layer(-1, "AAA", "AAA", "AAA")
                .layer(0, "BSB", "BBB", "BBB")
                .layer(1, "CCC", "CCC", "CCC")
                .layer(2, "CCC", "CCC", "CCC")
                .layer(3, "CCC", "CCC", "CCC")
                .layer(4, "CCC", "CCC", "CCC")
                .layer(5, "CCC", "CCC", "CCC")
                .layer(6, "CCC", "CCC", "CCC")
                .layer(7, "CCC", "CCC", "CCC")
                .where('A', CellMatch.part("heat_transmitter"), StructureRole.ENERGY_INPUT)
                .where('B', CellMatch.part("distillation_tower_part"), StructureRole.ITEM_FLUID_IO)
                .where('C', CellMatch.part("distillation_tower_part"), StructureRole.FLUID_OUTPUT));
    }
}
