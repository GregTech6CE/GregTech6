/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.signal;

import gregtech.api.machine.BlockFace;

/**
 * Supplies fractional redstone to an adjacent signal wire.
 * Implement this on the block entity. The wire then reads {@link #gtSignal(BlockFace)} instead of vanilla redstone.
 *
 * <p>{@code side} is the absolute direction from the wire toward this block, the same direction
 * the wire would pass to {@code Level.getSignal(neighborPos, side)}. The host maps
 * {@link BlockFace} to {@code Direction}.
 * A behavior machine implements {@code MachineBehavior.gtSignal(context, Direction)} instead.
 * Machine-relative faces stay {@code MachineFace}.
 */
public interface GtSignalSource {
    /**
     * Strength emitted toward the signal wire, in {@link GtSignalCarrier#UNIT} units.
     * The wire clamps values outside {@code [0, 15 * UNIT]}.
     */
    long gtSignal(BlockFace side);
}
