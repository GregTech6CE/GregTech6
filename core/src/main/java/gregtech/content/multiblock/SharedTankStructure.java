package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;

/** Hollow tank shapes; the valve chooses material and size, and the host handles six-axis rotation. */
public final class SharedTankStructure {
    private SharedTankStructure() {}

    private static final StructureGrid SIZE_3 = MultiblockPatterns.grid(StructureLayout.pattern("tank_3")
            .layer(-1, "AAA", "AAA", "AAA")
            .layer(0, "ASA", "A A", "AAA")
            .layer(1, "AAA", "AAA", "AAA")
            .where('A', CellMatch.part("wall"), StructureRole.FLUID_IO)
            .where(' ', CellMatch.air(), StructureRole.AIR));
    private static final StructureGrid SIZE_5 = MultiblockPatterns.grid(StructureLayout.pattern("tank_5")
            .layer(-2, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
            .layer(-1, "AAAAA", "A   A", "A   A", "A   A", "AAAAA")
            .layer(0, "AASAA", "A   A", "A   A", "A   A", "AAAAA")
            .layer(1, "AAAAA", "A   A", "A   A", "A   A", "AAAAA")
            .layer(2, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
            .where('A', CellMatch.part("wall"), StructureRole.FLUID_IO)
            .where(' ', CellMatch.air(), StructureRole.AIR));

    public static StructureGrid layout(int size) {
        return switch (size) {
            case 3 -> SIZE_3;
            case 5 -> SIZE_5;
            default -> throw new IllegalArgumentException("Unsupported tank size: " + size);
        };
    }
}
