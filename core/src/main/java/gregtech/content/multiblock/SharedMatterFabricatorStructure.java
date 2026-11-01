package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.*;

/** GT6 MultiTileEntityMatterFabricator: 97 walls, 26 coils, 16 vents and 9 processors. */
public final class SharedMatterFabricatorStructure {
    private SharedMatterFabricatorStructure() {}

    public static final List<SharedLargeMachineLayouts.Cell> CELLS = create();

    private static List<SharedLargeMachineLayouts.Cell> create() {
        return MultiblockPatterns.cells(StructureLayout.pattern("largemassfab")
                .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                .layer(1, "AAAAA", "ABBBA", "ABBBA", "ABBBA", "AAAAA")
                .layer(2, "AAAAA", "ABBBA", "AB BA", "ABBBA", "AAAAA")
                .layer(3, "AAAAA", "ABBBA", "ABBBA", "ABBBA", "AAAAA")
                .layer(4, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                .layer(5, "CCCCC", "CDDFC", "CDEFC", "CDFFC", "CCCCC")
                .where('A', CellMatch.part("dense_lead_wall"), StructureRole.ITEM_FLUID_ENERGY)
                .where('B', CellMatch.part("large_osmium_coil"), StructureRole.ITEM_FLUID_ENERGY)
                .where(' ', CellMatch.air(), StructureRole.AIR)
                .where('C', CellMatch.part("fusion_ventilation_unit"), StructureRole.CASING)
                .where('D', CellMatch.part("control_processor_unit"), StructureRole.CASING)
                .where('E', CellMatch.part("versatile_processor_unit"), StructureRole.CASING)
                .where('F', CellMatch.part("conversion_processor_unit"), StructureRole.CASING));
    }
}
