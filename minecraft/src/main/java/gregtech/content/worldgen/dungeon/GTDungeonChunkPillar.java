package gregtech.content.worldgen.dungeon;

import gregtech.content.block.OreBlock;
import gregtech.content.block.stone.StoneVariant;

/**
 * Port of GT6's {@code DungeonChunkPillar} ({@code gregapi/worldgen/dungeon/DungeonChunkPillar.java}).
 *
 * <p>Digs a 4×4 shaft down from the cell towards bedrock and, where it breaks into open ground, widens it
 * into a 6×6 rock casing. GT6 calls it from the corridors (a corridor that turns gets a pillar) and from
 * every room that extends it.</p>
 *
 * <p>The port keeps GT6's shape and its ground tests; the local Y loop runs down to
 * {@code level.getMinBuildHeight() + 2} instead of GT6's fixed {@code y >= 2}.
 * The foundation stays above the basal floor and preserves bedrock, embedded ores and block entities.</p>
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
                    foundation(data, x, -1, z, StoneVariant.SMOOTH);
                    foundation(data, x, -2, z, StoneVariant.BRICKS);
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
                    for (int z = 6; z <= 9; z++) foundation(data, x, y, z, StoneVariant.BRICKS);
                }
            } else {
                for (int x = 5; x <= 10; x++) {
                    for (int z = 5; z <= 10; z++) {
                        foundation(data, x, y + 1, z, StoneVariant.SMOOTH);
                        foundation(data, x, y, z, StoneVariant.BRICKS);
                        foundation(data, x, y - 1, z, StoneVariant.BRICKS);
                        foundation(data, x, y - 2, z, StoneVariant.SMOOTH);
                    }
                }
            }
        }
        return true;
    }

    private static void foundation(GTDungeonData data, int x, int y, int z, StoneVariant variant) {
        if (data.y + y <= data.level.getMinBuildHeight()) return;
        var state = data.get(x, y, z);
        if (!OreBlock.isBedrockFloor(state) && !state.hasBlockEntity()) data.stone(x, y, z, variant);
    }
}
