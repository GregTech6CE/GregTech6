/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;

/**
 * Inventory owned by a machine or a structure part, used during its server callback.
 * Reads are detached snapshots; writes copy their arguments and notify the host.
 * This is internal processing access, not a sided automation capability: it bypasses
 * insertion filters and slot roles. Do not retain this view beyond the callback.
 */
public interface InventoryAccess {
    int slots();

    /** A detached stack. Changing it never changes this inventory. Invalid slots throw. */
    ItemStack snapshot(int slot);

    /**
     * Replaces a slot through the host's normal storage notification path.
     * Null means empty. Replacement limits remain host-specific (parts clamp to their declared
     * limit; the legacy machine setter does not). This is not a capacity-checked insertion.
     */
    void set(int slot, ItemStack stack);

    /**
     * Takes up to {@code amount} items from a slot, regardless of its input/output role.
     * Returns a detached stack. Zero takes nothing; negative amounts throw.
     * Simulation returns what would be taken without writing or notifying the host.
     * This single-slot operation is not a transaction across multiple resources.
     */
    default ItemStack extract(int slot, int amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("negative item amount");
        ItemStack stored = snapshot(slot);
        if (amount == 0 || stored.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stored.split(Math.min(amount, stored.getCount()));
        if (!simulate) set(slot, stored);
        return taken;
    }
}
