/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;




/**
 * Internal: not part of the addon contract; may change without notice.
 * Input indexes a machine checks for and does not consume.
 * A count of 0, or 0 mB, is not a mark; the declared amount stays positive.
 */
public final class RecipeKeptInputs {
    private RecipeKeptInputs() {}

    /** Null and an empty list mark nothing. Out of range and duplicates fail. Order is kept. */
    public static int[] normalize(int[] kept, int itemInputs) {
        return indexes(kept, itemInputs, "item input count", "Kept input ", "Duplicate kept input ");
    }

    /** Count 0 is rejected. The message names {@code keptInputs}. */
    public static void requirePositiveCount(int count, int index) {
        if (count > 0) return;
        throw new IllegalArgumentException("Item input " + index + " has count " + count
                + "; mark it in keptInputs and leave the count at 1");
    }

    /** Null and an empty list mark nothing. Out of range and duplicates fail. Order is kept. */
    public static int[] normalizeFluids(int[] kept, int fluidInputs) {
        return indexes(kept, fluidInputs, "fluid input count", "Kept fluid input ", "Duplicate kept fluid input ");
    }

    private static int[] indexes(int[] kept, int count, String countLabel, String outside, String duplicate) {
        if (count < 0) throw new IllegalArgumentException("Invalid " + countLabel + ": " + count);
        if (kept == null || kept.length == 0) return new int[0];
        boolean[] seen = new boolean[count];
        int[] copy = new int[kept.length];
        for (int i = 0; i < kept.length; i++) {
            int index = kept[i];
            if (index < 0 || index >= count)
                throw new IllegalArgumentException(outside + index + " is outside "
                        + (count == 0 ? "an empty range" : "0.." + (count - 1)));
            if (seen[index]) throw new IllegalArgumentException(duplicate + index);
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
     * {@code -1} means the tank cannot cover the demand. It is not a missing lookup.
     */
    public static int settle(boolean kept, int declared, int parallel, int stored) {
        long demand = fluidDemand(kept, declared, parallel);
        if (demand < 1 || stored < demand || demand > Integer.MAX_VALUE) return -1;
        return kept ? stored : stored - (int) demand;
    }
}
