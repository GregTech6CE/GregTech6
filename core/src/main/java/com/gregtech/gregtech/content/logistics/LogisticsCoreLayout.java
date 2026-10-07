package com.gregtech.gregtech.content.logistics;
import java.util.List;
import java.util.ArrayList;
/** Original GT6 core geometry, CPU limits and energy economics. */
public final class LogisticsCoreLayout {
    public static final int WALL = 18008, VENT = 18299, VERSATILE = 18200,
            LOGIC = 18201, CONTROL = 18202, STORAGE = 18203, CONVERSION = 18204;
    /** GT6 {@code :64}: {@code MAX_STORAGE_CPU_COUNT}. */
    public static final int MAX_STORAGE_CPU_COUNT = 108;
    /** The original scans {@code tX+i, tY+j, tZ+k} for {@code i,j,k} in {@code [-2,2]} ({@code :117}). */
    public static final int RADIUS = 2;

    /** The three structural classes of {@code :118/:137/:139}. */
    public enum Kind { CPU, VENT, WALL }

    public record Cell(int right,int up,int back,Kind kind) {}
    public record Counts(int logic, int control, int storage, int conversion) {
        /** GT6 {@code :144}: every counter must be positive, and the structure must be complete. */
        public boolean valid() { return logic > 0 && control > 0 && storage > 0 && conversion > 0; }
        /** GT6 {@code :216}: minimum stored energy before a routing pass can start. */
        public long routingThreshold() { return 128L + (long) logic * 64L * conversion; }
        /** GT6 {@code :699}: the nominal buffer ceiling (an incoming packet may exceed it). */
        public long energyCapacity() { return 128L + (long) logic * 256L * conversion; }
        /** GT6 {@code :209,504}: fixed charge on every server tick. */
        public long fixedEnergyPerTick() { return 20L + logic + control + storage + conversion; }
    }

    public static Counts contribution(int id){return switch(id){case VERSATILE->new Counts(1,1,1,1);case LOGIC->new Counts(4,0,0,0);case CONTROL->new Counts(0,4,0,0);case STORAGE->new Counts(0,0,4,0);case CONVERSION->new Counts(0,0,0,4);default->null;};}
    private static final List<Cell> CELLS=create();
    private LogisticsCoreLayout() {}
    public static List<Cell> cells() { return CELLS; }
    private static List<Cell> create() {
        var cells = new ArrayList<Cell>();
        for (int right = -RADIUS; right <= RADIUS; right++) {
            for (int up = -RADIUS; up <= RADIUS; up++) {
                for (int back = 0; back <= 2 * RADIUS; back++) {
                    if (right == 0 && up == 0 && back == 0) continue; // the controller itself
                    int dy = back - RADIUS;                            // squared distance from the centre
                    int d2 = right * right + up * up + dy * dy;
                    cells.add(new Cell(right, up, back, d2 < 4 ? Kind.CPU : d2 > 6 ? Kind.WALL : Kind.VENT));
                }
            }
        }
        return List.copyOf(cells);
    }

}
