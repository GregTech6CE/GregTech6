package com.gregtech.gregtech.api.addon.platform;

import com.gregtech.gregtech.api.addon.SelectorTags;

import net.minecraft.world.item.ItemStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** GregTech material forms and programmed circuits as item stacks. An unknown prefix or material is an empty stack. */
public final class AddonItems {
    private AddonItems() {}

    public static ItemStack stack(String prefix, String material, int count) {
        return AddonPlatform.services().stack(prefix, material, count);
    }

    public static ItemStack stack(String prefix, String material) {
        return stack(prefix, material, 1);
    }

    /** Programmed circuit {@code n} ({@code integrated_circuit_n}), count 1. Outside 0..24 fails. */
    public static ItemStack selectorTag(int n) {
        SelectorTags.require(n);
        ItemStack stack = AddonPlatform.services().selectorTag(n);
        if (stack == null || stack.isEmpty())
            throw new IllegalStateException("Selector tag " + n + " is not registered");
        return stack;
    }
}
