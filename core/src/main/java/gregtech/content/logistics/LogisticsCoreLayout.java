package gregtech.content.logistics;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import gregtech.content.multiblock.MultiblockPatterns;
import java.util.List;

/** Original GT6 core geometry, CPU limits and energy economics. */
public final class LogisticsCoreLayout {
    public static final int WALL = 18008,
            VENT = 18299,
            VERSATILE = 18200,
            LOGIC = 18201,
            CONTROL = 18202,
            STORAGE = 18203,
            CONVERSION = 18204;

    /** GT6's: {@code MAX_STORAGE_CPU_COUNT}. */
    public static final int MAX_STORAGE_CPU_COUNT = 108;

    /** The original scans {@code tX+i, tY+j, tZ+k} for {@code i,j,k} in {@code [-2,2]}. */
    public static final int RADIUS = 2;

    /** The three structural classes of. */
    public enum Kind {
        CPU,
        VENT,
        WALL
    }

    public record Cell(int right, int up, int back, Kind kind) {}

    public record Counts(int logic, int control, int storage, int conversion) {

        /** GT6's: every counter must be positive, and the structure must be complete. */
        public boolean valid() {
            return logic > 0 && control > 0 && storage > 0 && conversion > 0;
        }

        /** GT6's: minimum stored energy before a routing pass can start. */
        public long routingThreshold() {
            return 128L + (long) logic * 64L * conversion;
        }

        /** GT6's: the nominal buffer ceiling (an incoming packet may exceed it). */
        public long energyCapacity() {
            return 128L + (long) logic * 256L * conversion;
        }

        /** GT6's: fixed charge on every server tick. */
        public long fixedEnergyPerTick() {
            return 20L + logic + control + storage + conversion;
        }
    }

    public static Counts contribution(int id) {
        return switch (id) {
            case VERSATILE -> new Counts(1, 1, 1, 1);
            case LOGIC -> new Counts(4, 0, 0, 0);
            case CONTROL -> new Counts(0, 4, 0, 0);
            case STORAGE -> new Counts(0, 0, 4, 0);
            case CONVERSION -> new Counts(0, 0, 0, 4);
            default -> null;
        };
    }

    private static final List<Cell> CELLS = create();

    private LogisticsCoreLayout() {}

    public static List<Cell> cells() {
        return CELLS;
    }

    private static List<Cell> create() {
        return MultiblockPatterns.cells(StructureLayout.pattern("logisticscore")
                        .layer(-2, "AAAAA", "ABBBA", "ABBBA", "ABBBA", "AAAAA")
                        .layer(-1, "ABBBA", "BCCCB", "BCCCB", "BCCCB", "ABBBA")
                        .layer(0, "ABSBA", "BCCCB", "BCCCB", "BCCCB", "ABBBA")
                        .layer(1, "ABBBA", "BCCCB", "BCCCB", "BCCCB", "ABBBA")
                        .layer(2, "AAAAA", "ABBBA", "ABBBA", "ABBBA", "AAAAA")
                        .where('A', CellMatch.part("galvanized_steel_wall"), StructureRole.ENERGY_INPUT)
                        .where('B', CellMatch.part("fusion_ventilation_unit"), StructureRole.LOGISTICS)
                        .where('C', CellMatch.part("versatile_processor_unit"), StructureRole.CASING))
                .stream()
                .map(c -> new Cell(
                        c.right(),
                        c.up(),
                        c.back(),
                        c.part() == VERSATILE ? Kind.CPU : c.part() == WALL ? Kind.WALL : Kind.VENT))
                .toList();
    }
}
