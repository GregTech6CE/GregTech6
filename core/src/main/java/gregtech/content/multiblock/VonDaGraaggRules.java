package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;

/** Original GT6 tower geometry, horizontal range and per-tick energy drain. */
public final class VonDaGraaggRules {

    private VonDaGraaggRules() {}

    public static final long CAPACITY = 4096, INPUT_MIN = 256, INPUT_RECOMMENDED = 2048;
    public static final int SENSOR_MAXIMUM = 256;

    public record Cell(int x, int y, int z, int partId, boolean energyInput) {}

    private static final java.util.List<Cell> CELLS = MultiblockPatterns.cells(StructureLayout.pattern("vondagraagg")
                    .layer(0, ".AAA.", "AAAAA", "AASAA", "AAAAA", ".AAA.")
                    .layer(1, ".AAA.", "AAAAA", "AAAAA", "AAAAA", ".AAA.")
                    .layer(2, ".....", ".....", "..B..", ".....", ".....")
                    .layer(3, ".....", ".....", "..B..", ".....", ".....")
                    .layer(4, ".....", ".....", "..B..", ".....", ".....")
                    .layer(5, ".....", "..C..", ".CBC.", "..C..", ".....")
                    .layer(6, ".....", ".CCC.", ".CBC.", ".CCC.", ".....")
                    .layer(7, ".....", "..C..", ".CCC.", "..C..", ".....")
                    .ignore('.')
                    .where('A', CellMatch.part("dense_galvanized_steel_wall"), StructureRole.ENERGY_INPUT)
                    .where('B', CellMatch.part("large_copper_coil"), StructureRole.CASING)
                    .where('C', CellMatch.part("dense_steel_wall"), StructureRole.CASING))
            .stream()
            .map(c -> new Cell(
                    c.right(),
                    c.up(),
                    c.back(),
                    c.part(),
                    c.role() == gregtech.api.multiblock.StructureGrid.Role.ENERGY_INPUT))
            .toList();

    public static java.util.List<Cell> layout() {
        return CELLS;
    }

    public static int range(long energy, boolean formed) {
        return formed ? (int) Math.min(255, Math.max(0, Math.min(energy, CAPACITY)) / 16) : 0;
    }

    public static long afterDrain(long energy) {
        return Math.max(0, energy - CAPACITY);
    }

    public static boolean within(int dx, int dz, int range) {
        return range > 0 && Math.abs((long) dx) <= range && Math.abs((long) dz) <= range;
    }
}
