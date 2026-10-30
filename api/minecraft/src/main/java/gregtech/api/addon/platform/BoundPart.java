/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One addon part bound by a formed behavior machine. An energy part and a functional part
 * share this list. A casing is not included. Do not keep the instance after the callback:
 * a broken structure drops it, and a removed part answers as if it were unbound.
 */
public interface BoundPart {
    BlockPos pos();

    /** Registry path of the part block, without the addon namespace. */
    String name();

    /** Non-null when this cell emits or accepts energy. */
    @Nullable EnergyPart energy();

    /**
     * The addon instance for a functional part. Null for an energy-only part,
     * or when this cell is no longer bound. Cast it to call the addon's own methods.
     */
    @Nullable PartBehavior behavior();

    /** The context those methods should use. Null when {@link #behavior()} is null. */
    @Nullable PartContext context();

    int slots();

    ItemStack item(int slot);
}
