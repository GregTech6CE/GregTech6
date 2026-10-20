package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Item-input indexes a machine checks for and does not consume.
 * A count of 0 is not a mark; the stack stays at 1 or more.
 */
public final class RecipeKeptInputs {
    private RecipeKeptInputs() {}

    /** Null and an empty list mark nothing. Out of range and duplicates fail. Order is kept. */
    public static int[] normalize(int[] kept, int itemInputs) {
        if (itemInputs < 0) throw new IllegalArgumentException("Invalid item input count: " + itemInputs);
        if (kept == null || kept.length == 0) return new int[0];
        boolean[] seen = itemInputs == 0 ? new boolean[0] : new boolean[itemInputs];
        int[] copy = new int[kept.length];
        for (int i = 0; i < kept.length; i++) {
            int index = kept[i];
            if (index < 0 || index >= itemInputs)
                throw new IllegalArgumentException("Kept input " + index + " is outside 0.." + (itemInputs - 1));
            if (seen[index]) throw new IllegalArgumentException("Duplicate kept input " + index);
            seen[index] = true;
            copy[i] = index;
        }
        return copy;
    }

    /** Count 0 is rejected. The message names {@code keptInputs}. */
    public static void requirePositiveCount(int count, int index) {
        if (count > 0) return;
        throw new IllegalArgumentException("Item input " + index + " has count " + count
                + "; mark it in keptInputs and leave the count at 1");
    }
}
