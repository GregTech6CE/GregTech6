package gregtech.content.worldgen;

import gregtech.content.block.stone.StoneType;
import gregtech.content.registry.GTDungeonKeys;
import gregtech.content.worldgen.dungeon.DungeonLayoutRules;
import gregtech.content.worldgen.dungeon.GTDungeonChunk;
import gregtech.content.worldgen.dungeon.GTDungeonChunkBarracks;
import gregtech.content.worldgen.dungeon.GTDungeonChunkCorridor;
import gregtech.content.worldgen.dungeon.GTDungeonChunkCorridor3;
import gregtech.content.worldgen.dungeon.GTDungeonChunkCorridor4;
import gregtech.content.worldgen.dungeon.GTDungeonChunkEntrance;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomEmpty;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomFarmCrop;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomFarmFish;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomFarmMobs;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomLibraryNormal;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomMiningBedrock;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomPortalEnd;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomPortalNether;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomStorage;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomWorkshop;
import gregtech.content.worldgen.dungeon.GTDungeonData;
import gregtech.content.worldgen.dungeon.GTDungeonLayout;
import gregtech.content.worldgen.dungeon.GTDungeonSharedState;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Port of GT6's {@code WorldgenDungeonGT} ({@code gregapi/worldgen/dungeon/WorldgenDungeonGT.java},
 * registered as {@code new WorldgenDungeonGT("overworld.structure.dungeon.large", T, 100, 3, 7, 20, 20, 6, T, F,
 * F, T, T, T, T, T, GEN_OVERWORLD, …)} in {@code Loader_Worldgen}).
 *
 * <p>The dungeon is a grid of chunk-sized cells: {@code -1} barracks and {@code -2} entrance (GT6's two
 * "important" cells), {@code -128} corridors and positive ids for the rooms, all laid out by
 * {@link GTDungeonLayout}. This class turns one cell into blocks; {@link GTDungeonCellPiece} calls it
 * from the structure's piece placement, one cell per chunk, so every dungeon chunk is written while that
 * chunk is generated (see {@link GTDungeonLayout} for the port's cell-by-cell split).</p>
 *
 * <p>Rooms are tried in a shuffled order and a room that declines (returns false) or throws hands the cell
 * to the next candidate, exactly like GT6's rejection loop; an empty room is the fallback. GT6's dead-end
 * list holds the storage room and the five portal rooms (one per dimension pair); the port keeps its
 * storage room plus the Nether and End portal rooms, because the Twilight, Aether and Mystcraft portals
 * are other-mod content. Every cell also carries GT6's five key stacks
 * ({@code WorldgenDungeonGT}), which the barracks, the workshop and the library hand out.</p>
 */
public final class GTDungeonGenerator {

    /** Independent spike-colour draw for each outer cell, unaffected by corridor generation order. */
    private static final long FARM_PLATFORM_SALT = 0x4641524D5F475436L;

    /** GT6's static cell types ({@code WorldgenDungeonGT}). */
    public static final GTDungeonChunk CORRIDOR = new GTDungeonChunkCorridor();

    public static final GTDungeonChunk CORRIDOR3 = new GTDungeonChunkCorridor3();
    public static final GTDungeonChunk CORRIDOR4 = new GTDungeonChunkCorridor4();
    public static final GTDungeonChunk ENTRANCE = new GTDungeonChunkEntrance();
    public static final GTDungeonChunk BARRACKS = new GTDungeonChunkBarracks();
    public static final GTDungeonChunk ROOM_EMPTY = new GTDungeonChunkRoomEmpty();

    /** GT6's {@code ROOMS} list, minus the rooms of mods the port does not have. */
    public static final List<GTDungeonChunk> ROOMS = List.of(
            new GTDungeonChunkRoomWorkshop(),
            new GTDungeonChunkRoomMiningBedrock(),
            new GTDungeonChunkRoomLibraryNormal(),
            new GTDungeonChunkRoomFarmMobs(),
            new GTDungeonChunkRoomFarmCrop(),
            new GTDungeonChunkRoomFarmFish());

    /** GT6's {@code DEAD_END} list, minus the three portal rooms of mods the port does not have. */
    public static final List<GTDungeonChunk> DEAD_END = List.of(
            new GTDungeonChunkRoomStorage(), new GTDungeonChunkRoomPortalNether(), new GTDungeonChunkRoomPortalEnd());

    /** GT6's five dungeon keys ({@code WorldgenDungeonGT}), one per dungeon. */
    public static final int KEY_COUNT = 5;

    private GTDungeonGenerator() {}

    public static boolean roomEnabled(GTDungeonChunk room) {
        if (room instanceof GTDungeonChunkRoomWorkshop) return gregtech.GregTechConfig.Rule.WORKSHOP.enabled();
        if (room instanceof GTDungeonChunkRoomMiningBedrock)
            return gregtech.GregTechConfig.Rule.MINING_BEDROCK.enabled();
        if (room instanceof GTDungeonChunkRoomLibraryNormal) return gregtech.GregTechConfig.Rule.LIBRARY.enabled();
        if (room instanceof GTDungeonChunkRoomFarmMobs) return gregtech.GregTechConfig.Rule.FARM_MOBS.enabled();
        if (room instanceof GTDungeonChunkRoomFarmCrop) return gregtech.GregTechConfig.Rule.FARM_CROP.enabled();
        if (room instanceof GTDungeonChunkRoomFarmFish) return gregtech.GregTechConfig.Rule.FARM_FISH.enabled();
        if (room instanceof GTDungeonChunkRoomPortalNether) return gregtech.GregTechConfig.Rule.PORTAL_NETHER.enabled();
        if (room instanceof GTDungeonChunkRoomPortalEnd) return gregtech.GregTechConfig.Rule.PORTAL_END.enabled();
        return true;
    }

    /**
     * Whether a cell writes any blocks: every non-empty cell does (a declined room falls back to
     * {@link #ROOM_EMPTY}, which always builds), and an empty border cell does when it carries part of
     * the mob farm's outer platform.
     */
    public static boolean writesCell(
            byte[][] cells, GTDungeonSharedState plan, GTDungeonChunkRoomFarmMobs.Footprint farm, int i, int j) {
        return cells[i][j] != GTDungeonLayout.EMPTY || (farm != null && farm.isOuter(i, j));
    }

    /** Builds one cell of the dungeon; called by the structure piece that covers this chunk. */
    public static boolean generateCell(
            WorldGenLevel level,
            ChunkPos anchor,
            GTDungeonLayout.Plan dungeonPlan,
            int i,
            int j,
            long seed) {
        byte[][] cells = dungeonPlan.cells();
        // A saved piece can outlive a config change that shrinks the layout; skip rather than crash.
        if (i < 0 || j < 0 || i >= cells.length || j >= cells[i].length) return false;
        int offsetY = dungeonPlan.offsetY();
        int color = dungeonPlan.color();
        StoneType[] rocks = dungeonPlan.rocks();
        byte kind = cells[i][j];
        GTDungeonSharedState sharedPlan = GTDungeonSharedState.create(cells, seed);
        GTDungeonChunkRoomFarmMobs.Footprint farm = GTDungeonChunkRoomFarmMobs.footprint(cells, sharedPlan);
        if (!writesCell(cells, sharedPlan, farm, i, j)) return false;
        int minX = DungeonLayoutRules.cellChunkX(anchor.x, cells.length, i) * 16;
        int minZ = DungeonLayoutRules.cellChunkZ(anchor.z, cells[0].length, j) * 16;
        int y = GTDungeonLayout.y(level, offsetY);
        int connections = GTDungeonLayout.connectionCount(cells, i, j);
        RandomSource cellRandom = RandomSource.create(cellSeed(seed, i, j));
        GTDungeonSharedState.CellState shared = sharedPlan.cell(i, j);
        long[] keyIds = new long[KEY_COUNT];
        for (int key = 0; key < KEY_COUNT; key++) keyIds[key] = keySeed(seed, key);
        ItemStack[] keyStacks = new ItemStack[KEY_COUNT];
        for (int key = 0; key < KEY_COUNT; key++) keyStacks[key] = keyStack(keyIds, key);
        GTDungeonData data = new GTDungeonData(
                level,
                minX,
                y,
                minZ,
                rocks[0],
                rocks[1],
                color,
                cells,
                i,
                j,
                connections,
                keyIds,
                keyStacks,
                shared == null ? new boolean[KEY_COUNT] : shared.keysBefore(),
                new HashSet<>(),
                shared == null ? new HashSet<>() : new HashSet<>(shared.tagsBefore()),
                cellRandom,
                shared);
        boolean generated = false;
        if (kind > 0) {
            // GT6 removes a random entry from the candidate list until one accepts the cell.
            List<GTDungeonChunk> candidates = new ArrayList<>(connections == 1 ? DEAD_END : ROOMS);
            Collections.shuffle(candidates, new java.util.Random(cellRandom.nextLong()));
            for (GTDungeonChunk candidate : candidates) {
                if (!roomEnabled(candidate)) continue;
                // A tagged room belongs to exactly one cell in the immutable dungeon plan. This is
                // independent of which chunk the world generator happens to visit first.
                if (GTDungeonSharedState.tagFor(candidate) != null && candidate != shared.chosenRoom()) continue;
                if (tryGenerate(candidate, data)) {
                    generated = true;
                    break;
                }
            }
            if (!generated) generated = tryGenerate(ROOM_EMPTY, data);
        } else if (kind < 0) {
            generated = switch (kind) {
                case GTDungeonLayout.CORRIDOR ->
                    connections == 4
                            ? tryGenerate(CORRIDOR4, data)
                            : connections == 3 ? tryGenerate(CORRIDOR3, data) : tryGenerate(CORRIDOR, data);
                case GTDungeonLayout.ENTRANCE -> tryGenerate(ENTRANCE, data);
                case GTDungeonLayout.BARRACKS -> tryGenerate(BARRACKS, data);
                default -> false;
            };
        }

        if (farm != null && farm.includes(i, j)) {
            GTDungeonData platformData = data;
            if (farm.isOuter(i, j)) {
                // The target chunk owns its own 16x16 shell. GT6's cross-cell writes become a separate
                // deterministic pass, so another chunk never has to be loaded or modified here.
                platformData = new GTDungeonData(
                        level,
                        minX,
                        y,
                        minZ,
                        rocks[0],
                        rocks[1],
                        color,
                        cells,
                        i,
                        j,
                        connections,
                        keyIds,
                        keyStacks,
                        data.generatedKeys,
                        data.lightUpdates,
                        data.tags,
                        RandomSource.create(cellSeed(seed ^ FARM_PLATFORM_SALT, i, j)),
                        shared);
                try {
                    GTDungeonChunkRoomFarmMobs.generateOuterPlatform(platformData);
                    generated = true;
                } catch (Throwable error) {
                    Mc.logger().warn("[gregtech] dungeon mob farm outer cell {}/{} failed: {}", i, j, error.toString());
                    return generated;
                }
            }
            GTDungeonChunkRoomFarmMobs.addPipeFragments(platformData, farm, i, j);
        }
        return generated;
    }

    /** GT6 wraps every cell type in a try/catch so one broken room cannot fail the dungeon. */
    public static boolean tryGenerate(GTDungeonChunk chunk, GTDungeonData data) {
        try {
            return chunk.generate(data);
        } catch (Throwable error) {
            Mc.logger().warn("[gregtech] dungeon cell {} failed: {}", chunk.id(), error.toString());
            return false;
        }
    }

    /** The per-cell random seed: GT6 shared one stream, the port derives a stable one per cell. */
    public static long cellSeed(long dungeonSeed, int i, int j) {
        long mixed = dungeonSeed ^ ((long) i * 0x9E3779B97F4A7C15L) ^ ((long) j * 0xC2B2AE3D27D4EB4FL);
        mixed ^= mixed >>> 29;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 32;
        return mixed;
    }

    /**
     * GT6's dungeon key ids ({@code WorldgenDungeonGT}): the first id is random and every further
     * one is exactly one lower ({@code tKeyIDs[i] = tKeyIDs[i - 1] - 1}), so the five ids are always
     * distinct and reproducible from the dungeon seed.
     */
    public static long keySeed(long dungeonSeed, int index) {
        RandomSource random = RandomSource.create(dungeonSeed + 7919L);
        return 1 + random.nextInt(1000000) - index;
    }

    /**
     * One of GT6's five key stacks ({@code WorldgenDungeonGT}):
     * {@code IL.KEYS[aRandom.nextInt(IL.KEYS.length)].getWithNameAndNBT(1, "Key #" + (i + 1),
     * UT.NBT.makeLong(NBT_KEY, tKeyIDs[i]))}.
     *
     * <p>GT6 drew the key <em>type</em> from its one dungeon wide random stream; the port derives it
     * from the key's own id instead, so every cell of a dungeon builds the same five stacks (the port
     * generates the dungeon cell by cell, see {@link GTDungeonLayout}).</p>
     */
    public static ItemStack keyStack(long[] keyIds, int index) {
        int type = RandomSource.create(keyIds[index]).nextInt(GTDungeonKeys.all().size());
        return GTDungeonKeys.stack(type, keyIds[index], index);
    }
}
