/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;

/**
 * The part state GregTech has synced to the client. An addon renderer reads this.
 * It does not subclass the block entity. A missing named field reads as zero, false or an empty string.
 * A key that was not declared throws.
 */
public interface PartClientView {
    String addonId();

    /** Registry path of the part block, without the addon namespace. */
    String name();

    int slots();

    ItemStack item(int slot);

    boolean occupied(int slot);

    int getInt(String key);

    long getLong(String key);

    boolean getBoolean(String key);

    String getString(String key);
}
