package gregtech.content.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.transport.WeightedItemPipeRoutes.Exit;
import gregtech.content.transport.WeightedItemPipeRoutes.Search;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WeightedItemPipeRoutesTest {
    private record Graph(long[] steps, int[][] links, int[][] exits) implements WeightedItemPipeRoutes.Graph {
        @Override
        public int pipes() {
            return steps.length;
        }

        @Override
        public long stepSize(int pipe) {
            return steps[pipe];
        }

        @Override
        public int[] links(int pipe) {
            return links[pipe];
        }

        @Override
        public int[] exits(int pipe) {
            return exits[pipe];
        }
    }

    @Test
    void nearbyExitStopsExplorationAndLaterDeliveryResumes() {
        Graph graph = line(1000);
        graph.exits[1] = new int[] {0};
        graph.exits[999] = new int[] {1};
        Search search = new Search(graph, 0, 1000);
        Channel channel = new Channel(graph, 1, 64);
        channel.stacks = 1;
        assertEquals(1, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(1, channel.received[0]);
        assertEquals(2, search.explored());

        channel.stacks = 1;
        assertEquals(1, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(1, channel.received[1]);
        assertEquals(1000, search.explored());
    }

    @Test
    void saturatedBranchSkipsToNextExitAndOnlyChargesItsPath() {
        Graph graph =
                new Graph(new long[] {1, 1, 1, 1, 1, 1}, new int[][] {{1, 3}, {2}, {}, {4}, {5}, {}}, new int[][] {
                    {}, {}, {0}, {}, {}, {1}
                });
        Channel channel = new Channel(graph, 64, 64);
        channel.allowance[1] = 1;
        channel.stacks = 3;
        Search search = new Search(graph, 0, 100);
        assertEquals(3, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(1, channel.received[0]);
        assertEquals(2, channel.received[1]);
        assertTrue(Arrays.equals(new long[] {3, 1, 1, 2, 2, 2}, channel.transferred));

        Arrays.fill(channel.transferred, 0);
        channel.stacks = 1;
        channel.closedFrom = 1;
        channel.closedTo = 2;
        assertEquals(1, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(1, channel.received[0]);
        assertEquals(3, channel.received[1]);
        assertEquals(0, channel.transferred[1]);
        channel.closedFrom = -1;
        channel.stacks = 1;
        assertEquals(1, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(2, channel.received[0]);
    }

    @Test
    void sourceBandwidthStopsSearchUntilNextWindow() {
        Graph graph = line(10);
        graph.exits[9] = new int[] {0};
        Search search = new Search(graph, 0, 10);
        Channel channel = new Channel(graph, 64);
        channel.stacks = 5;
        channel.allowance[0] = 2;
        channel.transferred[0] = 2;
        assertEquals(0, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(1, search.explored());
        channel.transferred[0] = 0;
        assertEquals(2, ItemPipeNetworkTransfer.deliver(search, channel));
        assertEquals(0, ItemPipeNetworkTransfer.deliver(search, channel));
        channel.transferred[0] = 0;
        assertEquals(2, ItemPipeNetworkTransfer.deliver(search, channel));
    }

    @Test
    void visitLimitAndInvalidSourcesDoNotReadOutsideTheSearch() {
        Graph graph = line(10);
        graph.exits[0] = new int[] {0};
        graph.exits[3] = new int[] {1};
        graph.exits[4] = new int[] {2};
        Search search = new Search(graph, 0, 4);
        assertTrue(search.hasExit(1));
        assertEquals(1, search.exit(1));
        assertFalse(search.hasExit(2));
        assertEquals(4, search.explored());
        assertFalse(search.hasExit(100));
        assertFalse(new Search(graph, -1, 10).hasExit(0));
        assertFalse(new Search(graph, 10, 10).hasExit(0));
        assertFalse(new Search(graph, 0, 0).hasExit(0));
        assertThrows(IndexOutOfBoundsException.class, () -> search.hasExit(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> search.exit(2));
        assertThrows(IndexOutOfBoundsException.class, () -> search.exitPipe(2));
        assertThrows(IndexOutOfBoundsException.class, () -> search.exitCost(2));
        graph.steps[0] = -1;
        assertThrows(IllegalArgumentException.class, () -> new Search(graph, 0, 10));
    }

    @Test
    void retainedBudgetIncludesInitialCapacityFrontierAndExits() {
        Graph graph = line(512);
        graph.exits[0] = new int[] {0};
        graph.links[0] = new int[511];
        for (int i = 0; i < 511; i++) graph.links[0][i] = i + 1;
        Search search = new Search(graph, 0, 512);
        long initial = search.retainedBytes();
        assertTrue(initial > 0);
        assertEquals(new Search(line(1), 0, 1).retainedBytes(), initial);
        assertTrue(search.hasExit(0));
        assertEquals(initial, search.retainedBytes());
        assertFalse(search.hasExit(1));
        assertTrue(search.retainedBytes() > initial + 511L * 20);

        Graph manyExits = line(1);
        manyExits.exits[0] = new int[1000];
        Search exits = new Search(manyExits, 0, 1);
        assertTrue(exits.hasExit(999));
        assertEquals(1, exits.explored());
        assertTrue(exits.retainedBytes() >= initial + (1000 - 4) * 16L);
    }

    @Test
    void randomDirectedGraphsMatchStableReferenceIncludingZeroAndSaturatedCosts() {
        Random random = new Random(0x51DEC0DEL);
        for (int round = 0; round < 300; round++) {
            int size = 2 + random.nextInt(100);
            Graph graph = line(size);
            for (int pipe = 0; pipe < size; pipe++) {
                graph.steps[pipe] = switch (random.nextInt(10)) {
                    case 0 -> Long.MAX_VALUE;
                    case 1 -> 100;
                    default -> random.nextInt(5);
                };
                graph.links[pipe] = new int[random.nextInt(7)];
                for (int i = 0; i < graph.links[pipe].length; i++) graph.links[pipe][i] = random.nextInt(size);
                graph.exits[pipe] = random.nextBoolean() ? new int[] {pipe * 2, pipe * 2 + 1} : new int[0];
            }
            int source = random.nextInt(size);
            int limit = random.nextBoolean() ? size : random.nextInt(size + 1);
            List<Exit> expected = reference(graph, source, limit);
            Search search = new Search(graph, source, limit);
            for (int i = 0; i < expected.size(); i++) {
                Exit exit = expected.get(i);
                assertTrue(search.hasExit(i), "missing exit in round " + round);
                assertEquals(exit.exit(), search.exit(i));
                assertEquals(exit.pipe(), search.exitPipe(i));
                assertEquals(exit.cost(), search.exitCost(i));
                long cost = 0;
                int length = 0;
                int last = -1;
                for (int at = exit.pipe(); at >= 0; at = search.previous(at)) {
                    assertTrue(++length <= size, "predecessor cycle");
                    cost = saturatedAdd(cost, graph.steps[at]);
                    last = at;
                }
                assertEquals(source, last);
                assertEquals(exit.cost(), cost);
            }
            assertFalse(search.hasExit(expected.size()));
            assertEquals(
                    expected, WeightedItemPipeRoutes.scan(graph, source, limit).exits());
        }
    }

    private static Graph line(int size) {
        long[] steps = new long[size];
        Arrays.fill(steps, 1);
        int[][] links = new int[size][];
        int[][] exits = new int[size][];
        for (int i = 0; i < size; i++) {
            links[i] = i + 1 < size ? new int[] {i + 1} : new int[0];
            exits[i] = new int[0];
        }
        return new Graph(steps, links, exits);
    }

    private record Entry(int pipe, long cost, int order) {}

    private static List<Exit> reference(Graph graph, int source, int limit) {
        long[] costs = new long[graph.pipes()];
        boolean[] discovered = new boolean[graph.pipes()];
        boolean[] settled = new boolean[graph.pipes()];
        PriorityQueue<Entry> frontier =
                new PriorityQueue<>(Comparator.comparingLong(Entry::cost).thenComparingInt(Entry::order));
        int order = 0;
        costs[source] = graph.steps[source];
        discovered[source] = true;
        frontier.add(new Entry(source, costs[source], order++));
        List<Exit> exits = new ArrayList<>();
        int visited = 0;
        while (visited < limit && !frontier.isEmpty()) {
            Entry entry = frontier.remove();
            int pipe = entry.pipe;
            if (settled[pipe] || entry.cost != costs[pipe]) continue;
            settled[pipe] = true;
            visited++;
            for (int exit : graph.exits[pipe]) exits.add(new Exit(exit, pipe, entry.cost));
            for (int next : graph.links[pipe]) {
                if (settled[next]) continue;
                long cost = saturatedAdd(entry.cost, graph.steps[next]);
                if (!discovered[next] || cost < costs[next]) {
                    discovered[next] = true;
                    costs[next] = cost;
                    frontier.add(new Entry(next, cost, order++));
                }
            }
        }
        return exits;
    }

    private static long saturatedAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    private static final class Channel implements ItemPipeNetworkTransfer.Channel {
        private final long[] allowance;
        private final long[] transferred;
        private final int[] room;
        private final int[] received;
        private int stacks;
        private int closedFrom = -1;
        private int closedTo = -1;

        Channel(Graph graph, int... room) {
            this.room = room;
            received = new int[room.length];
            allowance = new long[graph.pipes()];
            Arrays.fill(allowance, 64);
            transferred = new long[graph.pipes()];
        }

        @Override
        public boolean hasItems() {
            return stacks > 0;
        }

        @Override
        public boolean hasCapacity(int pipe) {
            return ItemPipeNetworkTransfer.hasCapacity(transferred[pipe], allowance[pipe]);
        }

        @Override
        public void charge(int pipe) {
            transferred[pipe]++;
        }

        @Override
        public boolean linkOpen(int from, int to) {
            return from != closedFrom || to != closedTo;
        }

        @Override
        public boolean send(int exit) {
            if (room[exit] <= 0) return false;
            room[exit]--;
            received[exit]++;
            stacks--;
            return true;
        }
    }
}
