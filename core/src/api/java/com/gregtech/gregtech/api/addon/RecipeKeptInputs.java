package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Input indexes a machine checks for and does not consume.
 * A count of 0, or 0 mB, is not a mark; the declared amount stays positive.
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

    /** Null and an empty list mark nothing. Out of range and duplicates fail. Order is kept. */
    public static int[] normalizeFluids(int[] kept, int fluidInputs) {
        if (fluidInputs < 0) throw new IllegalArgumentException("Invalid fluid input count: " + fluidInputs);
        if (kept == null || kept.length == 0) return new int[0];
        boolean[] seen = fluidInputs == 0 ? new boolean[0] : new boolean[fluidInputs];
        int[] copy = new int[kept.length];
        for (int i = 0; i < kept.length; i++) {
            int index = kept[i];
            if (index < 0 || index >= fluidInputs)
                throw new IllegalArgumentException("Kept fluid input " + index + " is outside 0.." + (fluidInputs - 1));
            if (seen[index]) throw new IllegalArgumentException("Duplicate kept fluid input " + index);
            seen[index] = true;
            copy[i] = index;
        }
        return copy;
    }

    /**
     * Native stack representatives must stay positive so their fluid identity survives copying.
     * Textual zero-mB inputs are normalized before this boundary and use separate presence indexes;
     * positive retained quantities use {@code keptFluidInputs}.
     */
    public static void requirePositiveFluid(int amount, int index) {
        if (amount > 0) return;
        throw new IllegalArgumentException("Fluid input " + index + " has amount " + amount);
    }

    /** A kept fluid is checked once. An ordinary fluid is checked once per parallel. */
    public static long fluidDemand(boolean kept, int declared, int parallel) {
        if (declared < 1 || parallel < 1) return 0;
        return (long) declared * (kept ? 1 : parallel);
    }

    /**
     * Amount left in one tank after the demand is met. {@code -1} when the tank is short.
     * A kept fluid is put back, so the stored amount is unchanged.
     */
    public static int settle(boolean kept, int declared, int parallel, int stored) {
        long demand = fluidDemand(kept, declared, parallel);
        if (demand < 1 || stored < demand || demand > Integer.MAX_VALUE) return -1;
        return kept ? stored : stored - (int) demand;
    }
}
