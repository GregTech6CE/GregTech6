package com.gregtech.gregtech.worldgen.dungeon;

/**
 * Port of GT6's {@code DungeonChunkCorridor4}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkCorridor4.java:29-103}), the corridor GT6 builds for a
 * cell with four dungeon neighbours ({@code WorldgenDungeonGT:289}), i.e. a crossing.
 *
 * <p>GT6 turns a crossing into a hall: a 12x12 chamber (local 2..13) with brick walls, a tiled floor and
 * a small-tiled ceiling, in which the sixteen columns at x/z 4, 7, 8 and 11 are chiselled at the floor
 * and carry a lit lamp in the ceiling. Four three-block entrance arms (13..15 and 0..2) connect the hall
 * to the corridors of the neighbouring cells. A crossing that connects in both axes also gets a
 * {@link GTDungeonChunkPillar}, which GT6 catches because the pillar is optional.</p>
 *
 * <p>Port differences:</p>
 * <ul>
 *   <li>The hall reaches local {@code y = 6}, and the ceiling lamps put their redstone brick at
 *       {@code y = 7} ({@code :40}, {@code :84-85}). That is GT6's own cell height: GT6's rooms are nine
 *       blocks tall (floor 0, interior 1..6, ceiling 7, tile skin 8, see {@code DungeonChunkBarracks:46},
 *       {@code DungeonChunkEntrance:94} and {@code DungeonChunkRoomFarmMobs:59}), so a crossing is as
 *       tall as a room while the port's {@link GTDungeonData} {@code FLOOR_Y}/{@code CEILING_Y}
 *       constants only describe the 0..4 shell of a plain corridor. GT6's coordinates are kept, so a
 *       crossing stays inside the dungeon's room height; only the four entrance arms (0..4) are
 *       corridor sized, exactly as in GT6.</li>
 *   <li>{@link GTDungeonData#lamp} is GT6's {@code DungeonData:163-167} lamp and keeps the
 *       {@code +1} redstone brick above the ceiling, so the lamps of the crossing behave like GT6's.</li>
 *   <li>GT6 prints the stack trace of a failed pillar ({@code :31}); the port swallows it like
 *       {@link GTDungeonChunkCorridor} does, because {@code WorldgenDungeonGT}'s dispatcher already
 *       reports a failing cell.</li>
 * </ul>
 */
public class GTDungeonChunkCorridor4 extends GTDungeonChunkPillar {

    @Override
    public boolean generate(GTDungeonData data) {
        boolean connectsX = data.connected(1, 0) || data.connected(-1, 0);
        boolean connectsZ = data.connected(0, 1) || data.connected(0, -1);
        if (connectsX && connectsZ) {
            try {
                super.generate(data);
            } catch (Throwable ignored) {
                // GT6 :31: the pillar is not important enough to fail the entire corridor.
            }
        }

        // GT6's hall (:34-56): bricks everywhere except the tiled floor, the small-tiled ceiling and
        // the chiselled pillars, which carry a lamp instead of ceiling.
        for (int x = 2; x <= 13; x++) {
            for (int z = 2; z <= 13; z++) {
                for (int y = 0; y <= 6; y++) {
                    if (x == 2 || x == 13 || z == 2 || z == 13 || y == 0 || y == 6) {
                        if ((x == 4 || x == 7 || x == 8 || x == 11) && (z == 4 || z == 7 || z == 8 || z == 11)) {
                            if (y == 0) {
                                data.chiseled(x, y, z);
                            } else if (y == 6) {
                                data.lamp(x, y, z, 1);
                            } else {
                                data.bricks(x, y, z);
                            }
                        } else {
                            if (y == 0) {
                                data.tiles(x, y, z);
                            } else if (y == 6) {
                                data.smalltiles(x, y, z);
                            } else {
                                data.bricks(x, y, z);
                            }
                        }
                    } else {
                        data.air(x, y, z);
                    }
                }
            }
        }

        // GT6's four entrance arms (:58-101), written in GT6's order: +X, -X, +Z, -Z.
        arm(data, 13, 15, 5, 10, true);
        arm(data, 0, 2, 5, 10, true);
        arm(data, 5, 10, 13, 15, false);
        arm(data, 5, 10, 0, 2, false);
        return true;
    }

    /** GT6's arm box (:58-101): tiles on the floor, small tiles on the ceiling, brick side walls. */
    private static void arm(GTDungeonData data, int x0, int x1, int z0, int z1, boolean alongX) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y <= 4; y++) {
                    if (y == 0) {
                        data.tiles(x, y, z);
                    } else if (y == 4) {
                        data.smalltiles(x, y, z);
                    } else if (alongX ? (z == z0 || z == z1) : (x == x0 || x == x1)) {
                        data.bricks(x, y, z);
                    } else {
                        data.air(x, y, z);
                    }
                }
            }
        }
    }
}
