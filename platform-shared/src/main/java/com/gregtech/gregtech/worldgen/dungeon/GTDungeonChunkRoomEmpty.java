package com.gregtech.gregtech.worldgen.dungeon;

/**
 * Port of GT6's {@code DungeonChunkRoomEmpty}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomEmpty.java}).
 *
 * <p>GT6's plain dungeon room, and the base class of every other room: an eight block tall hall whose shell
 * is a ring of stone bricks with tiles on the floor and small tiles on the ceiling, sixteen chiselled pillar
 * columns carrying lamps, and air everywhere inside. GT6 builds it in the local range {@code Y 0..8}:
 * the floor is {@code 0}, the interior {@code 1..6}, the ceiling {@code 7} and one extra tile layer at
 * {@code 8} sits on top of the plain ceiling parts. Only the four wall openings that lead to a neighbouring
 * dungeon cell use the corridor's range {@code Y 0..4}, so a room's doorway is three blocks high while the
 * hall itself is taller. Where the cell reaches open sky or sits under a liquid, the ceiling of the middle
 * box (local {@code 5..10}) turns into GT6's glowing glass.</p>
 *
 * <p>GT6 calls this class as the last room candidate when every other room declined, so
 * {@link #generate(GTDungeonData)} always returns {@code true}.</p>
 *
 * <p>Port differences, all inherited from {@link GTDungeonData}: the ceiling lamps use the port's
 * {@code lamp(x, y, z, +1)} (GT6's redstone brick above the lamp), the glowing glass is the port's single
 * uncoloured glowing glass (GT6 used its colour theme) and the rock variants are deterministic per block
 * (GT6 rolled a random brick meta). The wall openings ask {@link GTDungeonData#connected}, which also
 * answers for cells outside the layout, instead of GT6's direct {@code mRoomLayout} index.</p>
 */
public class GTDungeonChunkRoomEmpty extends GTDungeonChunkPillar {

    @Override
    public boolean generate(GTDungeonData data) {
        try {
            super.generate(data);
        } catch (Throwable ignored) {
            // GT6 :35 - the pillar is not important enough to fail the entire room.
        }

        // GT6 :37-60 - floor, ceiling and the border ring of the walls; everything inside is cleared.
        for (int tX = 0; tX < 16; tX++) {
            for (int tZ = 0; tZ < 16; tZ++) {
                for (int tY = 0; tY <= 7; tY++) {
                    if (tX == 0 || tX == 15 || tZ == 0 || tZ == 15 || tY == 0 || tY == 7) {
                        if ((tX == 3 || tX == 6 || tX == 9 || tX == 12)
                                && (tZ == 3 || tZ == 6 || tZ == 9 || tZ == 12)) {
                            if (tY == 0) {
                                data.chiseled(tX, tY, tZ);
                            } else if (tY == 7) {
                                data.lamp(tX, tY, tZ, +1);
                            } else {
                                data.bricks(tX, tY, tZ);
                            }
                        } else {
                            if (tY == 0) {
                                data.tiles(tX, tY, tZ);
                            } else if (tY == 7) {
                                data.smalltiles(tX, tY, tZ);
                                data.tiles(tX, tY + 1, tZ);
                            } else {
                                data.bricks(tX, tY, tZ);
                            }
                        }
                    } else {
                        data.air(tX, tY, tZ);
                    }
                }
            }
        }

        // GT6 :62-72 - the hall is open to the sky or under a liquid: its ceiling becomes glowing glass.
        if (data.liquid(8, 9, 8) || data.seesSky(8, 9, 8)) {
            for (int tX = 5; tX <= 10; tX++) {
                for (int tZ = 5; tZ <= 10; tZ++) {
                    if ((tX == 5 || tX == 10) && (tZ == 5 || tZ == 10)) {
                        data.chiseled(tX, 7, tZ);
                        data.chiseled(tX, 8, tZ);
                    } else {
                        data.glassglow(tX, 7, tZ);
                        data.glassglow(tX, 8, tZ);
                    }
                }
            }
        }

        // GT6 :74-105 - the opening towards the neighbour at X + 1.
        if (data.connected(1, 0)) {
            data.chiseled(15, 0, 5);
            data.smooth(15, 0, 6);
            data.smooth(15, 0, 7);
            data.smooth(15, 0, 8);
            data.smooth(15, 0, 9);
            data.chiseled(15, 0, 10);
            data.smooth(15, 1, 5);
            data.air(15, 1, 6);
            data.air(15, 1, 7);
            data.air(15, 1, 8);
            data.air(15, 1, 9);
            data.smooth(15, 1, 10);
            data.smooth(15, 2, 5);
            data.air(15, 2, 6);
            data.air(15, 2, 7);
            data.air(15, 2, 8);
            data.air(15, 2, 9);
            data.smooth(15, 2, 10);
            data.smooth(15, 3, 5);
            data.air(15, 3, 6);
            data.air(15, 3, 7);
            data.air(15, 3, 8);
            data.air(15, 3, 9);
            data.smooth(15, 3, 10);
            data.chiseled(15, 4, 5);
            data.smooth(15, 4, 6);
            data.smooth(15, 4, 7);
            data.smooth(15, 4, 8);
            data.smooth(15, 4, 9);
            data.chiseled(15, 4, 10);
        }
        // GT6 :106-137 - the opening towards the neighbour at X - 1.
        if (data.connected(-1, 0)) {
            data.chiseled(0, 0, 5);
            data.smooth(0, 0, 6);
            data.smooth(0, 0, 7);
            data.smooth(0, 0, 8);
            data.smooth(0, 0, 9);
            data.chiseled(0, 0, 10);
            data.smooth(0, 1, 5);
            data.air(0, 1, 6);
            data.air(0, 1, 7);
            data.air(0, 1, 8);
            data.air(0, 1, 9);
            data.smooth(0, 1, 10);
            data.smooth(0, 2, 5);
            data.air(0, 2, 6);
            data.air(0, 2, 7);
            data.air(0, 2, 8);
            data.air(0, 2, 9);
            data.smooth(0, 2, 10);
            data.smooth(0, 3, 5);
            data.air(0, 3, 6);
            data.air(0, 3, 7);
            data.air(0, 3, 8);
            data.air(0, 3, 9);
            data.smooth(0, 3, 10);
            data.chiseled(0, 4, 5);
            data.smooth(0, 4, 6);
            data.smooth(0, 4, 7);
            data.smooth(0, 4, 8);
            data.smooth(0, 4, 9);
            data.chiseled(0, 4, 10);
        }
        // GT6 :138-169 - the opening towards the neighbour at Z + 1.
        if (data.connected(0, 1)) {
            data.chiseled(5, 0, 15);
            data.smooth(6, 0, 15);
            data.smooth(7, 0, 15);
            data.smooth(8, 0, 15);
            data.smooth(9, 0, 15);
            data.chiseled(10, 0, 15);
            data.smooth(5, 1, 15);
            data.air(6, 1, 15);
            data.air(7, 1, 15);
            data.air(8, 1, 15);
            data.air(9, 1, 15);
            data.smooth(10, 1, 15);
            data.smooth(5, 2, 15);
            data.air(6, 2, 15);
            data.air(7, 2, 15);
            data.air(8, 2, 15);
            data.air(9, 2, 15);
            data.smooth(10, 2, 15);
            data.smooth(5, 3, 15);
            data.air(6, 3, 15);
            data.air(7, 3, 15);
            data.air(8, 3, 15);
            data.air(9, 3, 15);
            data.smooth(10, 3, 15);
            data.chiseled(5, 4, 15);
            data.smooth(6, 4, 15);
            data.smooth(7, 4, 15);
            data.smooth(8, 4, 15);
            data.smooth(9, 4, 15);
            data.chiseled(10, 4, 15);
        }
        // GT6 :170-201 - the opening towards the neighbour at Z - 1.
        if (data.connected(0, -1)) {
            data.chiseled(5, 0, 0);
            data.smooth(6, 0, 0);
            data.smooth(7, 0, 0);
            data.smooth(8, 0, 0);
            data.smooth(9, 0, 0);
            data.chiseled(10, 0, 0);
            data.smooth(5, 1, 0);
            data.air(6, 1, 0);
            data.air(7, 1, 0);
            data.air(8, 1, 0);
            data.air(9, 1, 0);
            data.smooth(10, 1, 0);
            data.smooth(5, 2, 0);
            data.air(6, 2, 0);
            data.air(7, 2, 0);
            data.air(8, 2, 0);
            data.air(9, 2, 0);
            data.smooth(10, 2, 0);
            data.smooth(5, 3, 0);
            data.air(6, 3, 0);
            data.air(7, 3, 0);
            data.air(8, 3, 0);
            data.air(9, 3, 0);
            data.smooth(10, 3, 0);
            data.chiseled(5, 4, 0);
            data.smooth(6, 4, 0);
            data.smooth(7, 4, 0);
            data.smooth(8, 4, 0);
            data.smooth(9, 4, 0);
            data.chiseled(10, 4, 0);
        }

        // GT6 :202 - the empty room never declines, it is the fallback of the room dispatcher.
        return true;
    }
}
