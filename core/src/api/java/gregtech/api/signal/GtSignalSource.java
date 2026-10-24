/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.signal;

/**
 * Supplies fractional redstone to an adjacent signal wire.
 * Implement this on the block entity. The wire then reads {@link #gtSignal(int)} instead of vanilla redstone.
 *
 * <p>{@code side} follows {@code Direction.ordinal()}: 0 DOWN, 1 UP, 2 NORTH, 3 SOUTH, 4 WEST, 5 EAST.
 * The wire passes the direction from itself toward this block, the same direction it would pass to
 * {@code Level.getSignal(neighborPos, side)}. This core type cannot name {@code Direction}.
 * A behavior machine implements {@code MachineBehavior.gtSignal(context, Direction)} instead.
 * Machine-relative faces stay {@code MachineFace}.
 */
public interface GtSignalSource {
    /**
     * Strength emitted toward the signal wire, in {@link GtSignalCarrier#UNIT} units.
     * The wire clamps values outside {@code [0, 15 * UNIT]}.
     */
    long gtSignal(int side);
}
