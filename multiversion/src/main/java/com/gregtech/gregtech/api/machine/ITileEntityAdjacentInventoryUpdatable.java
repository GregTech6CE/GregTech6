package com.gregtech.gregtech.api.machine;

import net.minecraft.core.Direction;

/**
 * GT6 {@code ITileEntityAdjacentInventoryUpdatable} — notified when a neighboring inventory changes.
 * Used by pipes and similar blocks to re-check connectivity after hopper activity.
 */
public interface ITileEntityAdjacentInventoryUpdatable {
    /**
     * Called by adjacent inventory blocks when their content has changed.
     *
     * @param side      the side of this tile that is adjacent to the notifying inventory
     * @param sourcePos the block position of the notifying inventory (optional context)
     */
    void adjacentInventoryUpdated(Direction side, net.minecraft.core.BlockPos sourcePos);
}
