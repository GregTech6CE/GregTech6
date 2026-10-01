package com.gregtech.gregtech.content.transport;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Stable minimum-step relaxation, adapted from brokestar's iterative pipe scan.
 * Loaded-world, face and inventory checks are supplied by the platform adapter.
 * The bounded loaded-only traversal follows masson's network-discovery boundary.
 * Source hashes and original author/license evidence are in core/provenance.
 */
public final class WeightedItemPipeRoutes {
    public static final int MAX_VISITED_PIPES = 32_768;

    private WeightedItemPipeRoutes() {}

    private record Frontier<N>(N node, long cost, long order) {}

    public static <N> long minimumExitCost(N start, ToLongFunction<N> step,
            Function<N, ? extends Iterable<N>> neighbors, Predicate<N> accepts,
            int visitLimit) {
        if (start == null || visitLimit <= 0) return -1;
        PriorityQueue<Frontier<N>> queue = new PriorityQueue<>(
                Comparator.<Frontier<N>>comparingLong(Frontier::cost)
                        .thenComparingLong(Frontier::order));
        Map<N, Long> best = new HashMap<>();
        long initial = addStep(0, step.applyAsLong(start));
        long order = 0;
        best.put(start, initial);
        queue.add(new Frontier<>(start, initial, order++));
        int visited = 0;
        while (!queue.isEmpty() && visited < visitLimit) {
            Frontier<N> current = queue.remove();
            if (best.get(current.node()) != current.cost()) continue;
            visited++;
            if (accepts.test(current.node())) return current.cost();
            for (N next : neighbors.apply(current.node())) {
                long cost = addStep(current.cost(), step.applyAsLong(next));
                Long previous = best.get(next);
                if (previous == null || cost < previous) {
                    best.put(next, cost);
                    queue.add(new Frontier<>(next, cost, order++));
                }
            }
        }
        return -1;
    }

    private static long addStep(long accumulated, long step) {
        if (step < 0) throw new IllegalArgumentException("Negative pipe routing step");
        return accumulated > Long.MAX_VALUE - step ? Long.MAX_VALUE : accumulated + step;
    }
}
