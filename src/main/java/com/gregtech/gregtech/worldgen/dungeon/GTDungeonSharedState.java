package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The dungeon-wide decisions GT6 kept in one mutable {@code DungeonData} while writing all its cells.
 * Modern worldgen writes one chunk at a time, possibly in another order or on another thread. Rebuild
 * this small, immutable plan from the dungeon seed and layout in every cell instead of sharing mutable
 * state between chunk generators.
 *
 * <p>Positive rooms are assigned in GT6's row-major order, before the barracks and corridors. Each
 * cell uses the same candidate shuffle as {@link GTDungeonFeature#generateCell}; a unique room tag is
 * reserved by its first eligible cell. A tagged room can be built only in its reserved cell. A room that
 * later fails a world-dependent check falls back to an untagged room or the empty shell rather than
 * handing its reserved type to another chunk whose generation order is unpredictable.</p>
 */
public final class GTDungeonSharedState {
    private static final long COIN_SALT = 0x434F494E5F475436L;
    private static final long WORKSHOP_KEY_SALT = 0x574F524B5F4B4559L;
    private static final long LIBRARY_KEY_SALT = 0x4C4942525F4B4559L;

    private final CellState[][] cells;

    private GTDungeonSharedState(CellState[][] cells) {
        this.cells = cells;
    }

    public CellState cell(int i, int j) {
        return cells[i][j];
    }

    public static GTDungeonSharedState create(byte[][] layout, long dungeonSeed) {
        CellState[][] states = new CellState[layout.length][layout[0].length];
        String coin = GTDungeonData.COIN_METALS[RandomSource.create(dungeonSeed ^ COIN_SALT)
                .nextInt(GTDungeonData.COIN_METALS.length)];
        Set<String> claimedTags = new HashSet<>();
        boolean[] generatedKeys = new boolean[GTDungeonFeature.KEY_COUNT];

        // GT6 builds all positive rooms before any negative (corridor / important) cell.
        for (int i = 1; i < layout.length - 1; i++) {
            for (int j = 1; j < layout[i].length - 1; j++) {
                if (layout[i][j] <= 0) continue;
                GTDungeonChunk chosen = chooseRoom(layout, dungeonSeed, i, j, claimedTags);
                RandomSource workshopRoll = RandomSource.create(
                        GTDungeonFeature.cellSeed(dungeonSeed ^ WORKSHOP_KEY_SALT, i, j));
                int workshopKey = workshopRoll.nextInt(GTDungeonFeature.KEY_COUNT * 2);
                int workshopSlot = workshopRoll.nextInt(18);
                int[] libraryKeys = libraryKeys(dungeonSeed, i, j);
                states[i][j] = new CellState(chosen, Set.copyOf(claimedTags),
                        generatedKeys.clone(), coin, workshopKey, workshopSlot, libraryKeys);
                String tag = tagFor(chosen);
                if (tag != null) {
                    claimedTags.add(tag);
                    if (chosen instanceof GTDungeonChunkRoomLibraryNormal) {
                        claimedTags.add(GTDungeonChunkRoomLibrary.TAG_LIBRARY);
                    }
                }
                if (chosen instanceof GTDungeonChunkRoomWorkshop && workshopKey < generatedKeys.length) {
                    generatedKeys[workshopKey] = true;
                } else if (chosen instanceof GTDungeonChunkRoomLibraryNormal) {
                    for (int key : libraryKeys) generatedKeys[key] = true;
                }
            }
        }

        for (int i = 1; i < layout.length - 1; i++) {
            for (int j = 1; j < layout[i].length - 1; j++) {
                if (layout[i][j] >= 0) continue;
                states[i][j] = new CellState(null, Set.copyOf(claimedTags),
                        generatedKeys.clone(), coin, -1, -1, new int[0]);
                if (layout[i][j] == GTDungeonLayout.BARRACKS) {
                    for (int key : new int[]{0, 1, 3, 4}) generatedKeys[key] = true;
                }
            }
        }
        return new GTDungeonSharedState(states);
    }

    private static GTDungeonChunk chooseRoom(byte[][] layout, long seed, int i, int j,
                                              Set<String> claimedTags) {
        int connections = GTDungeonLayout.connectionCount(layout, i, j);
        List<GTDungeonChunk> candidates = new ArrayList<>(connections == 1
                ? GTDungeonFeature.DEAD_END : GTDungeonFeature.ROOMS);
        RandomSource random = RandomSource.create(GTDungeonFeature.cellSeed(seed, i, j));
        Collections.shuffle(candidates, new java.util.Random(random.nextLong()));
        for (GTDungeonChunk candidate : candidates) {
            String tag = tagFor(candidate);
            if (tag == null || !claimedTags.contains(tag)) return candidate;
        }
        return GTDungeonFeature.ROOM_EMPTY;
    }

    /** The one-per-dungeon tags used by the ported GT6 room list. */
    public static String tagFor(GTDungeonChunk room) {
        if (room instanceof GTDungeonChunkRoomWorkshop) return GTDungeonChunkRoomWorkshop.TAG_WORKSHOP;
        if (room instanceof GTDungeonChunkRoomMiningBedrock) return GTDungeonChunkRoomMiningBedrock.TAG_MINING_BEDROCK;
        if (room instanceof GTDungeonChunkRoomLibraryNormal) return GTDungeonChunkRoomLibraryNormal.TAG_LIBRARY_NORMAL;
        if (room instanceof GTDungeonChunkRoomFarmMobs) return GTDungeonChunkRoomFarmMobs.TAG_FARM_MOBS;
        if (room instanceof GTDungeonChunkRoomFarmCrop) return GTDungeonChunkRoomFarmCrop.TAG_FARM_CROP;
        if (room instanceof GTDungeonChunkRoomFarmFish) return GTDungeonChunkRoomFarmFish.TAG_FARM_FISH;
        if (room instanceof GTDungeonChunkRoomPortalNether) return GTDungeonChunkRoomPortalNether.TAG_PORTAL_NETHER;
        if (room instanceof GTDungeonChunkRoomPortalEnd) return GTDungeonChunkRoomPortalEnd.TAG_PORTAL_END;
        return null;
    }

    private static int[] libraryKeys(long seed, int i, int j) {
        RandomSource random = RandomSource.create(GTDungeonFeature.cellSeed(seed ^ LIBRARY_KEY_SALT, i, j));
        int[] result = new int[4];
        for (int row = 0; row < result.length; row++) result[row] = random.nextInt(3) + random.nextInt(3);
        return result;
    }

    public static final class CellState {
        private final GTDungeonChunk chosenRoom;
        private final Set<String> tagsBefore;
        private final boolean[] keysBefore;
        private final String coinMetal;
        private final int workshopKey;
        private final int workshopSlot;
        private final int[] libraryKeys;

        private CellState(GTDungeonChunk chosenRoom, Set<String> tagsBefore, boolean[] keysBefore,
                          String coinMetal, int workshopKey, int workshopSlot, int[] libraryKeys) {
            this.chosenRoom = chosenRoom;
            this.tagsBefore = tagsBefore;
            this.keysBefore = keysBefore;
            this.coinMetal = coinMetal;
            this.workshopKey = workshopKey;
            this.workshopSlot = workshopSlot;
            this.libraryKeys = libraryKeys;
        }

        public GTDungeonChunk chosenRoom() { return chosenRoom; }
        public Set<String> tagsBefore() { return tagsBefore; }
        public boolean[] keysBefore() { return keysBefore.clone(); }
        public String coinMetal() { return coinMetal; }
        public int workshopKey() { return workshopKey; }
        public int workshopSlot() { return workshopSlot; }
        public int libraryKey(int row) { return libraryKeys[(row - 1) - (row > 3 ? 1 : 0)]; }
    }
}
