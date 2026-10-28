package gregtech.content.multiblock;

import static gregtech.api.multiblock.StructureGrid.Role.*;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;

/** GT6: nine heat transmitters under a hollow 3x3x3 boiler, controller at front-bottom centre. */
public final class SharedBoilerStructure {
    public static final StructureGrid LAYOUT = create();

    private SharedBoilerStructure() {}

    private static StructureGrid create() {
        var layout = MultiblockPatterns.grid(StructureLayout.pattern("largeboiler")
                .layer(-1, "AAA", "AAA", "AAA")
                .layer(0, "BSB", "BBB", "BBB")
                .layer(1, "CCC", "C C", "CCC")
                .layer(2, "CCC", "CCC", "CCC")
                .where('A', CellMatch.part("wall"), StructureRole.HEAT_INPUT)
                .where('B', CellMatch.part("wall"), StructureRole.FLUID_INPUT)
                .where('C', CellMatch.part("wall"), StructureRole.FLUID_OUTPUT)
                .where(' ', CellMatch.air(), StructureRole.AIR));
        var cells = new java.util.ArrayList<>(layout.cells());
        cells.add(new StructureGrid.Cell(0, 0, 0, FLUID_INPUT));
        return new StructureGrid(cells);
    }
}
