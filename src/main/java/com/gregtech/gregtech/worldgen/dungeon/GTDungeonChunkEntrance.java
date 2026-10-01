package com.gregtech.gregtech.worldgen.dungeon;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of GT6's {@code DungeonChunkEntrance}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkEntrance.java}).
 *
 * <p>GT6's shaft to the surface, one of the two "important" cells of every dungeon (layout marker
 * {@code -2}). It first builds a {@link GTDungeonChunkPillar} and then a room like the empty room's, whose
 * sixteen pillar columns stand at 2/6/9/13 instead of 3/6/9/12 and whose middle 2x2 of the ceiling is
 * thickened - that 4x4 core, six blocks above the floor, is the foot of the shaft. From there GT6 scans
 * upwards in ten by ten layers until one of them is completely replaceable (that is the open sky above the
 * terrain, or the world's height cap), rounds the resulting height up to the next {@code 5k+1}, and builds
 * a 10x10 shaft of bricks around a 4x4 ring of smooth stone that is banded with the dungeon's colour every
 * fourth block. A diagonal staircase of smooth stone spirals up along the inside of that shaft, tiles cap
 * the wall at the top, and a 12x12 structure with a coloured coping sits around the opening. Finally the
 * walls towards a neighbouring cell are opened with the dungeon's colour, exactly like the empty room's.</p>
 *
 * <p>Port differences:</p>
 * <ul>
 *   <li>The staircase and the top cap call GT6's slab overloads
 *       ({@code smooth(x, y, z, mPrimary.mSlabs[i], mSecondary.mSlabs[i])}); the port uses
 *       {@link GTDungeonData#smoothSlab} / {@link GTDungeonData#tilesSlab} with GT6's own two slab
 *       orientations - {@code mSlabs[0]} is its bottom half ({@code SIDE_Y_NEG}) and {@code mSlabs[1]}
 *       its top half ({@code SIDE_Y_POS}) - so the stair treads are half blocks again.</li>
 *   <li>The colour bands use the port's concrete colour family; GT6's 1.7.10 world was 256 blocks tall and
 *       its shaft could rely on that, so every write above the room is bounded with
 *       {@code data.y + localY < data.level.getMaxBuildHeight()} ({@code top} below).</li>
 *   <li>GT6's {@code WD.easyRep}/{@code Block.isWood} terrain test becomes the port's block tags, and
 *       {@code mWorld.getHeight()} becomes {@code data.level.getHeight()}.</li>
 *   <li>The neighbouring cell is asked through {@link GTDungeonData#connected}, which also answers outside
 *       the layout, instead of GT6's direct {@code mRoomLayout} index.</li>
 * </ul>
 */
public class GTDungeonChunkEntrance extends GTDungeonChunkPillar {

    @Override
    public boolean generate(GTDungeonData data) {
        try {
            super.generate(data);
        } catch (Throwable ignored) {
            // GT6 :33 - the pillar is not important enough to fail the entire entrance.
        }

        // GT6 :35-62 - the room at the foot of the shaft.
        for (int tX = 0; tX < 16; tX++) {
            for (int tZ = 0; tZ < 16; tZ++) {
                for (int tY = 0; tY <= 7; tY++) {
                    if (tX == 0 || tX == 15 || tZ == 0 || tZ == 15 || tY == 0 || tY == 7) {
                        if ((tX == 2 || tX == 6 || tX == 9 || tX == 13)
                                && (tZ == 2 || tZ == 6 || tZ == 9 || tZ == 13)) {
                            if (tY == 0) {
                                data.chiseled(tX, tY, tZ);
                            } else if (tY == 7) {
                                if (!((tX == 6 || tX == 9) && (tZ == 6 || tZ == 9))) {
                                    data.lamp(tX, tY, tZ, +1);
                                } else {
                                    data.bricks(tX, tY, tZ);
                                    data.bricks(tX, tY + 1, tZ);
                                }
                            } else {
                                data.bricks(tX, tY, tZ);
                            }
                        } else {
                            if (tY == 0) {
                                data.tiles(tX, tY, tZ);
                            } else if (tY == 7) {
                                data.smalltiles(tX, tY, tZ);
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

        // GT6 :64-65 - how far up the shaft has to dig: from ten blocks above the cell to the first ten by
        // ten layer that is entirely replaceable (the open sky), or to the world's height cap.
        int tHeight = 10 + data.y;
        int eY = data.level.getHeight() - 32;
        for (int tAirAmount = 0; tHeight < eY && tAirAmount < 100; tHeight++) {
            tAirAmount = 0;
            for (int tX = 3; tX <= 12; tX++) {
                for (int tZ = 3; tZ <= 12; tZ++) {
                    if (easyReplace(data.get(tX, tHeight - data.y, tZ))) tAirAmount++;
                }
            }
        }
        tHeight -= data.y;

        // GT6 :74 - round the height up to the next 5k+1, so the shaft's bands line up.
        if ((tHeight - 1) % 5 != 0) tHeight += 5 - ((tHeight - 1) % 5);

        // The port's ceiling for the shaft: every positive local Y below must stay inside the dimension.
        int top = data.level.getMaxBuildHeight() - data.y;
        if (tHeight > top - 3) tHeight = top - 3;

        // GT6 :76-92 - the shaft: a 4x4 core ring inside a 10x10 wall, tiled where the wall ends.
        for (int tY = 7; tY <= tHeight && tY < top; tY++) {
            for (int tX = 3; tX <= 12; tX++) {
                for (int tZ = 3; tZ <= 12; tZ++) {
                    if (tX >= 6 && tX <= 9 && tZ >= 6 && tZ <= 9 && (tX == 6 || tZ == 6 || tX == 9 || tZ == 9)) {
                        if (tY % 4 == 0) {
                            data.colored(tX, tY, tZ);
                        } else {
                            data.smooth(tX, tY, tZ);
                        }
                    } else if (tX == 3 || tZ == 3 || tX == 12 || tZ == 12) {
                        if (tY == tHeight - 1) {
                            data.tiles(tX, tY, tZ);
                        } else {
                            data.bricks(tX, tY, tZ);
                        }
                    } else {
                        data.air(tX, tY, tZ);
                    }
                }
            }
        }

        // GT6 :94-102 - the core's lining inside the room, banded like the shaft.
        for (int tY = 1; tY <= 6; tY++) {
            for (int tX = 6; tX <= 9; tX++) {
                for (int tZ = 6; tZ <= 9; tZ++) {
                    if (tX == 6 || tZ == 6 || tX == 9 || tZ == 9) {
                        if (tY % 4 == 0) {
                            data.colored(tX, tY, tZ);
                        } else {
                            data.smooth(tX, tY, tZ);
                        }
                    }
                }
            }
        }

        // GT6 :104-111 - the doorway through the core, towards the neighbours along Z.
        data.air(7, 1, 6);
        data.air(7, 1, 9);
        data.air(7, 2, 6);
        data.air(7, 2, 9);
        data.air(8, 1, 6);
        data.air(8, 1, 9);
        data.air(8, 2, 6);
        data.air(8, 2, 9);

        // GT6 :113-175 - the spiral staircase in the four corners of the shaft, five blocks per turn.
        // Every tread is a smooth rock slab; GT6 alternates its two slab orientations (mSlabs[0] is the
        // bottom half, SIDE_Y_NEG, mSlabs[1] the top half, SIDE_Y_POS) so the flight steps upwards.
        int tOffsetY = -5;
        while (tOffsetY + 10 < tHeight) {
            tOffsetY += 5;

            data.smoothSlab(10, tOffsetY + 1, 6, Direction.DOWN);
            data.smoothSlab(11, tOffsetY + 1, 6, Direction.DOWN);
            data.smoothSlab(10, tOffsetY + 1, 7, Direction.UP);
            data.smoothSlab(11, tOffsetY + 1, 7, Direction.UP);
            data.smoothSlab(10, tOffsetY + 2, 8, Direction.DOWN);
            data.smoothSlab(11, tOffsetY + 2, 8, Direction.DOWN);
            data.smoothSlab(10, tOffsetY + 2, 9, Direction.UP);
            data.smoothSlab(11, tOffsetY + 2, 9, Direction.UP);

            data.smoothSlab(10, tOffsetY + 3, 10, Direction.DOWN);
            data.smoothSlab(11, tOffsetY + 3, 10, Direction.DOWN);
            data.smoothSlab(10, tOffsetY + 3, 11, Direction.DOWN);
            data.smoothSlab(11, tOffsetY + 3, 11, Direction.DOWN);

            data.smoothSlab(9, tOffsetY + 3, 10, Direction.UP);
            data.smoothSlab(9, tOffsetY + 3, 11, Direction.UP);
            data.smoothSlab(8, tOffsetY + 4, 10, Direction.DOWN);
            data.smoothSlab(8, tOffsetY + 4, 11, Direction.DOWN);
            data.smoothSlab(7, tOffsetY + 4, 10, Direction.UP);
            data.smoothSlab(7, tOffsetY + 4, 11, Direction.UP);
            data.smoothSlab(6, tOffsetY + 5, 10, Direction.DOWN);
            data.smoothSlab(6, tOffsetY + 5, 11, Direction.DOWN);

            data.smoothSlab(5, tOffsetY + 5, 10, Direction.UP);
            data.smoothSlab(5, tOffsetY + 5, 11, Direction.UP);
            data.smoothSlab(4, tOffsetY + 5, 10, Direction.UP);
            data.smoothSlab(4, tOffsetY + 5, 11, Direction.UP);

            data.smoothSlab(4, tOffsetY + 1, 9, Direction.DOWN);
            data.smoothSlab(5, tOffsetY + 1, 9, Direction.DOWN);
            data.smoothSlab(4, tOffsetY + 1, 8, Direction.UP);
            data.smoothSlab(5, tOffsetY + 1, 8, Direction.UP);
            data.smoothSlab(4, tOffsetY + 2, 7, Direction.DOWN);
            data.smoothSlab(5, tOffsetY + 2, 7, Direction.DOWN);
            data.smoothSlab(4, tOffsetY + 2, 6, Direction.UP);
            data.smoothSlab(5, tOffsetY + 2, 6, Direction.UP);

            data.smoothSlab(4, tOffsetY + 3, 5, Direction.DOWN);
            data.smoothSlab(5, tOffsetY + 3, 5, Direction.DOWN);
            data.smoothSlab(4, tOffsetY + 3, 4, Direction.DOWN);
            data.smoothSlab(5, tOffsetY + 3, 4, Direction.DOWN);

            data.smoothSlab(6, tOffsetY + 3, 4, Direction.UP);
            data.smoothSlab(6, tOffsetY + 3, 5, Direction.UP);
            data.smoothSlab(7, tOffsetY + 4, 4, Direction.DOWN);
            data.smoothSlab(7, tOffsetY + 4, 5, Direction.DOWN);
            data.smoothSlab(8, tOffsetY + 4, 4, Direction.UP);
            data.smoothSlab(8, tOffsetY + 4, 5, Direction.UP);
            data.smoothSlab(9, tOffsetY + 5, 4, Direction.DOWN);
            data.smoothSlab(9, tOffsetY + 5, 5, Direction.DOWN);

            data.smoothSlab(10, tOffsetY + 5, 4, Direction.UP);
            data.smoothSlab(10, tOffsetY + 5, 5, Direction.UP);
            data.smoothSlab(11, tOffsetY + 5, 4, Direction.UP);
            data.smoothSlab(11, tOffsetY + 5, 5, Direction.UP);
        }

        // GT6 :177-193 - the two tiled landings at the top of the staircase, GT6's tile slabs in its
        // mSlabs[1] orientation (the top half).
        data.tilesSlab(4, tOffsetY + 5, 9, Direction.UP);
        data.tilesSlab(5, tOffsetY + 5, 9, Direction.UP);
        data.tilesSlab(4, tOffsetY + 5, 8, Direction.UP);
        data.tilesSlab(5, tOffsetY + 5, 8, Direction.UP);
        data.tilesSlab(4, tOffsetY + 5, 7, Direction.UP);
        data.tilesSlab(5, tOffsetY + 5, 7, Direction.UP);
        data.tilesSlab(4, tOffsetY + 5, 6, Direction.UP);
        data.tilesSlab(5, tOffsetY + 5, 6, Direction.UP);

        data.tilesSlab(10, tOffsetY + 5, 9, Direction.UP);
        data.tilesSlab(11, tOffsetY + 5, 9, Direction.UP);
        data.tilesSlab(10, tOffsetY + 5, 8, Direction.UP);
        data.tilesSlab(11, tOffsetY + 5, 8, Direction.UP);
        data.tilesSlab(10, tOffsetY + 5, 7, Direction.UP);
        data.tilesSlab(11, tOffsetY + 5, 7, Direction.UP);
        data.tilesSlab(10, tOffsetY + 5, 6, Direction.UP);
        data.tilesSlab(11, tOffsetY + 5, 6, Direction.UP);

        // GT6 :195-205 - the structure around the opening: a 12x12 box, coloured coping one block above the
        // shaft's top, and the opening itself cleared down to the shaft.
        for (int tY = Math.max(8, tHeight - 2); tY <= tHeight + 2 && tY < top; tY++) {
            for (int tX = 2; tX <= 13; tX++) {
                for (int tZ = 2; tZ <= 13; tZ++) {
                    if (tX == 2 || tZ == 2 || tX == 13 || tZ == 13) {
                        if (tY == tHeight + 1) {
                            data.colored(tX, tY, tZ);
                        } else {
                            data.bricks(tX, tY, tZ);
                        }
                    } else {
                        if (tY >= tHeight) data.air(tX, tY, tZ);
                    }
                }
            }
        }

        // GT6 :207-238 - the opening towards the neighbour at X + 1.
        if (data.connected(1, 0)) {
            data.colored(15, 0, 5);
            data.colored(15, 0, 6);
            data.colored(15, 0, 7);
            data.colored(15, 0, 8);
            data.colored(15, 0, 9);
            data.colored(15, 0, 10);
            data.colored(15, 1, 5);
            data.air(15, 1, 6);
            data.air(15, 1, 7);
            data.air(15, 1, 8);
            data.air(15, 1, 9);
            data.colored(15, 1, 10);
            data.colored(15, 2, 5);
            data.air(15, 2, 6);
            data.air(15, 2, 7);
            data.air(15, 2, 8);
            data.air(15, 2, 9);
            data.colored(15, 2, 10);
            data.colored(15, 3, 5);
            data.air(15, 3, 6);
            data.air(15, 3, 7);
            data.air(15, 3, 8);
            data.air(15, 3, 9);
            data.colored(15, 3, 10);
            data.colored(15, 4, 5);
            data.colored(15, 4, 6);
            data.colored(15, 4, 7);
            data.colored(15, 4, 8);
            data.colored(15, 4, 9);
            data.colored(15, 4, 10);
        }
        // GT6 :239-270 - the opening towards the neighbour at X - 1.
        if (data.connected(-1, 0)) {
            data.colored(0, 0, 5);
            data.colored(0, 0, 6);
            data.colored(0, 0, 7);
            data.colored(0, 0, 8);
            data.colored(0, 0, 9);
            data.colored(0, 0, 10);
            data.colored(0, 1, 5);
            data.air(0, 1, 6);
            data.air(0, 1, 7);
            data.air(0, 1, 8);
            data.air(0, 1, 9);
            data.colored(0, 1, 10);
            data.colored(0, 2, 5);
            data.air(0, 2, 6);
            data.air(0, 2, 7);
            data.air(0, 2, 8);
            data.air(0, 2, 9);
            data.colored(0, 2, 10);
            data.colored(0, 3, 5);
            data.air(0, 3, 6);
            data.air(0, 3, 7);
            data.air(0, 3, 8);
            data.air(0, 3, 9);
            data.colored(0, 3, 10);
            data.colored(0, 4, 5);
            data.colored(0, 4, 6);
            data.colored(0, 4, 7);
            data.colored(0, 4, 8);
            data.colored(0, 4, 9);
            data.colored(0, 4, 10);
        }
        // GT6 :271-302 - the opening towards the neighbour at Z + 1.
        if (data.connected(0, 1)) {
            data.colored(5, 0, 15);
            data.colored(6, 0, 15);
            data.colored(7, 0, 15);
            data.colored(8, 0, 15);
            data.colored(9, 0, 15);
            data.colored(10, 0, 15);
            data.colored(5, 1, 15);
            data.air(6, 1, 15);
            data.air(7, 1, 15);
            data.air(8, 1, 15);
            data.air(9, 1, 15);
            data.colored(10, 1, 15);
            data.colored(5, 2, 15);
            data.air(6, 2, 15);
            data.air(7, 2, 15);
            data.air(8, 2, 15);
            data.air(9, 2, 15);
            data.colored(10, 2, 15);
            data.colored(5, 3, 15);
            data.air(6, 3, 15);
            data.air(7, 3, 15);
            data.air(8, 3, 15);
            data.air(9, 3, 15);
            data.colored(10, 3, 15);
            data.colored(5, 4, 15);
            data.colored(6, 4, 15);
            data.colored(7, 4, 15);
            data.colored(8, 4, 15);
            data.colored(9, 4, 15);
            data.colored(10, 4, 15);
        }
        // GT6 :303-334 - the opening towards the neighbour at Z - 1.
        if (data.connected(0, -1)) {
            data.colored(5, 0, 0);
            data.colored(6, 0, 0);
            data.colored(7, 0, 0);
            data.colored(8, 0, 0);
            data.colored(9, 0, 0);
            data.colored(10, 0, 0);
            data.colored(5, 1, 0);
            data.air(6, 1, 0);
            data.air(7, 1, 0);
            data.air(8, 1, 0);
            data.air(9, 1, 0);
            data.colored(10, 1, 0);
            data.colored(5, 2, 0);
            data.air(6, 2, 0);
            data.air(7, 2, 0);
            data.air(8, 2, 0);
            data.air(9, 2, 0);
            data.colored(10, 2, 0);
            data.colored(5, 3, 0);
            data.air(6, 3, 0);
            data.air(7, 3, 0);
            data.air(8, 3, 0);
            data.air(9, 3, 0);
            data.colored(10, 3, 0);
            data.colored(5, 4, 0);
            data.colored(6, 4, 0);
            data.colored(7, 4, 0);
            data.colored(8, 4, 0);
            data.colored(9, 4, 0);
            data.colored(10, 4, 0);
        }

        // GT6 :336 - the entrance always accepts its cell.
        return true;
    }

    /**
     * GT6's {@code WD.easyRep} ({@code WD:687-688}: air, a bush, snow, fire or leaves) together with the
     * {@code Block.isWood} test of :69. The fully qualified vanilla {@code BushBlock} keeps this apart from
     * the port's own bush block.
     */
    private static boolean easyReplace(BlockState state) {
        return state.isAir()
                || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS);
    }
}
