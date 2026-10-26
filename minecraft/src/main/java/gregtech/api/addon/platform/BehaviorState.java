/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.addon.machine.*;

import net.minecraft.world.item.ItemStack;

/**
 * Per-block-entity values GregTech writes to NBT. A machine accepts any key matching
 * {@code [a-z0-9_.]{1,64}}; a functional part only accepts a key it declared with
 * {@code LayoutPart.Builder#field}. Int, long, boolean and string values live in separate tables,
 * so a key written with {@link #setInt} reads as zero through {@link #getLong}. Stored strings are
 * non-null and at most 256 characters. A missing key reads as zero, false, an empty string or an
 * empty item. GregTech also drops stored items when the block is broken.
 */
public interface BehaviorState {
    int getInt(String key);

    void setInt(String key, int value);

    long getLong(String key);

    void setLong(String key, long value);

    boolean getBoolean(String key);

    void setBoolean(String key, boolean value);

    String getString(String key);

    void setString(String key, String value);

    /** A detached snapshot. Change it locally, then call {@link #setItem} to persist the change. */
    ItemStack item(String key);

    /** Stores a copy and marks the host changed; null/empty removes the stored item. */
    void setItem(String key, ItemStack stack);
}
