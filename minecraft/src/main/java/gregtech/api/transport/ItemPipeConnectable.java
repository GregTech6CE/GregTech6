/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.transport;

import net.minecraft.core.Direction;

/**
 * Opt-in: an item pipe connects to this block without a wrench, both when the pipe is placed
 * against it and when it changes next to an existing pipe.
 *
 * <p>Implement this on the block, or on its block entity. {@code side} is the face of this block
 * that faces the pipe. Returning {@code false} leaves the face disconnected; a wrench can still
 * open it manually, and the pipe then keeps it open across neighbour changes.
 */
public interface ItemPipeConnectable {
    boolean canConnectItemPipe(Direction side);
}
