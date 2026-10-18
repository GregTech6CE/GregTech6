package com.gregtech.gregtech.internal.addon;

import com.gregtech.gregtech.api.addon.platform.AddonPlatform;
import com.gregtech.gregtech.api.addon.platform.AddonTool;
import com.gregtech.gregtech.api.addon.platform.EnergyBlock;
import com.gregtech.gregtech.api.energy.EnergyBlockDefaults;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the shared platform addon entry. */
public final class AddonPlatformServices {
    private AddonPlatformServices() {}

    public static void bind() {
        AddonPlatform.bind(new Services());
    }

    static GregTechTags.Tag tag(String energy) {
        if (energy == null) throw new IllegalArgumentException("Missing energy type");
        return GregTechTags.Energy.ALL.stream()
                .filter(candidate -> candidate.getShortName().equalsIgnoreCase(energy))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown energy type: " + energy));
    }

    private static final class Services implements AddonPlatform.Services {
        @Override public ItemStack stack(String prefix, String material, int count) {
            if (prefix == null || material == null || count <= 0) return ItemStack.EMPTY;
            MaterialPrefix form = PrefixRegistry.byName(prefix);
            if (form == null) return ItemStack.EMPTY;
            GTMaterial resolved = GTMaterialRegistry.get(material);
            if (!resolved.isValid()) return ItemStack.EMPTY;
            ItemStack stack = GTItems.getStack(form, resolved, count);
            return stack == null ? ItemStack.EMPTY : stack;
        }

        @Override public boolean matchesTool(ItemStack stack, AddonTool tool) {
            return stack != null && tool != null && GTToolHelper.matchesTool(stack, GTToolType.valueOf(tool.name()));
        }

        @Override public void damageTool(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot) {
            if (stack == null || slot == null) return;
            GTToolHelper.damageForUse(stack, amount, user, slot);
        }

        @Override public long inject(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute) {
            return EnergyBlockDefaults.doEnergyInjection(AddonEnergyAdapter.of(block), tag(energy), side, size, amount, execute);
        }

        @Override public long extract(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute) {
            return EnergyBlockDefaults.doEnergyExtraction(AddonEnergyAdapter.of(block), tag(energy), side, size, amount, execute);
        }
    }
}
