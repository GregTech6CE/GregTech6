package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.registry.*;
import net.minecraft.world.level.block.Blocks;

/**
 * Port of GT6's {@code DungeonChunkCorridor3}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkCorridor3.java:32-205}), the corridor GT6 builds for a
 * cell with exactly three dungeon neighbours ({@code WorldgenDungeonGT:289}).
 *
 * <p>GT6 first lays down {@link GTDungeonChunkCorridor}'s shell (which itself adds the
 * {@link GTDungeonChunkPillar} of a turn), then decorates the one side of the cell that leads nowhere.
 * That dead end is rolled with {@code next(4)}: an alcove with a crafting station and a loot chest, or a
 * cobblestone wall with a container behind it. Every branch returns, so a cell decorates at most one
 * dead end, exactly like GT6.</p>
 *
 * <p>Port differences:</p>
 * <ul>
 *   <li>GT6's chest multi-tile ({@code (next1in2() ? 508 : 8) + next(3)}, {@code :48}) becomes
 *       {@link GTDungeonData#chest}. The port has a single chest block, which always faces north (GT6
 *       turned the multi-tile towards the room), so the roll is evaluated - it consumes GT6's
 *       {@code next1in2()} and {@code next(3)} from the cell's random - and its result is dropped.</li>
 *   <li>GT6's key-locked Safe multi-tile ({@code 3010}, {@code Loader_MultiTileEntities:135}) becomes a
 *       chest with the same loot table. This cell is the one place where GT6 <em>uses</em> a dungeon key
 *       instead of handing one out - the safe is opened with key #3 (or with key #1/#2/#4/#5 when key #3
 *       was never generated) - and it has no key-locked container in the port (its safe is owner based),
 *       so the lock is skipped; the key items themselves exist since the keys batch
 *       ({@code GTDungeonKeys}).</li>
 *   <li>GT6 hands its cup one of twelve drinks ({@code :37}, all of them GT fluids). The drink is
 *       selected separately from the cell's random, as GT6's global {@code RNGSUS} did.</li>
 *   <li>GT6's {@code case 1} (the breakable wall) only builds something when dungeon key #3 exists
 *       ({@code :57-58}). GT6's {@code mGeneratedKeys} is one array for the whole dungeon; the port's is
 *       per cell ({@link GTDungeonData#keyAvailable}), and a corridor cell never hands a key out itself,
 *       so the condition is false here - which is exactly what GT6 does in a dungeon whose key #3 was
 *       never generated. The condition is kept as GT6 wrote it.</li>
 *   <li>{@link GTDungeonData#chest} draws one extra long from the cell's random for the loot seed, where
 *       GT6 stored the loot category in the multi-tile's NBT.</li>
 * </ul>
 */
public class GTDungeonChunkCorridor3 extends GTDungeonChunkCorridor {

    /** GT6's {@code ChestGenHooks.STRONGHOLD_CORRIDOR}, the loot of every container in this cell. */
    private static final String LOOT = "chests/stronghold_corridor";

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6 :35: the corridor shell (and with it the pillar of a turn) always goes first.
        super.generate(data);

        // GT6's four dead ends (:39, :80, :121, :162). Only an open neighbour is decorated, and the
        // first matching branch returns, so the write order below is GT6's.
        if (data.neighbour(1, 0) == 0) {
            switch (data.next(4)) {
                case 0:
                    // Loot chest with a crafting station at the unused end (GT6 :42-55).
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.smooth(11, y, z);
                    for (int y = 1; y <= 3; y++) for (int z = 6; z <= 9; z++) data.air(10, y, z);

                    data.smooth(10, 1, 6);
                    data.set(10, 1, 7, Blocks.CRAFTING_TABLE.defaultBlockState());
                    // GT6 :48 selects metal or reinforced wood, then brass/bronze/steel.
                    data.corridorChest(10, 1, 8, LOOT);
                    data.smooth(10, 1, 9);

                    data.coins(10, 2, 6);
                    GTDungeonCupDrinks.corridor(data, 10, 2, 7);

                    data.coins(10, 2, 9);
                    return true;
                case 1:
                    // Breakable wall, more likely when dungeon key #3 exists (GT6 :57-59).
                    if (!data.generatedKeys[2]) return true;
                    // GT6 falls through into case 2 here.
                case 2:
                    // Breakable wall with the safe at the unused end (GT6 :60-73).
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.cobbles(13, y, z);
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.cobbles(12, y, z);
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.cobbles(11, y, z);
                    for (int y = 1; y <= 3; y++) for (int z = 6; z <= 9; z++) data.cobbles(10, y, z);
                    for (int y = 1; y <= 3; y++) for (int z = 6; z <= 9; z++) data.air(11, y, z);
                    for (int y = 1; y <= 2; y++) for (int z = 7; z <= 8; z++) data.air(12, y, z);

                    data.coins(12, 1, 7);
                    // GT6 :69 is its Key Locked Safe (3010), opened with key #3 (or #1 when that key was
                    // never generated). The port has no key-locked container, so the loot becomes a chest
                    // and the lock - GT6's only use of a key in this cell - is skipped.
                    data.safe(12, 1, 8, LOOT, data.keyIds[data.generatedKeys[2] ? 2 : 0], true);

                    data.coins(12, 2, 8);

                    return true;
                default:
                    break;
            }
            // Default corridor crossing (GT6 :75-76).
            return true;
        }

        if (data.neighbour(-1, 0) == 0) {
            switch (data.next(4)) {
                case 0:
                    // Loot chest with a crafting station at the unused end (GT6 :83-96).
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.smooth(4, y, z);
                    for (int y = 1; y <= 3; y++) for (int z = 6; z <= 9; z++) data.air(5, y, z);

                    data.smooth(5, 1, 6);
                    data.corridorChest(5, 1, 7, LOOT);
                    data.set(5, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
                    data.smooth(5, 1, 9);

                    data.coins(5, 2, 6);

                    GTDungeonCupDrinks.corridor(data, 5, 2, 8);
                    data.coins(5, 2, 9);
                    return true;
                case 1:
                    // Breakable wall, more likely when dungeon key #3 exists (GT6 :97-99).
                    if (!data.generatedKeys[2]) return true;
                    // GT6 falls through into case 2 here.
                case 2:
                    // Breakable wall with the safe at the unused end (GT6 :100-113).
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.cobbles(2, y, z);
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.cobbles(3, y, z);
                    for (int y = 0; y <= 4; y++) for (int z = 5; z <= 10; z++) data.cobbles(4, y, z);
                    for (int y = 1; y <= 3; y++) for (int z = 6; z <= 9; z++) data.cobbles(5, y, z);
                    for (int y = 1; y <= 3; y++) for (int z = 6; z <= 9; z++) data.air(4, y, z);
                    for (int y = 1; y <= 2; y++) for (int z = 7; z <= 8; z++) data.air(3, y, z);

                    // GT6 :109: the safe, locked with key #3 or key #2 - a chest in the port.
                    data.safe(3, 1, 7, LOOT, data.keyIds[data.generatedKeys[2] ? 2 : 1], true);
                    data.coins(3, 1, 8);

                    data.coins(3, 2, 7);

                    return true;
                default:
                    break;
            }
            // Default corridor crossing (GT6 :116-117).
            return true;
        }

        if (data.neighbour(0, 1) == 0) {
            switch (data.next(4)) {
                case 0:
                    // Loot chest with a crafting station at the unused end (GT6 :124-137).
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.smooth(x, y, 11);
                    for (int y = 1; y <= 3; y++) for (int x = 6; x <= 9; x++) data.air(x, y, 10);

                    data.smooth(6, 1, 10);
                    data.set(7, 1, 10, Blocks.CRAFTING_TABLE.defaultBlockState());
                    data.corridorChest(8, 1, 10, LOOT);
                    data.smooth(9, 1, 10);

                    data.coins(6, 2, 10);
                    GTDungeonCupDrinks.corridor(data, 7, 2, 10);

                    data.coins(9, 2, 10);
                    return true;
                case 1:
                    // Breakable wall, more likely when dungeon key #3 exists (GT6 :138-140).
                    if (!data.generatedKeys[2]) return true;
                    // GT6 falls through into case 2 here.
                case 2:
                    // Breakable wall with the safe at the unused end (GT6 :141-153).
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.cobbles(x, y, 13);
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.cobbles(x, y, 12);
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.cobbles(x, y, 11);
                    for (int y = 1; y <= 3; y++) for (int x = 6; x <= 9; x++) data.cobbles(x, y, 10);
                    for (int y = 1; y <= 3; y++) for (int x = 6; x <= 9; x++) data.air(x, y, 11);
                    for (int y = 1; y <= 2; y++) for (int x = 7; x <= 8; x++) data.air(x, y, 12);

                    data.coins(7, 1, 12);
                    // GT6 :151: the safe, locked with key #3 or key #4 - a chest in the port.
                    data.safe(8, 1, 12, LOOT, data.keyIds[data.generatedKeys[2] ? 2 : 3], true);

                    data.coins(8, 2, 12);

                    return true;
                default:
                    break;
            }
            // Default corridor crossing (GT6 :157-158).
            return true;
        }

        if (data.neighbour(0, -1) == 0) {
            switch (data.next(4)) {
                case 0:
                    // Loot chest with a crafting station at the unused end (GT6 :165-178).
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.smooth(x, y, 4);
                    for (int y = 1; y <= 3; y++) for (int x = 6; x <= 9; x++) data.air(x, y, 5);

                    data.smooth(6, 1, 5);
                    data.corridorChest(7, 1, 5, LOOT);
                    data.set(8, 1, 5, Blocks.CRAFTING_TABLE.defaultBlockState());
                    data.smooth(9, 1, 5);

                    data.coins(6, 2, 5);

                    GTDungeonCupDrinks.corridor(data, 8, 2, 5);
                    data.coins(9, 2, 5);
                    return true;
                case 1:
                    // Breakable wall, more likely when dungeon key #3 exists (GT6 :179-181).
                    if (!data.generatedKeys[2]) return true;
                    // GT6 falls through into case 2 here.
                case 2:
                    // Breakable wall with the safe at the unused end (GT6 :182-194).
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.cobbles(x, y, 2);
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.cobbles(x, y, 3);
                    for (int y = 0; y <= 4; y++) for (int x = 5; x <= 10; x++) data.cobbles(x, y, 4);
                    for (int y = 1; y <= 3; y++) for (int x = 6; x <= 9; x++) data.cobbles(x, y, 5);
                    for (int y = 1; y <= 3; y++) for (int x = 6; x <= 9; x++) data.air(x, y, 4);
                    for (int y = 1; y <= 2; y++) for (int x = 7; x <= 8; x++) data.air(x, y, 3);

                    // GT6 :191: the safe, locked with key #3 or key #5 - a chest in the port.
                    data.safe(7, 1, 3, LOOT, data.keyIds[data.generatedKeys[2] ? 2 : 4], true);
                    data.coins(8, 1, 3);

                    data.coins(7, 2, 3);

                    return true;
                default:
                    break;
            }
            // Default corridor crossing (GT6 :198-199).
            return true;
        }

        // Default corridor crossing (GT6 :203-204).
        return true;
    }
}
