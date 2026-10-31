package gregtech.engine.recipe;

/** Allocates overlapping ingredients without expanding their Cartesian product into recipes. */
public final class IngredientAllocation {
    private IngredientAllocation() {}

    /**
     * Each homogeneous requirement chooses one identity, but may consume it from multiple slots.
     * Backtracking prevents a broad tag from stealing the only input accepted by a later slot.
     */
    public static int[][] plan(
            int[] available, long[] demand, boolean[][] accepts, int[] identities, boolean[] homogeneous) {
        int[][] taken = new int[demand.length][available.length];
        return assign(0, available.clone(), demand, accepts, identities, homogeneous, taken) ? taken : null;
    }

    private static boolean assign(
            int input,
            int[] remaining,
            long[] demand,
            boolean[][] accepts,
            int[] identities,
            boolean[] homogeneous,
            int[][] taken) {
        if (input == demand.length) return true;
        if (demand[input] == 0) return assign(input + 1, remaining, demand, accepts, identities, homogeneous, taken);
        for (int choice = 0; choice < remaining.length; choice++) {
            if (remaining[choice] == 0 || !accepts[input][choice]) continue;
            boolean repeated = false;
            if (homogeneous[input])
                for (int previous = 0; previous < choice; previous++) {
                    if (remaining[previous] > 0
                            && accepts[input][previous]
                            && identities[previous] == identities[choice]) {
                        repeated = true;
                        break;
                    }
                }
            if (repeated) continue;
            long needed = demand[input];
            for (int slot = 0; slot < remaining.length && needed > 0; slot++) {
                if (!accepts[input][slot] || homogeneous[input] && identities[slot] != identities[choice]) continue;
                int count = (int) Math.min(needed, remaining[slot]);
                remaining[slot] -= count;
                taken[input][slot] = count;
                needed -= count;
            }
            if (needed == 0 && assign(input + 1, remaining, demand, accepts, identities, homogeneous, taken))
                return true;
            for (int slot = 0; slot < remaining.length; slot++) {
                remaining[slot] += taken[input][slot];
                taken[input][slot] = 0;
            }
            if (!homogeneous[input]) break;
        }
        return false;
    }
}
