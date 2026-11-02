package gregtech.content.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Stable minimum-step relaxation, adapted from brokestar's iterative pipe scan.
 * Loaded-world, face and inventory checks are supplied by the platform adapter.
 * The bounded loaded-only traversal follows masson's network-discovery boundary.
 * Source hashes and original author/license evidence are in core/provenance.
 *
 * <p>One scan per source and network topology: the result lists every exit the source can reach,
 * nearest first, plus the shortest-path tree that leads to them. The platform can reuse it until
 * the network changes or the source's route is evicted from its bounded cache.</p>
 */
public final class WeightedItemPipeRoutes {
    public static final int MAX_VISITED_PIPES = 32_768;

    private WeightedItemPipeRoutes() {}

    /** A pipe network in local indices {@code 0 .. pipes() - 1}. */
    public interface Graph {
        int pipes();

        /** Routing cost of entering {@code pipe}; never negative. */
        long stepSize(int pipe);

        /** Pipes {@code pipe} may hand items to. */
        int[] links(int pipe);

        /** Exit ids beside {@code pipe}, in a fixed order. */
        int[] exits(int pipe);
    }

    /** One exit beside {@code pipe}, at the accumulated step cost of reaching that pipe. */
    public record Exit(int exit, int pipe, long cost) {}

    /**
     * Every exit reachable from {@code source}, nearest first, and the shortest-path tree leading to
     * them: {@code previous[pipe]} is the pipe before {@code pipe} on its path, {@code -1} for the
     * source. Walking {@code previous} from an exit's pipe ends at the source.
     */
    public record Routes(int source, List<Exit> exits, int[] previous) {}

    private record Frontier(int pipe, long cost, long order) {}

    /**
     * Dijkstra from {@code source}, the source costing its own step. Equal costs keep discovery order;
     * exits beside one pipe keep the graph's order. At most {@code visitLimit} pipes are settled.
     */
    public static Routes scan(Graph graph, int source, int visitLimit) {
        int size = graph.pipes();
        int[] previous = new int[size];
        Arrays.fill(previous, -1);
        List<Exit> exits = new ArrayList<>();
        if (source < 0 || source >= size || visitLimit <= 0) return new Routes(source, List.of(), previous);
        long[] cost = new long[size];
        Arrays.fill(cost, Long.MAX_VALUE);
        boolean[] discovered = new boolean[size];
        boolean[] settled = new boolean[size];
        PriorityQueue<Frontier> queue =
                new PriorityQueue<>(Comparator.comparingLong(Frontier::cost).thenComparingLong(Frontier::order));
        long order = 0;
        cost[source] = addStep(0, graph.stepSize(source));
        discovered[source] = true;
        queue.add(new Frontier(source, cost[source], order++));
        int visited = 0;
        while (!queue.isEmpty() && visited < visitLimit) {
            Frontier current = queue.remove();
            int pipe = current.pipe();
            if (settled[pipe] || current.cost() != cost[pipe]) continue;
            settled[pipe] = true;
            visited++;
            for (int exit : graph.exits(pipe)) exits.add(new Exit(exit, pipe, current.cost()));
            for (int next : graph.links(pipe)) {
                if (settled[next]) continue;
                long nextCost = addStep(current.cost(), graph.stepSize(next));
                // A saturated cost is still reachable, so it cannot also mark an unseen pipe.
                if (!discovered[next] || nextCost < cost[next]) {
                    discovered[next] = true;
                    cost[next] = nextCost;
                    previous[next] = pipe;
                    queue.add(new Frontier(next, nextCost, order++));
                }
            }
        }
        return new Routes(source, List.copyOf(exits), previous);
    }

    private static long addStep(long accumulated, long step) {
        if (step < 0) throw new IllegalArgumentException("Negative pipe routing step");
        return accumulated > Long.MAX_VALUE - step ? Long.MAX_VALUE : accumulated + step;
    }
}
