/*
 * Blueprint data adapted from Gregorius Techneticies' UT.NBT / RecipeMapAutocrafting,
 * LGPL-3.0-or-later. Existing port blueprint lists remain readable.
 */
package com.gregtech.gregtech.content.data;

import com.gregtech.gregtech.content.recipe.AutocraftingRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.Arrays;

public final class CraftingBlueprintData {
    private CraftingBlueprintData() {}

    public static ItemStack[] read(Level level, CompoundTag data) {
        if (data == null || !data.contains(AutocraftingRules.BLUEPRINT_KEY, 10)
                && !data.contains(AutocraftingRules.LEGACY_KEY, 9)) return new ItemStack[0];
        ItemStack[] pattern = new ItemStack[AutocraftingRules.CELLS];
        Arrays.fill(pattern, ItemStack.EMPTY);
        if (data.contains(AutocraftingRules.BLUEPRINT_KEY, 10)) {
            CompoundTag cells = data.getCompound(AutocraftingRules.BLUEPRINT_KEY);
            for (int i = 0; i < pattern.length; i++) if (cells.contains(Integer.toString(i), 10))
                pattern[i] = ItemStack.of(cells.getCompound(Integer.toString(i))).copyWithCount(1);
        } else {
            var cells = data.getList(AutocraftingRules.LEGACY_KEY, 10);
            for (int i = 0; i < Math.min(pattern.length, cells.size()); i++)
                pattern[i] = ItemStack.of(cells.getCompound(i)).copyWithCount(1);
        }
        return pattern;
    }

    public static CompoundTag itemData(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }
    public static ItemStack[] readItem(Level level, ItemStack stack) {
        return read(level, itemData(stack));
    }
    public static CompoundTag write(Level level, ItemStack[] pattern) {
        if (pattern.length != AutocraftingRules.CELLS) throw new IllegalArgumentException("Blueprint grid");
        CompoundTag data = new CompoundTag(), cells = new CompoundTag();
        for (int i = 0; i < pattern.length; i++) if (!pattern[i].isEmpty())
            cells.put(Integer.toString(i), pattern[i].copyWithCount(1).save(new CompoundTag()));
        if (!cells.isEmpty()) data.put(AutocraftingRules.BLUEPRINT_KEY, cells);
        return data;
    }
    public static void writeItem(Level level, ItemStack stack, ItemStack[] pattern) {
        CompoundTag data = write(level, pattern);
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(AutocraftingRules.LEGACY_KEY);
        tag.remove(AutocraftingRules.BLUEPRINT_KEY);
        tag.merge(data);
    }
}

