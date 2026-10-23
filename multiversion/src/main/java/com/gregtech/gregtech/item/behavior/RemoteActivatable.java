package com.gregtech.gregtech.item.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Target contract for the remote activator, implemented by a block or block entity.
 *
 * <p>Return {@code true} to keep the coordinate bound after activation, or {@code false} to remove
 * it. A false return does not by itself mean activation failed: a dynamite charge deliberately
 * leaves the remote's list after its fuse is armed.
 *
 * <p>{@link BehaviorRemote} queries loaded coordinates, checking the block entity before the block.
 * The original advanced button also implements this contract; the port's local button press has
 * not yet been connected to that remote path.
 */
public interface RemoteActivatable {

    /** GT6 {@code ITileEntityRemoteActivateable#remoteActivate()}. @return GT6's boolean result */
    boolean remoteActivate(Level level, BlockPos pos);
}
