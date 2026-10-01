package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.registry.GTDungeonKeys;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunk;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkBarracks;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkCorridor;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkCorridor3;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkCorridor4;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkEntrance;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomEmpty;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmCrop;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmFish;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmMobs;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomLibraryNormal;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomMiningBedrock;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortalEnd;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortalNether;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomStorage;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomWorkshop;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonSharedState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Port of GT6's {@code WorldgenDungeonGT} ({@code gregapi/worldgen/dungeon/WorldgenDungeonGT.java},
 * registered as {@code new WorldgenDungeonGT("overworld.structure.dungeon.large", T, 100, 3, 7, 20, 20, 6, T, F,
 * F, T, T, T, T, T, GEN_OVERWORLD, …)} in {@code Loader_Worldgen:652}).
 *
 * <p>The dungeon is a grid of chunk-sized cells: {@code -1} barracks and {@code -2} entrance (GT6's two
 * "important" cells), {@code -128} corridors and positive ids for the rooms, all laid out by
 * {@link GTDungeonLayout}. This feature is the dispatcher that turns one cell into blocks — see
 * {@link GTDungeonLayout} for the port's cell-by-cell generation and for the differences to GT6's
 * "one call writes the whole dungeon" model.</p>
 *
 * <p>Rooms are tried in a shuffled order and a room that declines (returns false) or throws hands the cell
 * to the next candidate, exactly like GT6's rejection loop; an empty room is the fallback. GT6's dead-end
 * list holds the storage room and the five portal rooms (one per dimension pair); the port keeps its
 * storage room plus the Nether and End portal rooms, because the Twilight, Aether and Mystcraft portals
 * are other-mod content. Every cell also carries GT6's five key stacks
 * ({@code WorldgenDungeonGT:169-173}), which the barracks, the workshop and the library hand out.</p>
 */
public class GTDungeonFeature extends Feature<NoneFeatureConfiguration> {

    /** Independent spike-colour draw for each outer cell, unaffected by corridor generation order. */
    private static final long FARM_PLATFORM_SALT = 0x4641524D5F475436L;

    /** GT6's static cell types ({@code WorldgenDungeonGT:57-66}). */
    public static final GTDungeonChunk CORRIDOR = new GTDungeonChunkCorridor();
    public static final GTDungeonChunk CORRIDOR3 = new GTDungeonChunkCorridor3();
    public static final GTDungeonChunk CORRIDOR4 = new GTDungeonChunkCorridor4();
    public static final GTDungeonChunk ENTRANCE = new GTDungeonChunkEntrance();
    public static final GTDungeonChunk BARRACKS = new GTDungeonChunkBarracks();
    public static final GTDungeonChunk ROOM_EMPTY = new GTDungeonChunkRoomEmpty();

    /** GT6's {@code ROOMS} list ({@code :85-94}), minus the rooms of mods the port does not have. */
    public static final List<GTDungeonChunk> ROOMS = List.of(
            new GTDungeonChunkRoomWorkshop(),
            new GTDungeonChunkRoomMiningBedrock(),
            new GTDungeonChunkRoomLibraryNormal(),
            new GTDungeonChunkRoomFarmMobs(),
            new GTDungeonChunkRoomFarmCrop(),
            new GTDungeonChunkRoomFarmFish());

    /** GT6's {@code DEAD_END} list ({@code :96-103}), minus the three portal rooms of mods the port does not have. */
    public static final List<GTDungeonChunk> DEAD_END = List.of(
            new GTDungeonChunkRoomStorage(),
            new GTDungeonChunkRoomPortalNether(),
            new GTDungeonChunkRoomPortalEnd());

    /** GT6's five dungeon keys ({@code WorldgenDungeonGT:161}), one per dungeon. */
    public static final int KEY_COUNT = 5;

    public GTDungeonFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        if (server.dimension() != Level.OVERWORLD) return false;
        BlockPos origin = context.origin();
        int chunkX = origin.getX() >> 4, chunkZ = origin.getZ() >> 4;
        ChunkPos anchor = GTDungeonLayout.anchorFor(chunkX, chunkZ);
        if (anchor == null) return false;
        long seed = GTDungeonLayout.seedFor(server, anchor);
        RandomSource random = RandomSource.create(seed);
        // GT6's own consumption order: probability, offset Y, colour, the two rocks, then the layout.
        if (random.nextInt(GTDungeonLayout.PROBABILITY) != 0) return false;
        if (!GTDungeonLayout.passesPosition(level, anchor, chunkX, chunkZ)) return false;
        int offsetY = GTDungeonLayout.offsetY(random);
        int color = random.nextInt(16);
        StoneType[] rocks = GTDungeonLayout.rocks(random);
        byte[][] cells = GTDungeonLayout.layout(random);
        int[] cell = GTDungeonLayout.cellOf(anchor, cells, chunkX, chunkZ);
        if (cell == null) return false;
        return generateCell(level, anchor, cells, cell[0], cell[1], offsetY, color, rocks, seed);
    }

    /** Builds one cell of the dungeon; also the entry point the tests drive directly. */
    public static boolean generateCell(WorldGenLevel level, ChunkPos anchor, byte[][] cells, int i, int j,
                                       int offsetY, int color, StoneType[] rocks, long seed) {
        byte kind = cells[i][j];
        GTDungeonSharedState dungeonPlan = GTDungeonSharedState.create(cells, seed);
        GTDungeonChunkRoomFarmMobs.Footprint farm = GTDungeonChunkRoomFarmMobs.footprint(cells, dungeonPlan);
        if (kind == GTDungeonLayout.EMPTY && (farm == null || !farm.isOuter(i, j))) return false;
        int minX = (anchor.x - cells.length / 2 + i) * 16;
        int minZ = (anchor.z - cells[0].length / 2 + j) * 16;
        int y = GTDungeonLayout.y(level, offsetY);
        int connections = GTDungeonLayout.connectionCount(cells, i, j);
        RandomSource cellRandom = RandomSource.create(cellSeed(seed, i, j));
        GTDungeonSharedState.CellState shared = dungeonPlan.cell(i, j);
        long[] keyIds = new long[KEY_COUNT];
        for (int key = 0; key < KEY_COUNT; key++) keyIds[key] = keySeed(seed, key);
        ItemStack[] keyStacks = new ItemStack[KEY_COUNT];
        for (int key = 0; key < KEY_COUNT; key++) keyStacks[key] = keyStack(keyIds, key);
        GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], color, cells, i, j,
                connections, keyIds, keyStacks, shared == null ? new boolean[KEY_COUNT] : shared.keysBefore(),
                new HashSet<>(), shared == null ? new HashSet<>() : new HashSet<>(shared.tagsBefore()),
                cellRandom, shared);
        boolean generated = false;
        if (kind > 0) {
            // GT6 removes a random entry from the candidate list until one accepts the cell.
            List<GTDungeonChunk> candidates = new ArrayList<>(connections == 1 ? DEAD_END : ROOMS);
            Collections.shuffle(candidates, new java.util.Random(cellRandom.nextLong()));
            for (GTDungeonChunk candidate : candidates) {
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
                case GTDungeonLayout.CORRIDOR -> connections == 4 ? tryGenerate(CORRIDOR4, data)
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
                platformData = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], color,
                        cells, i, j, connections, keyIds, keyStacks, data.generatedKeys,
                        data.lightUpdates, data.tags,
                        RandomSource.create(cellSeed(seed ^ FARM_PLATFORM_SALT, i, j)), shared);
                try {
                    GTDungeonChunkRoomFarmMobs.generateOuterPlatform(platformData);
                    generated = true;
                } catch (Throwable error) {
                    com.mojang.logging.LogUtils.getLogger().warn("[gregtech] dungeon mob farm outer cell {}/{} failed: {}",
                            i, j, error.toString());
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
            com.mojang.logging.LogUtils.getLogger().warn("[gregtech] dungeon cell {} failed: {}",
                    chunk.id(), error.toString());
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
     * GT6's dungeon key ids ({@code WorldgenDungeonGT:170-171}): the first id is random and every further
     * one is exactly one lower ({@code tKeyIDs[i] = tKeyIDs[i - 1] - 1}), so the five ids are always
     * distinct and reproducible from the dungeon seed.
     */
    public static long keySeed(long dungeonSeed, int index) {
        RandomSource random = RandomSource.create(dungeonSeed + 7919L);
        return 1 + random.nextInt(1000000) - index;
    }

    /**
     * One of GT6's five key stacks ({@code WorldgenDungeonGT:172-173}):
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

    /** Registry key of the configured feature ({@code worldgen/configured_feature/gt_dungeon.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_dungeon"));
}
