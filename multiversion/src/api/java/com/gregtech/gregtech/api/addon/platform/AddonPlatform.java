package com.gregtech.gregtech.api.addon.platform;

import com.gregtech.gregtech.api.addon.GregTechMaterials;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Platform entry installed by GregTech before material initialization.
 * Fluid stacks and recipe rows that name a loader fluid type live on {@code AddonLoader} instead.
 */
public final class AddonPlatform {
    private static Services services;

    private AddonPlatform() {}

    /** GT bootstrap only. Addons must not install or replace the host implementation. */
    public static synchronized void bind(Services implementation) {
        if (implementation == null) throw new IllegalArgumentException("addon platform services");
        if (services != null) throw new IllegalStateException("GregTech addon platform API is already installed");
        services = implementation;
    }

    public static synchronized Services services() {
        if (services == null) throw new IllegalStateException("GregTech addon platform API is not installed");
        return services;
    }

    /** GT implementation contract; addon consumers use the items, tools and energy facades. */
    public interface Services {
        ItemStack stack(String prefix, String material, int count);

        /** Integrated circuit {@code configuration}, or an empty stack when that item is not registered yet. */
        ItemStack selectorTag(int configuration);

        boolean matchesTool(ItemStack stack, AddonTool tool);

        void damageTool(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot);

        long inject(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute);

        long extract(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute);

        /** Prefix and material units already stored for this stack, or empty when the stack is unknown. */
        GregTechMaterials.StackComposition composition(ItemStack stack);
    }
}
