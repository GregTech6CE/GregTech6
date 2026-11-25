package gregtech.engine.fluid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToLongFunction;

/** Levels one fluid over the edges that admit it in both directions. */
public final class FluidPipeBalance {
    private static final long DEADBAND = 1;

    private FluidPipeBalance() {}

    @FunctionalInterface
    public interface Transfer {
        void move(int source, int target, long requested);
    }

    /**
     * Retains neighbor order when building each spanning tree; traversal order breaks ties
     * between target amounts. Transfers may be partial, so subsequent decisions read live amounts.
     */
    public static void distribute(
            List<List<Integer>> neighbors, long[] capacity, IntToLongFunction amount, Transfer transfer) {
        int size = neighbors.size();
        long[] initialAmount = new long[size];
        for (int node = 0; node < size; node++) {
            if (!neighbors.get(node).isEmpty()) initialAmount[node] = amount.applyAsLong(node);
        }
        boolean[] visited = new boolean[size];
        int[] parent = new int[size];
        long[] balance = new long[size];
        List<Integer> order = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int root = 0; root < size; root++) {
            if (visited[root] || neighbors.get(root).isEmpty()) continue;
            order.clear();
            visited[root] = true;
            stack.push(root);
            while (!stack.isEmpty()) {
                int node = stack.pop();
                order.add(node);
                for (int next : neighbors.get(node)) {
                    if (visited[next]) continue;
                    visited[next] = true;
                    parent[next] = node;
                    stack.push(next);
                }
            }
            balanceTree(order, parent, balance, initialAmount, capacity, amount, transfer);
        }
    }

    private static void balanceTree(
            List<Integer> order,
            int[] parent,
            long[] balance,
            long[] initialAmount,
            long[] capacity,
            IntToLongFunction amount,
            Transfer transfer) {
        long total = 0;
        long totalCapacity = 0;
        for (int node : order) {
            total += initialAmount[node];
            totalCapacity += capacity[node];
        }
        if (total <= 0 || total >= totalCapacity) return;
        long[] target = FluidPipeChannels.balancedTargets(order, capacity, total);

        // Leaves shed excess into their parents before deficits are measured.
        for (int k = order.size() - 1; k > 0; k--) {
            int node = order.get(k);
            long surplus = amount.applyAsLong(node) - target[node];
            if (surplus > DEADBAND) transfer.move(node, parent[node], surplus);
        }
        for (int node : order) balance[node] = amount.applyAsLong(node) - target[node];
        for (int k = order.size() - 1; k > 0; k--) {
            int node = order.get(k);
            balance[parent[node]] += balance[node];
        }
        // A pipe at its own target can still supply a deficit farther down its branch.
        for (int k = 1; k < order.size(); k++) {
            int node = order.get(k);
            if (balance[node] < -DEADBAND) transfer.move(parent[node], node, -balance[node]);
        }
    }
}
