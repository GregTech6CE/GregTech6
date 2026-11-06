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

    /**
     * Feasible allocation when an input may draw from any accepting slot, regardless of identity.
     * A greedy first-match scan can strand an earlier broad requirement on the only slot a later
     * specific requirement accepts; this is the same bipartite assignment solved as a flow, so the
     * result depends on availability rather than on inventory order.
     *
     * @param available units currently present per slot
     * @param demand    units needed per input
     * @param accepts   whether an input can consume a slot at all
     * @return per-input per-slot amounts, or {@code null} when the demand cannot be met
     */
    public static int[][] planMixed(int[] available, long[] demand, boolean[][] accepts) {
        int inputs = demand.length;
        int slots = available.length;
        long total = 0;
        for (long need : demand) total += need;
        if (total == 0) return new int[inputs][slots];
        int source = 0;
        int firstInput = 1;
        int firstSlot = firstInput + inputs;
        int sink = firstSlot + slots;
        long[][] capacity = new long[sink + 1][sink + 1];
        for (int input = 0; input < inputs; input++) capacity[source][firstInput + input] = demand[input];
        for (int slot = 0; slot < slots; slot++) capacity[firstSlot + slot][sink] = Math.max(0, available[slot]);
        for (int input = 0; input < inputs; input++)
            for (int slot = 0; slot < slots; slot++)
                if (accepts[input][slot] && available[slot] > 0)
                    capacity[firstInput + input][firstSlot + slot] =
                            Math.min(demand[input], Math.max(0, available[slot]));
        long flow = 0;
        int[] parent = new int[sink + 1];
        while (true) {
            java.util.Arrays.fill(parent, -1);
            parent[source] = source;
            var queue = new java.util.ArrayDeque<Integer>();
            queue.add(source);
            while (!queue.isEmpty() && parent[sink] < 0) {
                int current = queue.poll();
                for (int next = 0; next <= sink; next++)
                    if (parent[next] < 0 && capacity[current][next] > 0) {
                        parent[next] = current;
                        queue.add(next);
                    }
            }
            if (parent[sink] < 0) break;
            long push = Long.MAX_VALUE;
            for (int node = sink; node != source; node = parent[node])
                push = Math.min(push, capacity[parent[node]][node]);
            for (int node = sink; node != source; node = parent[node]) {
                capacity[parent[node]][node] -= push;
                capacity[node][parent[node]] += push;
            }
            flow += push;
        }
        if (flow != total) return null;
        int[][] taken = new int[inputs][slots];
        for (int input = 0; input < inputs; input++)
            for (int slot = 0; slot < slots; slot++) {
                // The residual back edge carries exactly what the forward edge accepted.
                long used = capacity[firstSlot + slot][firstInput + input];
                if (used > 0) taken[input][slot] = (int) used;
            }
        return taken;
    }
}
