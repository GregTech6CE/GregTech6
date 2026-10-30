/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.recipe.SelectorTags;
import net.minecraft.world.item.ItemStack;

/**
 * GregTech material forms and programmed circuits as item stacks.
 * An unknown prefix or material is {@link ItemStack#EMPTY}, never null.
 * A selector outside 0..24, or one that is not registered, throws {@link IllegalArgumentException},
 * the same exception {@link AddonRecipes#map(String)} throws for an unknown map.
 */
public final class AddonItems {
    private AddonItems() {}

    public static ItemStack stack(String prefix, String material, int count) {
        return AddonPlatform.services().stack(prefix, material, count);
    }

    public static ItemStack stack(String prefix, String material) {
        return stack(prefix, material, 1);
    }

    /**
     * Prefix name and material units GregTech already stores for this stack.
     * An unknown stack is empty. The stack count is not multiplied in.
     */
    public static GregTechMaterials.StackComposition composition(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return GregTechMaterials.StackComposition.EMPTY;
        return AddonPlatform.services().composition(stack);
    }

    /** Programmed circuit {@code n} ({@code integrated_circuit_n}), count 1. Outside 0..24 fails. */
    public static ItemStack selectorTag(int n) {
        SelectorTags.require(n);
        ItemStack stack = AddonPlatform.services().selectorTag(n);
        if (stack == null || stack.isEmpty())
            throw new IllegalArgumentException("Selector tag " + n + " is not registered");
        return stack;
    }
}
