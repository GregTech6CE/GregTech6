package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;

/** Upright drill: the bottom layer must be bedrock or a bedrock deposit. */
public final class SharedDrillStructure {
    private SharedDrillStructure() {}

    public static final StructureGrid LAYOUT = MultiblockPatterns.grid(StructureLayout.pattern("bedrockdrill")
            .layer(-5, "AAA", "AAA", "AAA")
            .layer(-4, "AAA", "AAA", "AAA")
            .layer(-3, "BBB", "BBB", "BBB")
            .layer(-2, "BBB", "BBB", "BBB")
            .layer(-1, "BCB", "CBC", "BCB")
            .layer(0, "BBB", "BSB", "BBB")
            .where('A', CellMatch.part("wall"), StructureRole.CASING)
            .where('B', CellMatch.part("wall"), StructureRole.FLUID_INPUT)
            .where('C', CellMatch.part("wall"), StructureRole.ENERGY_INPUT));
}
