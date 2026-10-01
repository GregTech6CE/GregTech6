package com.gregtech.gregtech.api.energy;

/** Six-way engine faces using native DOWN, UP, NORTH, SOUTH, WEST, EAST indices.
 * Keeps the existing capability-side convention and derives its exact inverse.
 */
public final class EngineFaceRotation {
    private EngineFaceRotation() {}

    // Columns are BOTTOM, TOP, RIGHT, FRONT, LEFT, BACK (MachineFaceMasks indices).
    private static final int[][] WORLD_SIDES = {
            {3, 2, 5, 0, 4, 1}, // facing down
            {3, 2, 5, 1, 4, 0}, // facing up
            {0, 1, 4, 2, 5, 3}, // facing north
            {0, 1, 5, 3, 4, 2}, // facing south
            {0, 1, 3, 4, 2, 5}, // facing west
            {0, 1, 2, 5, 3, 4}  // facing east
    };

    public static int toWorld(int facing, int relativeSide) {
        check(facing); check(relativeSide);
        return WORLD_SIDES[facing][relativeSide];
    }

    public static int toRelative(int facing, int worldSide) {
        check(facing); check(worldSide);
        for (int relative = 0; relative < 6; relative++)
            if (WORLD_SIDES[facing][relative] == worldSide) return relative;
        throw new IllegalStateException("Incomplete engine face rotation");
    }

    private static void check(int side) {
        if (side < 0 || side >= 6) throw new IllegalArgumentException("Invalid engine face: " + side);
    }
}
