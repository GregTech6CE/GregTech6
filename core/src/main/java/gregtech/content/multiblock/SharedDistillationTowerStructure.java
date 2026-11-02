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
                .layer(0, "BSB", "BBB", "BDB")
                .layer(1, "CCC", "CCC", "CEC")
                .layer(2, "CCC", "CCC", "CEC")
                .layer(3, "CCC", "CCC", "CEC")
                .layer(4, "CCC", "CCC", "CEC")
                .layer(5, "CCC", "CCC", "CEC")
                .layer(6, "CCC", "CCC", "CEC")
                .layer(7, "CCC", "CCC", "CEC")
                .where('A', CellMatch.part("heat_transmitter"), StructureRole.ENERGY_INPUT)
                .where('B', CellMatch.part("distillation_tower_part"), StructureRole.ITEM_FLUID_IO)
                .where('C', CellMatch.part("distillation_tower_part"), StructureRole.FLUID_OUTPUT)
                .where('D', CellMatch.part("distillation_tower_part"), StructureRole.ITEM_FLUID_IO, 1)
                .where('E', CellMatch.part("distillation_tower_part"), StructureRole.FLUID_OUTPUT, 1));
    }
}
