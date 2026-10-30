/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;

/**
 * Inventory owned by a behavior machine. Valid only during the same behavior callback as its
 * context. Reads are detached snapshots; writes copy their arguments and notify the host.
 */
public interface MachineInventoryAccess extends InventoryAccess {
    /** Puts {@code stack} into output slots, then slots that allow either role. Returns what did not fit. */
    ItemStack insertOutput(ItemStack stack);
}
