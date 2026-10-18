package com.gregtech.gregtech.worldgen.dungeon;

/**
 * Port of GT6's {@code DungeonChunkPillar} ({@code gregapi/worldgen/dungeon/DungeonChunkPillar.java}).
 *
 * <p>Digs a 4×4 shaft down from the cell towards bedrock and, where it breaks into open ground, widens it
 * into a 6×6 rock casing. GT6 calls it from the corridors (a corridor that turns gets a pillar) and from
 * every room that extends it.</p>
 *
 * <p>The port keeps GT6's shape and its ground tests; the local Y loop runs down to
 * {@code level.getMinBuildHeight() + 2} instead of GT6's fixed {@code y >= 2}, and "is this a falling
 * block / sand / diggable" is expressed with the port's block tags.</p>
 */
public class GTDungeonChunkPillar implements GTDungeonChunk {

    @Override
    public boolean generate(GTDungeonData data) {
        boolean open = true;
        // GT6: the 4x4 core must not already be opaque above the cell floor.
        for (int x = 6; x <= 9 && open; x++) {
            for (int z = 6; z <= 9 && open; z++) {
                if (data.opaque(x, -1, z)) open = false;
            }
        }

        if (open) {
            for (int x = 5; x <= 10; x++) {
                for (int z = 5; z <= 10; z++) {
                    data.smooth(x, -1, z);
                    data.bricks(x, -2, z);
                }
            }
        }

        int floor = data.level.getMinBuildHeight() + 2;
        for (int y = -3; data.y + y >= floor && open; y--) {
            open = false;
            for (int x = 6; x <= 9 && !open; x++) {
                for (int z = 6; z <= 9 && !open; z++) {
                    if (!data.opaque(x, y, z) || data.softGround(x, y, z)) open = true;
                }
            }
            if (open) {
                for (int x = 6; x <= 9; x++) {
                    for (int z = 6; z <= 9; z++) data.bricks(x, y, z);
                }
            } else {
                for (int x = 5; x <= 10; x++) {
                    for (int z = 5; z <= 10; z++) {
                        data.smooth(x, y + 1, z);
                        data.bricks(x, y, z);
                        data.bricks(x, y - 1, z);
                        // GT6 only lays the last ring when it is above bedrock (hardness >= 0).
                        if (y > 2 || data.aboveBedrock(x, z)) data.smooth(x, y - 2, z);
                    }
                }
            }
        }
        return true;
    }
}
