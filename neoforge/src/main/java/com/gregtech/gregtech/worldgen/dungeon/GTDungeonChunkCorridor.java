package com.gregtech.gregtech.worldgen.dungeon;

/**
 * Port of GT6's {@code DungeonChunkCorridor} ({@code gregapi/worldgen/dungeon/DungeonChunkCorridor.java}).
 *
 * <p>A corridor is a cross carved into its 16×16 cell: the central box (local 5..10) has tiled floor, brick
 * walls and an air interior, its ceiling turns into GT6's glowing glass where the cell reaches open sky or
 * sits under a liquid, and the centre four blocks carry a redstone-brick lamp. Every connected neighbour
 * then gets an arm (10..15 and so on), which overwrites the shared wall and so opens the passage — GT6
 * relies on that write order, and so does this port.</p>
 *
 * <p>A corridor that connects in both axes is a turn and gets a {@link GTDungeonChunkPillar} first; GT6
 * catches failures there because the pillar is optional.</p>
 *
 * <p>Port difference: GT6 decorates some corridors with multi-tile 32110 (a wall decoration). The port has
 * no equivalent block, so that decoration is skipped.</p>
 */
public class GTDungeonChunkCorridor extends GTDungeonChunkPillar {

    @Override
    public boolean generate(GTDungeonData data) {
        boolean connectsX = data.connected(1, 0) || data.connected(-1, 0);
        boolean connectsZ = data.connected(0, 1) || data.connected(0, -1);
        if (connectsX && connectsZ) {
            try {
                super.generate(data);
            } catch (Throwable ignored) {
                // GT6: the pillar is not important enough to fail the entire corridor.
            }
        }

        // GT6's central box (:36-52).
        for (int x = 5; x <= 10; x++) {
            for (int z = 5; z <= 10; z++) {
                for (int y = 0; y <= 4; y++) {
                    if (y == 0) {
                        data.tiles(x, y, z);
                    } else if (x == 5 || x == 10 || z == 5 || z == 10) {
                        data.bricks(x, y, z);
                    } else if (y == 4) {
                        ceilingOrGlass(data, x, z);
                    } else {
                        data.air(x, y, z);
                    }
                }
            }
        }

        // GT6's centre lamp (:81-94).
        if (skyOrLiquid(data, 7, 7) || skyOrLiquid(data, 7, 8) || skyOrLiquid(data, 8, 7)
                || skyOrLiquid(data, 8, 8)) {
            data.glassglow(7, 4, 7);
            data.glassglow(7, 4, 8);
            data.glassglow(8, 4, 7);
            data.glassglow(8, 4, 8);
        } else {
            data.redstoned(7, 4, 7);
            data.lamp(7, 4, 8, 0);
            data.lamp(8, 4, 7, 0);
            data.redstoned(8, 4, 8);
        }

        // GT6's four arms (:96-187). Order matters: an arm overwrites the shared wall into an opening.
        if (data.connected(1, 0)) {
            arm(data, 10, 15, 5, 10, true);
            data.redstoned(13, 4, 6);
            data.lamp(13, 4, 7, 0);
            data.lamp(13, 4, 8, 0);
            data.redstoned(13, 4, 9);
        }
        if (data.connected(-1, 0)) {
            arm(data, 0, 5, 5, 10, true);
            data.redstoned(2, 4, 6);
            data.lamp(2, 4, 7, 0);
            data.lamp(2, 4, 8, 0);
            data.redstoned(2, 4, 9);
        }
        if (data.connected(0, 1)) {
            arm(data, 5, 10, 10, 15, false);
            data.redstoned(6, 4, 13);
            data.lamp(7, 4, 13, 0);
            data.lamp(8, 4, 13, 0);
            data.redstoned(9, 4, 13);
        }
        if (data.connected(0, -1)) {
            arm(data, 5, 10, 0, 5, false);
            data.redstoned(6, 4, 2);
            data.lamp(7, 4, 2, 0);
            data.lamp(8, 4, 2, 0);
            data.redstoned(9, 4, 2);
        }
        return true;
    }

    /** GT6's arm box: tiles on the floor, walls along the two long sides, air inside. */
    private static void arm(GTDungeonData data, int x0, int x1, int z0, int z1, boolean alongX) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y <= 4; y++) {
                    if (y == 0) {
                        data.tiles(x, y, z);
                    } else if (alongX ? (z == z0 || z == z1) : (x == x0 || x == x1)) {
                        data.bricks(x, y, z);
                    } else if (y == 4) {
                        ceilingOrGlass(data, x, z);
                    } else {
                        data.air(x, y, z);
                    }
                }
            }
        }
    }

    /** GT6's ceiling rule: glowing glass where the cell is open to the sky or under a liquid. */
    private static void ceilingOrGlass(GTDungeonData data, int x, int z) {
        if (skyOrLiquid(data, x, z)) {
            data.glassglow(x, 4, z);
        } else {
            data.bricks(x, 4, z);
        }
    }

    /** GT6's {@code WD.liquid(…) || canBlockSeeTheSky(…)} test one block above the ceiling. */
    private static boolean skyOrLiquid(GTDungeonData data, int x, int z) {
        return data.liquid(x, 5, z) || data.seesSky(x, 5, z);
    }
}
