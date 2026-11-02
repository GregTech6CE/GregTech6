package gregtech.content.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Stable minimum-step relaxation, adapted from brokestar's iterative pipe scan.
 * Loaded-world, face and inventory checks are supplied by the platform adapter.
 * The bounded loaded-only traversal follows masson's network-discovery boundary.
 * Source hashes and original author/license evidence are in core/provenance.
 *
 * <p>A {@link Search} is a resumable Dijkstra from one source. Exits come out nearest first and
 * only as far as a delivery asks, so a source whose nearest exit takes its items explores only up
 * to that exit. Memory grows with the explored region, not with the network, and a search is
 * thrown away whole when the network's layout changes.</p>
 */
public final class WeightedItemPipeRoutes {
    public static final int MAX_VISITED_PIPES = 32_768;

    private WeightedItemPipeRoutes() {}

    /** A pipe network in local indices {@code 0 .. pipes() - 1}, fixed for a search's lifetime. */
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

    /** A fully evaluated search, with {@code -1} marking an absent predecessor or the source. */
    public record Routes(int source, List<Exit> exits, int[] previous) {}

    /** Evaluate all reachable exits; delivery callers should retain a lazy {@link Search} instead. */
    public static Routes scan(Graph graph, int source, int visitLimit) {
        Search search = new Search(graph, source, visitLimit);
        List<Exit> exits = new ArrayList<>();
        for (int i = 0; search.hasExit(i); i++) {
            exits.add(new Exit(search.exit(i), search.exitPipe(i), search.exitCost(i)));
        }
        int[] previous = new int[graph.pipes()];
        Arrays.fill(previous, -1);
        for (int i = 0; i < search.records; i++) previous[search.nodes[i]] = search.previous[i];
        return new Routes(source, List.copyOf(exits), previous);
    }

    /**
     * Nearest-first exits from one source, discovered on demand. Equal costs keep discovery order;
     * exits beside one pipe keep the graph's order. At most {@code visitLimit} pipes are settled.
     * Not thread-safe; one per source.
     */
    public static final class Search {
        private static final int ABSENT = -1;

        private final Graph graph;
        private final int source;
        private final int visitLimit;

        // Explored pipes: open-addressed table of record index + 1, records in parallel arrays.
        private int[] table = new int[16];
        private int[] nodes = new int[8];
        private long[] costs = new long[8];
        private int[] previous = new int[8];
        private boolean[] settled = new boolean[8];
        private int records;

        // Frontier: binary min-heap ordered by (cost, discovery order); stale entries are skipped.
        private long[] heapCost = new long[8];
        private long[] heapOrder = new long[8];
        private int[] heapNode = new int[8];
        private int heapSize;
        private long order;

        private int[] exitIds = new int[4];
        private int[] exitPipes = new int[4];
        private long[] exitCosts = new long[4];
        private int exitCount;
        private int visited;
        private int pending = ABSENT;
        private boolean exhausted;

        public Search(Graph graph, int source, int visitLimit) {
            this.graph = Objects.requireNonNull(graph);
            this.source = source;
            this.visitLimit = visitLimit;
            if (source < 0 || source >= graph.pipes() || visitLimit <= 0) {
                exhausted = true;
                return;
            }
            long cost = addStep(0, graph.stepSize(source));
            record(source, cost, ABSENT);
            push(source, cost);
        }

        public int source() {
            return source;
        }

        /** Whether an {@code index}-th nearest exit exists, exploring further only if needed. */
        public boolean hasExit(int index) {
            if (index < 0) throw new IndexOutOfBoundsException(index);
            while (exitCount <= index && !exhausted) step();
            return index < exitCount;
        }

        public int exit(int index) {
            Objects.checkIndex(index, exitCount);
            return exitIds[index];
        }

        public int exitPipe(int index) {
            Objects.checkIndex(index, exitCount);
            return exitPipes[index];
        }

        public long exitCost(int index) {
            Objects.checkIndex(index, exitCount);
            return exitCosts[index];
        }

        /** The pipe before {@code pipe} on its shortest path; {@code -1} for the source. */
        public int previous(int pipe) {
            int at = find(pipe);
            return at < 0 ? ABSENT : previous[at];
        }

        /** Pipes recorded so far, including the unsettled frontier. */
        public int explored() {
            return records;
        }

        /**
         * Estimated retained bytes for cache budgeting, including allocated array capacity and a
         * conservative fixed allowance for object/array headers and alignment. Excludes the shared graph.
         */
        public long retainedBytes() {
            return 512L + 4L * table.length + 17L * nodes.length + 20L * heapNode.length + 16L * exitIds.length;
        }

        private void step() {
            // Offer exits before expanding their pipe. A nearby accepting exit can stop the search here.
            if (pending >= 0) {
                int pipe = nodes[pending];
                long cost = costs[pending];
                pending = ABSENT;
                for (int next : graph.links(pipe)) {
                    int known = find(next);
                    if (known >= 0 && settled[known]) continue;
                    long nextCost = addStep(cost, graph.stepSize(next));
                    if (known < 0) {
                        record(next, nextCost, pipe);
                        push(next, nextCost);
                    } else if (nextCost < costs[known]) {
                        costs[known] = nextCost;
                        previous[known] = pipe;
                        push(next, nextCost);
                    }
                }
            }
            while (heapSize > 0) {
                int pipe = heapNode[0];
                long cost = heapCost[0];
                pop();
                int at = find(pipe);
                if (settled[at] || costs[at] != cost) continue;
                settled[at] = true;
                visited++;
                for (int exit : graph.exits(pipe)) addExit(exit, pipe, cost);
                if (visited < visitLimit) pending = at;
                else exhausted = true;
                return;
            }
            exhausted = true;
        }

        private void addExit(int exit, int pipe, long cost) {
            if (exitCount == exitIds.length) {
                exitIds = Arrays.copyOf(exitIds, exitCount * 2);
                exitPipes = Arrays.copyOf(exitPipes, exitCount * 2);
                exitCosts = Arrays.copyOf(exitCosts, exitCount * 2);
            }
            exitIds[exitCount] = exit;
            exitPipes[exitCount] = pipe;
            exitCosts[exitCount] = cost;
            exitCount++;
        }

        private static int slot(int pipe, int mask) {
            return (pipe * 0x9E3779B9) >>> 7 & mask;
        }

        private int find(int pipe) {
            int mask = table.length - 1;
            for (int slot = slot(pipe, mask); ; slot = slot + 1 & mask) {
                int entry = table[slot];
                if (entry == 0) return ABSENT;
                if (nodes[entry - 1] == pipe) return entry - 1;
            }
        }

        private void record(int pipe, long cost, int before) {
            if (records == nodes.length) {
                int size = records * 2;
                nodes = Arrays.copyOf(nodes, size);
                costs = Arrays.copyOf(costs, size);
                previous = Arrays.copyOf(previous, size);
                settled = Arrays.copyOf(settled, size);
            }
            nodes[records] = pipe;
            costs[records] = cost;
            previous[records] = before;
            records++;
            if (records * 2 > table.length) {
                table = new int[table.length * 2];
                for (int i = 0; i < records; i++) place(i);
            } else {
                place(records - 1);
            }
        }

        private void place(int recordIndex) {
            int mask = table.length - 1;
            int slot = slot(nodes[recordIndex], mask);
            while (table[slot] != 0) slot = slot + 1 & mask;
            table[slot] = recordIndex + 1;
        }

        private boolean before(int a, int b) {
            return heapCost[a] < heapCost[b] || heapCost[a] == heapCost[b] && heapOrder[a] < heapOrder[b];
        }

        private void push(int pipe, long cost) {
            if (heapSize == heapNode.length) {
                int size = heapSize * 2;
                heapCost = Arrays.copyOf(heapCost, size);
                heapOrder = Arrays.copyOf(heapOrder, size);
                heapNode = Arrays.copyOf(heapNode, size);
            }
            int at = heapSize++;
            heapCost[at] = cost;
            heapOrder[at] = order++;
            heapNode[at] = pipe;
            while (at > 0) {
                int parent = (at - 1) >>> 1;
                if (!before(at, parent)) break;
                swap(at, parent);
                at = parent;
            }
        }

        private void pop() {
            heapSize--;
            if (heapSize == 0) return;
            heapCost[0] = heapCost[heapSize];
            heapOrder[0] = heapOrder[heapSize];
            heapNode[0] = heapNode[heapSize];
            for (int at = 0; ; ) {
                int left = at * 2 + 1, right = left + 1, best = at;
                if (left < heapSize && before(left, best)) best = left;
                if (right < heapSize && before(right, best)) best = right;
                if (best == at) return;
                swap(at, best);
                at = best;
            }
        }

        private void swap(int a, int b) {
            long cost = heapCost[a];
            heapCost[a] = heapCost[b];
            heapCost[b] = cost;
            long order = heapOrder[a];
            heapOrder[a] = heapOrder[b];
            heapOrder[b] = order;
            int node = heapNode[a];
            heapNode[a] = heapNode[b];
            heapNode[b] = node;
        }
    }

    private static long addStep(long accumulated, long step) {
        if (step < 0) throw new IllegalArgumentException("Negative pipe routing step");
        return accumulated > Long.MAX_VALUE - step ? Long.MAX_VALUE : accumulated + step;
    }
}
