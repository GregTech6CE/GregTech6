package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;

/** GT6 MultiTileEntityLargeTurbine: front-centered, solid 3x3x4 housing. */
public final class SharedTurbineStructure {

    private SharedTurbineStructure() {}

    public static final StructureGrid LAYOUT = create();

    public static StructureGrid.Role role(int right, int up, int back) {
        return StructureGrid.axialRole(0, right, up, back, true);
    }

    private static StructureGrid create() {
        return MultiblockPatterns.grid(StructureLayout.pattern("largeturbine")
                .layer(-1, "AAA", "CCC", "CCC", "CCC")
                .layer(0, "BSB", "DDD", "DDD", "DED")
                .layer(1, "BBB", "DDD", "DDD", "DDD")
                .where('A', CellMatch.part("wall"), StructureRole.FLUID_IO)
                .where('B', CellMatch.part("wall"), StructureRole.FLUID_INPUT)
                .where('C', CellMatch.part("wall"), StructureRole.FLUID_OUTPUT)
                .where('D', CellMatch.part("wall"), StructureRole.CASING)
                .where('E', CellMatch.part("wall"), StructureRole.ENERGY_OUTPUT));
    }
}
