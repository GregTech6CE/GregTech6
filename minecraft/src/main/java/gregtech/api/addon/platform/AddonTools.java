/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Platform checks against GregTech's tool table; names come from {@link AddonTool}. */
public final class AddonTools {
    private AddonTools() {}

    public static boolean matches(ItemStack stack, AddonTool tool) {
        return AddonPlatform.services().matchesTool(stack, tool);
    }

    public static boolean isMagnifyingGlass(ItemStack stack) {
        return matches(stack, AddonTool.MAGNIFYING_GLASS);
    }

    public static void damageForUse(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot) {
        AddonPlatform.services().damageTool(stack, amount, user, slot);
    }
}
