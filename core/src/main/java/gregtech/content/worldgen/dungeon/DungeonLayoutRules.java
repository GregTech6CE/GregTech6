package gregtech.content.worldgen.dungeon;
import java.util.ArrayList;import java.util.List;
/** Complete original lattice, room scatter, corridor carving and both cleanup passes. */
public final class DungeonLayoutRules {private DungeonLayoutRules(){}
    /** GT6's registration values ({@code Loader_Worldgen:652}). */
    public static final int PROBABILITY = 100;
    public static final int MIN_SIZE = 3, MAX_SIZE = 7;
    public static final int MIN_Y = 20, MAX_Y = 20;
    public static final int ROOM_CHANCE = 6;
    /** GT6: {@code ROOM_ID_COUNT = 1}, {@code IMPORTANT_ROOM_COUNT = 2}. */
    public static final int ROOM_ID_COUNT = 1, IMPORTANT_ROOM_COUNT = 2;
    /** GT6's grid period: {@code mMaxSize + 4}. */
    public static final int GRID_PERIOD = MAX_SIZE + 4;
    /** GT6's anchor alignment: {@code (mMaxSize + 4) / 2}. */
    public static final int ANCHOR_OFFSET = GRID_PERIOD / 2;
    /** GT6's spawn protection: {@code 256 + mMaxSize * 16}. */
    public static final int SPAWN_CLEARANCE = 256 + MAX_SIZE * 16;
    /** The widest layout is {@code 2 + MAX_SIZE} cells, i.e. {@code MAX_LAYOUT_HALF} cells per side. */
    public static final int MAX_LAYOUT_HALF = (2 + MAX_SIZE) / 2;

    /** GT6's layout markers. */
    public static final byte EMPTY = 0, CORRIDOR = -128, BARRACKS = -1, ENTRANCE = -2;

    public record Settings(int minSize, int maxSize, int minY, int maxY, int roomChance) {
        public Settings {
            minSize = Math.max(3, Math.min(16, minSize));
            maxSize = Math.max(minSize, Math.min(16, maxSize));
            maxY = Math.max(minY, maxY);
            roomChance = Math.max(1, roomChance);
        }
        public int period() { return maxSize + 4; }
        public int offset() { return period() / 2; }
        public int half() { return (2 + maxSize) / 2; }
        public int clearance() { return 256 + maxSize * 16; }
    }
    public static final Settings DEFAULTS = new Settings(MIN_SIZE, MAX_SIZE, MIN_Y, MAX_Y, ROOM_CHANCE);

    public static int nearestAnchor(int chunk) { return nearestAnchor(chunk, DEFAULTS); }
    public static int nearestAnchor(int chunk, Settings settings) {
        int sign = chunk < 0 ? -1 : 1;
        int magnitude = Math.abs(chunk);
        int lattice = settings.offset() + settings.period()
                * Math.round((magnitude - settings.offset()) / (float) settings.period());
        // The widest layout is (2 + settings.maxSize()) cells, so its anchor sits up to that many halves away.
        if (Math.abs(magnitude - lattice) > settings.half()) return Integer.MIN_VALUE;
        return sign * lattice;
    }
    public static boolean isAnchor(int chunk) { return isAnchor(chunk, DEFAULTS); }
    public static boolean isAnchor(int chunk, Settings settings) {
        return Math.abs((long) chunk) % settings.period() == settings.offset();
    }
    public static int regionOf(int chunk) { return regionOf(chunk, DEFAULTS); }
    public static int regionOf(int chunk, Settings settings) {
        return Math.floorDiv(chunk, settings.period());
    }
    public static int anchorOfRegion(int region) { return anchorOfRegion(region, DEFAULTS); }
    public static int anchorOfRegion(int region, Settings settings) {
        return region * settings.period() + (region >= 0 ? settings.offset() : settings.period() - settings.offset());
    }
    public static int offsetY(java.util.function.IntUnaryOperator random) { return offsetY(random, DEFAULTS); }
    public static int offsetY(java.util.function.IntUnaryOperator random, Settings settings) {
        return settings.minY() + random.applyAsInt(Math.max(1, settings.maxY() - settings.minY()));
    }
    public static int size(java.util.function.IntUnaryOperator random) { return size(random, DEFAULTS); }
    public static int size(java.util.function.IntUnaryOperator random, Settings settings) {
        return 2 + settings.minSize() + random.applyAsInt(1 + settings.maxSize() - settings.minSize());
    }
    public static byte[][] layout(java.util.function.IntUnaryOperator random) { return layout(random, DEFAULTS); }
    public static byte[][] layout(java.util.function.IntUnaryOperator random, Settings settings) {
        // GT6 rolls the two dimensions separately but allocates one rectangular array.
        int rows = size(random, settings), columns = size(random, settings);
        byte[][] cells = new byte[rows][columns];

        // GT6: two important rooms, placed at random inner cells (the countdown runs from -1 down).
        for (int k = -1, tries = 0; k >= -IMPORTANT_ROOM_COUNT && tries < 10000; tries++) {
            int i = 1 + random.applyAsInt(cells.length - 2);
            int j = 1 + random.applyAsInt(cells[i].length - 2);
            if (cells[i][j] == 0) cells[i][j] = (byte) k--;
        }

        // GT6: keep scattering rooms until at least two exist.
        int roomCount = 0;
        while (roomCount < 2) {
            for (int i = 1; i < cells.length - 1; i++) {
                for (int j = 1; j < cells[i].length - 1; j++) {
                    if (cells[i][j] == 0 && random.applyAsInt(settings.roomChance()) == 0) {
                        cells[i][j] = (byte) (1 + random.applyAsInt(ROOM_ID_COUNT));
                        roomCount++;
                    }
                }
            }
        }

        // GT6: carve a corridor from every occupied cell towards the centre.
        for (int i = 1; i < cells.length - 1; i++) {
            for (int j = 1; j < cells[i].length - 1; j++) {
                if (cells[i][j] == 0) continue;
                int a = i, b = j;
                while (a != cells.length / 2) {
                    a += a > cells.length / 2 ? -1 : 1;
                    if (cells[a][b] == 0) cells[a][b] = CORRIDOR; else break;
                }
                while (b != cells[a].length / 2) {
                    b += b > cells[a].length / 2 ? -1 : 1;
                    if (cells[a][b] == 0) cells[a][b] = CORRIDOR; else break;
                }
            }
        }

        // GT6's two cleanup passes.
        settle(cells, false);
        settle(cells, true);
        return cells;
    }
    private static void settle(byte[][] cells, boolean extended) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 1; i < cells.length - 1; i++) {
                for (int j = 1; j < cells[i].length - 1; j++) {
                    if (cells[i][j] != CORRIDOR) continue;
                    // A straight-through piece is fine (GT6's two early "continue" cases).
                    if (cells[i + 1][j] != 0 && cells[i - 1][j] != 0 && cells[i][j - 1] == 0 && cells[i][j + 1] == 0) continue;
                    if (cells[i + 1][j] == 0 && cells[i - 1][j] == 0 && cells[i][j - 1] != 0 && cells[i][j + 1] != 0) continue;

                    int connections = 0;
                    if (cells[i + 1][j] != 0) connections++;
                    if (cells[i - 1][j] != 0) connections++;
                    if (cells[i][j + 1] != 0) connections++;
                    if (cells[i][j - 1] != 0) connections++;
                    if (connections <= 1) {
                        cells[i][j] = EMPTY;
                        changed = true;
                        continue;
                    }
                    if (extended) {
                        if (cells[i + 1][j + 1] != 0) connections++;
                        if (cells[i + 1][j - 1] != 0) connections++;
                        if (cells[i - 1][j + 1] != 0) connections++;
                        if (cells[i - 1][j - 1] != 0) connections++;
                        if (connections >= 7) {
                            cells[i][j] = EMPTY;
                            changed = true;
                            continue;
                        }
                        if (connections == 5) {
                            if (cells[i + 1][j - 1] == 0 && cells[i + 1][j] == 0 && cells[i + 1][j + 1] == 0) {
                                cells[i][j] = EMPTY; changed = true; continue;
                            }
                            if (cells[i - 1][j - 1] == 0 && cells[i - 1][j] == 0 && cells[i - 1][j + 1] == 0) {
                                cells[i][j] = EMPTY; changed = true; continue;
                            }
                            if (cells[i - 1][j + 1] == 0 && cells[i][j + 1] == 0 && cells[i + 1][j + 1] == 0) {
                                cells[i][j] = EMPTY; changed = true; continue;
                            }
                            if (cells[i - 1][j - 1] == 0 && cells[i][j - 1] == 0 && cells[i + 1][j - 1] == 0) {
                                cells[i][j] = EMPTY; changed = true; continue;
                            }
                        }
                    }
                    // GT6's four corner rules.
                    if (cells[i + 1][j] != 0 && cells[i + 1][j + 1] != 0 && cells[i][j + 1] != 0
                            && cells[i - 1][j] == 0 && cells[i][j - 1] == 0) {
                        cells[i][j] = EMPTY; changed = true; continue;
                    }
                    if (cells[i + 1][j] != 0 && cells[i + 1][j - 1] != 0 && cells[i][j - 1] != 0
                            && cells[i - 1][j] == 0 && cells[i][j + 1] == 0) {
                        cells[i][j] = EMPTY; changed = true; continue;
                    }
                    if (cells[i - 1][j] != 0 && cells[i - 1][j + 1] != 0 && cells[i][j + 1] != 0
                            && cells[i + 1][j] == 0 && cells[i][j - 1] == 0) {
                        cells[i][j] = EMPTY; changed = true; continue;
                    }
                    if (cells[i - 1][j] != 0 && cells[i - 1][j - 1] != 0 && cells[i][j - 1] != 0
                            && cells[i + 1][j] == 0 && cells[i][j + 1] == 0) {
                        cells[i][j] = EMPTY;
                        changed = true;
                    }
                }
            }
        }
    }
    public static int connectionCount(byte[][] cells, int i, int j) {
        int count = 0;
        if (occupied(cells, i + 1, j)) count++;
        if (occupied(cells, i - 1, j)) count++;
        if (occupied(cells, i, j + 1)) count++;
        if (occupied(cells, i, j - 1)) count++;
        return count;
    }
    private static boolean occupied(byte[][] cells, int i, int j) {
        return i >= 0 && i < cells.length && j >= 0 && j < cells[i].length && cells[i][j] != EMPTY;
    }
    public static List<int[]> cells(byte[][] cells) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) out.add(new int[]{i, j, cells[i][j]});
        }
        return out;
    }
 public static long seed(long world,int x,int z){return world ^ (long)x*341873128712L ^ (long)z*132897987541L;}
 public static boolean clearance(int minX,int minZ) { return clearance(minX, minZ, DEFAULTS); }
    public static boolean clearance(int minX,int minZ, Settings settings) {return Math.abs((long)minX)>=settings.clearance()&&Math.abs((long)minZ)>=settings.clearance();}
}
