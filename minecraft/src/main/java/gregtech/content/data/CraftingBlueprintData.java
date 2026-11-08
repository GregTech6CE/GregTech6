/*
 * Blueprint data adapted from Gregorius Techneticies' UT.NBT / RecipeMapAutocrafting,
 * LGPL-3.0-or-later. Recognizes keyed grids and earlier list layouts; entries use native version serializers.
 */
package gregtech.content.data;

import gregtech.content.recipe.AutocraftingRules;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CraftingBlueprintData {

    private CraftingBlueprintData() {}

    public static ItemStack[] read(Level level, CompoundTag data) {
        if (data == null
                || !data.contains(AutocraftingRules.BLUEPRINT_KEY, 10)
                        && !data.contains(AutocraftingRules.LEGACY_KEY, 9)) return new ItemStack[0];
        ItemStack[] pattern = new ItemStack[AutocraftingRules.CELLS];
        Arrays.fill(pattern, ItemStack.EMPTY);
        if (data.contains(AutocraftingRules.BLUEPRINT_KEY, 10)) {
            CompoundTag cells = data.getCompound(AutocraftingRules.BLUEPRINT_KEY);
            for (int i = 0; i < pattern.length; i++)
                if (cells.contains(Integer.toString(i), 10))
                    pattern[i] = gregtech.engine.stack.NbtCompat.readItem(
                                    cells.getCompound(Integer.toString(i)), level.registryAccess())
                            .copyWithCount(1);
        } else {
            var cells = data.getList(AutocraftingRules.LEGACY_KEY, 10);
            for (int i = 0; i < Math.min(pattern.length, cells.size()); i++)
                pattern[i] = gregtech.engine.stack.NbtCompat.readItem(cells.getCompound(i), level.registryAccess())
                        .copyWithCount(1);
        }
        return pattern;
    }

    public static CompoundTag itemData(ItemStack stack) {
        return gregtech.engine.stack.StackData.readOrEmpty(stack);
    }

    public static ItemStack[] readItem(Level level, ItemStack stack) {
        return read(level, itemData(stack));
    }

    public static CompoundTag write(Level level, ItemStack[] pattern) {
        if (pattern.length != AutocraftingRules.CELLS) throw new IllegalArgumentException("Blueprint grid");
        CompoundTag data = new CompoundTag(), cells = new CompoundTag();
        for (int i = 0; i < pattern.length; i++)
            if (!pattern[i].isEmpty())
                cells.put(
                        Integer.toString(i),
                        gregtech.engine.stack.NbtCompat.saveItem(pattern[i].copyWithCount(1), level.registryAccess()));
        if (!cells.isEmpty()) data.put(AutocraftingRules.BLUEPRINT_KEY, cells);
        return data;
    }

    public static void writeItem(Level level, ItemStack stack, ItemStack[] pattern) {
        CompoundTag data = write(level, pattern);
        gregtech.engine.stack.StackData.update(stack, tag -> {
            tag.remove(AutocraftingRules.LEGACY_KEY);
            tag.remove(AutocraftingRules.BLUEPRINT_KEY);
            tag.merge(data);
        });
    }
}
