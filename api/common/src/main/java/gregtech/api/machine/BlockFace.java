/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

/**
 * An absolute block face, independent of any machine's facing.
 *
 * <p>The declaration order matches Minecraft's {@code Direction} ordinal:
 * {@code DOWN} 0, {@code UP} 1, {@code NORTH} 2, {@code SOUTH} 3, {@code WEST} 4,
 * {@code EAST} 5. A host adapter converts with {@code Direction.values()[face.index()]};
 * the core API uses this enum instead of a bare {@code Direction.ordinal()} integer.
 * Machine-relative faces stay {@link MachineFace}.</p>
 */
public enum BlockFace {
    DOWN, UP, NORTH, SOUTH, WEST, EAST;

    private static final BlockFace[] ALL = values();

    /** Index matching {@code Direction.ordinal()}. */
    public int index() {
        return ordinal();
    }

    /** The face at {@code Direction.ordinal()} {@code index}; rejects values outside 0..5. */
    public static BlockFace of(int index) {
        if (index < 0 || index >= ALL.length)
            throw new IllegalArgumentException("Block face must be 0..5: " + index);
        return ALL[index];
    }
}
