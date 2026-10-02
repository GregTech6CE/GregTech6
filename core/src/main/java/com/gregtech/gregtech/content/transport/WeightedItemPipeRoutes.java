package com.gregtech.gregtech.content.transport;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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

    public record Exit<N>(N firstHop, long cost) {}
    private record Frontier<N>(N node, long cost, N firstHop, int firstOrder, long order) {}

    public static <N> long minimumExitCost(N start, ToLongFunction<N> step,
            Function<N, ? extends Iterable<N>> neighbors, Predicate<N> accepts,
            int visitLimit) {
        if (start == null) return -1;
        Exit<N> exit = bestExit(List.of(start), step, neighbors, accepts, visitLimit);
        return exit == null ? -1 : exit.cost();
    }

    /** One bounded scan for all legal first hops; equal-cost routes retain first-hop ordering. */
    public static <N> Exit<N> bestExit(Iterable<N> starts, ToLongFunction<N> step,
            Function<N, ? extends Iterable<N>> neighbors, Predicate<N> accepts,
            int visitLimit) {
        if (visitLimit <= 0) return null;
        PriorityQueue<Frontier<N>> queue = new PriorityQueue<>(
                Comparator.<Frontier<N>>comparingLong(Frontier::cost)
                        .thenComparingInt(Frontier::firstOrder).thenComparingLong(Frontier::order));
        Map<N, Frontier<N>> best = new HashMap<>();
        long order = 0;
        int firstOrder = 0;
        for (N start : starts) {
            if (start == null || best.containsKey(start)) continue;
            Frontier<N> initial = new Frontier<>(start, addStep(0, step.applyAsLong(start)),
                    start, firstOrder++, order++);
            best.put(start, initial);
            queue.add(initial);
        }
        int visited = 0;
        while (!queue.isEmpty() && visited < visitLimit) {
            Frontier<N> current = queue.remove();
            if (best.get(current.node()) != current) continue;
            visited++;
            if (accepts.test(current.node())) return new Exit<>(current.firstHop(), current.cost());
            for (N next : neighbors.apply(current.node())) {
                long cost = addStep(current.cost(), step.applyAsLong(next));
                Frontier<N> previous = best.get(next);
                if (previous == null || cost < previous.cost()
                        || (cost == previous.cost() && current.firstOrder() < previous.firstOrder())) {
                    Frontier<N> candidate = new Frontier<>(next, cost, current.firstHop(), current.firstOrder(), order++);
                    best.put(next, candidate);
                    queue.add(candidate);
                }
            }
        }
        return null;
    }

    private static long addStep(long accumulated, long step) {
        if (step < 0) throw new IllegalArgumentException("Negative pipe routing step");
        return accumulated > Long.MAX_VALUE - step ? Long.MAX_VALUE : accumulated + step;
    }
}
