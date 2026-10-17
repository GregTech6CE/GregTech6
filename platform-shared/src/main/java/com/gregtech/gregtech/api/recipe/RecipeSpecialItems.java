package com.gregtech.gregtech.api.recipe;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Viewer-only copies of GT6's non-consumable special slot; never changes a machine recipe. */
public final class RecipeSpecialItems {
    private RecipeSpecialItems() {}
    public static List<ItemStack> display(Object value) {
        List<ItemStack> result = new ArrayList<>();
        collect(value, result);
        return List.copyOf(result);
    }
    private static void collect(Object value, List<ItemStack> result) {
        if (value instanceof ItemStack stack && !stack.isEmpty()) result.add(stack.copy());
        else if (value instanceof Object[] array) for (Object item : array) collect(item, result);
        else if (value instanceof Iterable<?> items) for (Object item : items) collect(item, result);
    }
}
