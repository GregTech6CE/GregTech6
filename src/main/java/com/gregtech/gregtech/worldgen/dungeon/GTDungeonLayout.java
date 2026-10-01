package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The dungeon's gate and layout, ported from GT6's {@code WorldgenDungeonGT}
 * ({@code gregapi/worldgen/dungeon/WorldgenDungeonGT.java}, registered as
 * {@code new WorldgenDungeonGT("overworld.structure.dungeon.large", T, 100, 3, 7, 20, 20, 6, T, F, F, T, T, T, T, T,
 * GEN_OVERWORLD, …)} in {@code Loader_Worldgen:652}).
 *
 * <table>
 *   <caption>GT6's registration values, kept as constants here</caption>
 *   <tr><td>probability</td><td>100 — one anchor per 100 chunks</td></tr>
 *   <tr><td>min/max size</td><td>3..7 cells per side</td></tr>
 *   <tr><td>min/max Y</td><td>20..20 (the dungeon sits 20 blocks above bedrock)</td></tr>
 *   <tr><td>room chance</td><td>6</td></tr>
 *   <tr><td>important rooms</td><td>2 (one barracks, one entrance)</td></tr>
 * </table>
 *
 * <p><b>Port difference:</b> GT6's world generator called this once per chunk and let that single call write
 * the whole dungeon (up to 9×9 chunks) after force-loading the neighbours. Modern world generation
 * forbids writing outside the chunk being decorated, so the port instead generates the dungeon
 * <em>cell by cell</em>: every chunk of the grid rebuilds the same layout from a seed derived from the
 * grid anchor and builds only its own cell. The gate, the layout algorithm and the cell contents are
 * GT6's; only the random stream is split — GT6 shared one {@code Random} across the whole dungeon, the
 * port derives a stable per-cell stream from the anchor seed.</p>
 */
public final class GTDungeonLayout {

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

    private GTDungeonLayout() {}

    /**
     * The anchor chunk of the dungeon grid that could contain this chunk, or null when no anchor is
     * close enough. GT6 generates the dungeon from the chunk that satisfies
     * {@code abs(chunkX) % 11 == 5} (and the same for Z); every other chunk of the grid is reached by
     * that one call. The port lets each cell rebuild the same dungeon, so this looks up the lattice
     * point whose grid may cover the given chunk.
     */
    @Nullable
    public static ChunkPos anchorFor(int chunkX, int chunkZ) {
        int ax = nearestAnchor(chunkX), az = nearestAnchor(chunkZ);
        if (ax == Integer.MIN_VALUE || az == Integer.MIN_VALUE) return null;
        return new ChunkPos(ax, az);
    }

    /**
     * The lattice point of one axis, or {@link Integer#MIN_VALUE} when the given chunk is further than
     * half a layout away from it. The anchors are the coordinates whose absolute value is
     * {@code ANCHOR_OFFSET} modulo {@link #GRID_PERIOD}.
     */
    private static int nearestAnchor(int chunk) {
        int sign = chunk < 0 ? -1 : 1;
        int magnitude = Math.abs(chunk);
        int lattice = ANCHOR_OFFSET + GRID_PERIOD
                * Math.round((magnitude - ANCHOR_OFFSET) / (float) GRID_PERIOD);
        // The widest layout is (2 + MAX_SIZE) cells, so its anchor sits up to that many halves away.
        if (Math.abs(magnitude - lattice) > MAX_LAYOUT_HALF) return Integer.MIN_VALUE;
        return sign * lattice;
    }

    /**
     * GT6's anchor test on one axis ({@code WorldgenDungeonGT:151-152}):
     * {@code Math.abs(chunk) % GRID_PERIOD == ANCHOR_OFFSET}, i.e. {@code 5, 16, 27, ...} and
     * {@code -5, -16, -27, ...}. This is the very rule {@link #anchorFor} snaps a chunk to; the structure
     * placement ({@code com.gregtech.gregtech.worldgen.GTDungeonPlacement}) declares exactly these chunks
     * as its structure chunks, so {@code /locate structure gregtech:gt_dungeon} answers with the same
     * anchors the feature builds a dungeon from. Public because it is that shared rule.
     */
    public static boolean isAnchor(int chunk) {
        return Math.abs((long) chunk) % GRID_PERIOD == ANCHOR_OFFSET;
    }

    /** The grid region of one axis, in vanilla's own {@code floorDiv} sense (region {@code 0} starts at 0). */
    public static int regionOf(int chunk) {
        return Math.floorDiv(chunk, GRID_PERIOD);
    }

    /**
     * The anchor {@link #isAnchor} accepts inside the given grid region. Chunk 0 lies on the border of
     * the regions {@code -1} and {@code 0}; the positive side keeps GT6's {@code ANCHOR_OFFSET} offset
     * (region {@code 0} holds 5, region {@code 1} holds 16) and the negative side its mirror
     * ({@code GRID_PERIOD - ANCHOR_OFFSET}, so region {@code -1} holds -5), which is what
     * {@code abs(chunk) % GRID_PERIOD} means.
     */
    public static int anchorOfRegion(int region) {
        return region * GRID_PERIOD + (region >= 0 ? ANCHOR_OFFSET : GRID_PERIOD - ANCHOR_OFFSET);
    }

    /** The grid cell of a chunk inside a layout, as {@code [i, j]}, or null when it is outside. */
    @Nullable
    public static int[] cellOf(ChunkPos anchor, byte[][] cells, int chunkX, int chunkZ) {
        int i = chunkX - (anchor.x - cells.length / 2);
        int j = chunkZ - (anchor.z - cells[0].length / 2);
        if (i < 0 || j < 0 || i >= cells.length || j >= cells[i].length) return null;
        return new int[]{i, j};
    }

    /** The seed the whole dungeon hangs off: the world seed plus the anchor position. */
    public static long seedFor(ServerLevel level, ChunkPos anchor) {
        return seedFor(level.getSeed(), anchor);
    }

    /**
     * The same seed from a raw world seed. The structure's side needs it without a level -
     * {@code Structure.GenerationContext} hands out the world seed, and
     * {@code GTDungeonStructure.findGenerationPoint} rolls GT6's one-in-a-hundred gate with it so the
     * structure exists exactly where the feature builds a dungeon.
     */
    public static long seedFor(long worldSeed, ChunkPos anchor) {
        long seed = worldSeed;
        seed ^= (long) anchor.x * 341873128712L;
        seed ^= (long) anchor.z * 132897987541L;
        return seed;
    }

    /** GT6's {@code aRandom.nextInt(mProbability) != 0} gate, evaluated once per anchor. */
    public static boolean passesProbability(long dungeonSeed) {
        return RandomSource.create(dungeonSeed).nextInt(PROBABILITY) == 0;
    }

    /** GT6's anchor-based spawn clearance ({@code WorldgenDungeonGT:142-144}). */
    public static boolean passesAnchorClearance(ChunkPos anchor) {
        int minX = anchor.getMinBlockX(), minZ = anchor.getMinBlockZ();
        return Math.abs((long) minX) >= SPAWN_CLEARANCE && Math.abs((long) minZ) >= SPAWN_CLEARANCE;
    }

    /**
     * Gate used by every cell of one dungeon. GT6 also checks the <em>anchor's</em> bedrock once before
     * writing the entire structure. A feature running in a different chunk cannot safely read that
     * anchor: at FEATURES status it can be four cells away and still only at STRUCTURE_STARTS status.
     * Reading the current cell's bedrock made a partially generated dungeon, potentially omitting the
     * cell with a key while the dungeon-wide plan assumed it existed. We therefore keep the common
     * anchor clearance and probability gates and do not make a cell-local bedrock decision. On custom
     * worlds without an anchor bedrock floor this can allow a dungeon GT6 would have rejected, but it
     * never makes different cells disagree about whether their dungeon exists.
     */
    public static boolean passesPosition(WorldGenLevel level, ChunkPos anchor, int cellChunkX, int cellChunkZ) {
        return passesAnchorClearance(anchor);
    }

    /** GT6's {@code mMinY + aRandom.nextInt(Math.max(1, mMaxY - mMinY))}. */
    public static int offsetY(RandomSource random) {
        return MIN_Y + random.nextInt(Math.max(1, MAX_Y - MIN_Y));
    }

    /** The dungeon's Y level in the 1.18+ world (see {@code GTWorldgenScale}). */
    public static int y(WorldGenLevel level, int offsetY) {
        return com.gregtech.gregtech.worldgen.GTWorldgenScale.remapY(level, offsetY);
    }

    /** The two rock types GT6 rolls for the dungeon ({@code BlocksGT.stones[rnd]}). */
    public static StoneType[] rocks(RandomSource random) {
        StoneType[] all = StoneType.values();
        return new StoneType[]{all[random.nextInt(all.length)], all[random.nextInt(all.length)]};
    }

    /** The size of the layout in cells, exactly GT6's {@code 2 + mMinSize + rnd(1 + mMaxSize - mMinSize)}. */
    public static int size(RandomSource random) {
        return 2 + MIN_SIZE + random.nextInt(1 + MAX_SIZE - MIN_SIZE);
    }

    /**
     * GT6's layout algorithm ({@code WorldgenDungeonGT:159-248}): pick two "important" cells
     * ({@code -1} barracks, {@code -2} entrance), scatter up to {@link #ROOM_ID_COUNT} normal rooms,
     * carve a path from every occupied cell towards the centre, and then run GT6's two cleanup passes
     * that remove corridors which would not connect anything.
     *
     * <p>Values: {@code 0} = no dungeon, positive = a room (GT6's room id), {@link #CORRIDOR} = corridor,
     * {@link #BARRACKS} / {@link #ENTRANCE} = the important rooms. The returned array is indexed
     * {@code [i][j]} with {@code i} along X and {@code j} along Z.</p>
     */
    public static byte[][] layout(RandomSource random) {
        // GT6 rolls the two dimensions separately but allocates one rectangular array.
        int rows = size(random), columns = size(random);
        byte[][] cells = new byte[rows][columns];

        // GT6: two important rooms, placed at random inner cells (the countdown runs from -1 down).
        for (int k = -1, tries = 0; k >= -IMPORTANT_ROOM_COUNT && tries < 10000; tries++) {
            int i = 1 + random.nextInt(cells.length - 2);
            int j = 1 + random.nextInt(cells[i].length - 2);
            if (cells[i][j] == 0) cells[i][j] = (byte) k--;
        }

        // GT6: keep scattering rooms until at least two exist.
        int roomCount = 0;
        while (roomCount < 2) {
            for (int i = 1; i < cells.length - 1; i++) {
                for (int j = 1; j < cells[i].length - 1; j++) {
                    if (cells[i][j] == 0 && random.nextInt(ROOM_CHANCE) == 0) {
                        cells[i][j] = (byte) (1 + random.nextInt(ROOM_ID_COUNT));
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

    /** One of GT6's two cleanup passes over the corridor cells. */
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

    /** GT6's per-cell connection count: how many of the four horizontal neighbours are dungeon cells. */
    public static int connectionCount(byte[][] cells, int i, int j) {
        int count = 0;
        if (occupied(cells, i + 1, j)) count++;
        if (occupied(cells, i - 1, j)) count++;
        if (occupied(cells, i, j + 1)) count++;
        if (occupied(cells, i, j - 1)) count++;
        return count;
    }

    // Mob-farm outer platforms can occupy the layout's empty border cells. Outside the
    // layout is empty space, not a fifth neighbour or an index into another dungeon.
    private static boolean occupied(byte[][] cells, int i, int j) {
        return i >= 0 && i < cells.length && j >= 0 && j < cells[i].length && cells[i][j] != EMPTY;
    }

    /** Every cell of a layout, in GT6's own iteration order (row-major over i then j). */
    public static List<int[]> cells(byte[][] cells) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) out.add(new int[]{i, j, cells[i][j]});
        }
        return out;
    }
}
