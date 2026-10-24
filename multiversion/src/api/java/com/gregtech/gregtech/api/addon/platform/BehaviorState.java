/* SPDX-License-Identifier: LGPL-3.0-or-later */

package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;

/**
 * Per-block-entity values GregTech writes to NBT. Keys match {@code [a-z0-9_.]+}.
 * A missing key reads as zero, false, an empty string or an empty item.
 * GregTech also drops stored items when the block is broken.
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
