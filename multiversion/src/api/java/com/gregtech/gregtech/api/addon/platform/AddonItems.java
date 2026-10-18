package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** GregTech material forms as item stacks. An unknown prefix or material is an empty stack. */
public final class AddonItems {
    private AddonItems() {}

    public static ItemStack stack(String prefix, String material, int count) {
        return AddonPlatform.services().stack(prefix, material, count);
    }

    public static ItemStack stack(String prefix, String material) {
        return stack(prefix, material, 1);
    }
}
