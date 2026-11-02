package gregtech.content.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.transport.WeightedItemPipeRoutes.Exit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Nearest-first delivery over cached routes, with per-pipe stack bandwidth. */
final class ItemPipeNetworkTransferTest {
    /** A hand-built network: pipes with steps and allowances, exits with room for a number of stacks. */
    private static final class Net implements WeightedItemPipeRoutes.Graph {
        final List<Long> steps = new ArrayList<>();
        final List<Long> allowances = new ArrayList<>();
        final List<List<Integer>> links = new ArrayList<>();
        final List<List<Integer>> exitsOf = new ArrayList<>();
        final List<Integer> room = new ArrayList<>();
        final List<Integer> received = new ArrayList<>();
        final List<Long> transferred = new ArrayList<>();
        final Set<Long> closed = new HashSet<>();
        int stacks;

        int pipe(long step, long allowance) {
            steps.add(step);
            allowances.add(allowance);
            links.add(new ArrayList<>());
            exitsOf.add(new ArrayList<>());
            transferred.add(0L);
            return steps.size() - 1;
        }

        int pipe() {
            return pipe(1, 64);
        }

        int got(int exit) {
            return received.get(exit);
        }

        long used(int pipe) {
            return transferred.get(pipe);
        }

        int exit(int pipe, int stacksRoom) {
            room.add(stacksRoom);
            received.add(0);
            exitsOf.get(pipe).add(room.size() - 1);
            return room.size() - 1;
        }

        void chain(int... pipes) {
            for (int i = 0; i + 1 < pipes.length; i++) {
                links.get(pipes[i]).add(pipes[i + 1]);
                links.get(pipes[i + 1]).add(pipes[i]);
            }
        }

        @Override
        public int pipes() {
            return steps.size();
        }

        @Override
        public long stepSize(int pipe) {
            return steps.get(pipe);
        }

        @Override
        public int[] links(int pipe) {
            return links.get(pipe).stream().mapToInt(Integer::intValue).toArray();
        }

        @Override
        public int[] exits(int pipe) {
            return exitsOf.get(pipe).stream().mapToInt(Integer::intValue).toArray();
        }

        int deliver(int source) {
            var routes = WeightedItemPipeRoutes.scan(this, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
            return deliver(routes);
        }

        int deliver(WeightedItemPipeRoutes.Routes routes) {
            return ItemPipeNetworkTransfer.deliver(routes, new ItemPipeNetworkTransfer.Channel() {
                @Override
                public boolean hasItems() {
                    return stacks > 0;
                }

                @Override
                public boolean hasCapacity(int pipe) {
                    return ItemPipeNetworkTransfer.hasCapacity(transferred.get(pipe), allowances.get(pipe));
                }

                @Override
                public void charge(int pipe) {
                    transferred.set(pipe, transferred.get(pipe) + 1);
                }

                @Override
                public boolean linkOpen(int from, int to) {
                    return !closed.contains(((long) from << 32) | to);
                }

                @Override
                public boolean send(Exit exit) {
                    if (room.get(exit.exit()) <= 0) return false;
                    room.set(exit.exit(), room.get(exit.exit()) - 1);
                    received.set(exit.exit(), received.get(exit.exit()) + 1);
                    stacks--;
                    return true;
                }
            });
        }
    }

    @Test
    void nearestAcceptingExitTakesTheItemsDirectly() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe(), nearPipe = net.pipe(), c = net.pipe(), d = net.pipe();
        int farPipe = net.pipe();
        net.chain(source, a, nearPipe);
        net.chain(source, c, d, farPipe);
        int near = net.exit(nearPipe, 64), far = net.exit(farPipe, 64);
        net.stacks = 3;

        assertEquals(3, net.deliver(source));
        assertEquals(3, net.got(near));
        assertEquals(0, net.got(far));
    }

    @Test
    void exitBesideTheSourceComesFirst() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe();
        net.chain(source, a);
        int further = net.exit(a, 64), beside = net.exit(source, 64);
        net.stacks = 2;

        net.deliver(source);
        assertEquals(2, net.got(beside));
        assertEquals(0, net.got(further));
    }

    @Test
    void restrictivePipeIsAvoidedWhileACheaperRouteExists() {
        Net net = new Net();
        int source = net.pipe(), restrictive = net.pipe(100, 64), behind = net.pipe();
        int a = net.pipe(), b = net.pipe(), open = net.pipe();
        net.chain(source, restrictive, behind);
        net.chain(source, a, b, open);
        int blocked = net.exit(behind, 64), taken = net.exit(open, 64);
        net.stacks = 1;

        net.deliver(source);
        assertEquals(1, net.got(taken));
        assertEquals(0, net.got(blocked));
    }

    @Test
    void sourceAllowanceBoundsStacksPerWindow() {
        Net net = new Net();
        int source = net.pipe(1, 2), exitPipe = net.pipe();
        net.chain(source, exitPipe);
        net.exit(exitPipe, 64);
        net.stacks = 5;

        assertEquals(2, net.deliver(source));
        assertEquals(3, net.stacks);
        assertEquals(0, net.deliver(source), "a spent allowance blocks the source until its window resets");
        net.transferred.set(source, 0L);
        assertEquals(2, net.deliver(source));
    }

    @Test
    void saturatedPipeOnThePathSkipsToTheNextExit() {
        Net net = new Net();
        int source = net.pipe(), thin = net.pipe(1, 1), nearPipe = net.pipe();
        int b = net.pipe(), c = net.pipe(), farPipe = net.pipe();
        net.chain(source, thin, nearPipe);
        net.chain(source, b, c, farPipe);
        int near = net.exit(nearPipe, 64), far = net.exit(farPipe, 64);
        net.stacks = 3;

        assertEquals(3, net.deliver(source));
        assertEquals(1, net.got(near));
        assertEquals(2, net.got(far));
    }

    @Test
    void onlyPipesOnThePathPayBandwidth() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe(), exitPipe = net.pipe(), branch = net.pipe();
        net.chain(source, a, exitPipe);
        net.chain(source, branch);
        net.exit(exitPipe, 64);
        net.stacks = 2;

        net.deliver(source);
        assertEquals(2L, net.used(source));
        assertEquals(2L, net.used(a));
        assertEquals(2L, net.used(exitPipe));
        assertEquals(0L, net.used(branch));
    }

    @Test
    void fullExitFallsThroughToTheNextOne() {
        Net net = new Net();
        int source = net.pipe(), nearPipe = net.pipe(), farPipe = net.pipe();
        net.chain(source, nearPipe, farPipe);
        int near = net.exit(nearPipe, 1), far = net.exit(farPipe, 64);
        net.stacks = 3;

        assertEquals(3, net.deliver(source));
        assertEquals(1, net.got(near));
        assertEquals(2, net.got(far));
    }

    @Test
    void closedLinkSkipsTheExitsBehindIt() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe(), nearPipe = net.pipe(), farPipe = net.pipe(), b = net.pipe();
        net.chain(source, a, nearPipe);
        net.chain(source, b, a, farPipe);
        int near = net.exit(nearPipe, 64);
        net.closed.add(((long) a << 32) | nearPipe);
        net.stacks = 1;

        assertEquals(0, net.deliver(source));
        assertEquals(0, net.got(near));
        assertEquals(1, net.stacks);
    }

    @Test
    void nothingMovesWithoutAnAcceptingExit() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe();
        net.chain(source, a);
        net.exit(a, 0);
        net.stacks = 1;

        assertEquals(0, net.deliver(source));
        assertEquals(1, net.stacks);
        assertEquals(0L, net.used(source));
    }

    @Test
    void scanListsExitsNearestFirstWithTheirPathTree() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe(), heavy = net.pipe(10, 64), b = net.pipe();
        net.chain(source, heavy);
        net.chain(source, a, b);
        int viaHeavy = net.exit(heavy, 1), viaB = net.exit(b, 1), atA = net.exit(a, 1);

        var routes = WeightedItemPipeRoutes.scan(net, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        assertEquals(
                List.of(atA, viaB, viaHeavy),
                routes.exits().stream().map(Exit::exit).toList());
        assertEquals(
                List.of(2L, 3L, 11L), routes.exits().stream().map(Exit::cost).toList());
        assertEquals(a, routes.previous()[b]);
        assertEquals(source, routes.previous()[a]);
        assertEquals(-1, routes.previous()[source]);
    }

    @Test
    void scanStopsAtTheVisitLimit() {
        Net net = new Net();
        int[] chain = new int[10];
        for (int i = 0; i < chain.length; i++) chain[i] = net.pipe();
        net.chain(chain);
        int last = net.exit(chain[9], 1), early = net.exit(chain[2], 1);

        var routes = WeightedItemPipeRoutes.scan(net, chain[0], 4);
        assertEquals(List.of(early), routes.exits().stream().map(Exit::exit).toList());
        assertTrue(routes.exits().stream().noneMatch(exit -> exit.exit() == last));
    }

    @Test
    void scanRetainsExitsAtAndBeyondTheMaximumCost() {
        Net net = new Net();
        int source = net.pipe(Long.MAX_VALUE - 1, 64), exact = net.pipe(), overflow = net.pipe();
        net.chain(source, exact, overflow);
        int exactExit = net.exit(exact, 1), overflowExit = net.exit(overflow, 1);

        var routes = WeightedItemPipeRoutes.scan(net, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        assertEquals(
                List.of(exactExit, overflowExit),
                routes.exits().stream().map(Exit::exit).toList());
        assertEquals(
                List.of(Long.MAX_VALUE, Long.MAX_VALUE),
                routes.exits().stream().map(Exit::cost).toList());
        assertEquals(source, routes.previous()[exact]);
        assertEquals(exact, routes.previous()[overflow]);
        net.stacks = 2;
        assertEquals(2, net.deliver(source));
    }

    @Test
    void maximumCostSourceStillReachesItsNeighbors() {
        Net net = new Net();
        int source = net.pipe(Long.MAX_VALUE, 64), other = net.pipe(0, 64);
        net.chain(source, other);
        int exit = net.exit(other, 1);
        net.stacks = 1;

        assertEquals(1, net.deliver(source));
        assertEquals(1, net.got(exit));
    }

    @Test
    void zeroCostCycleKeepsAnAcyclicPathAndChargesEachPipeOnce() {
        Net net = new Net();
        int source = net.pipe(0, 64), a = net.pipe(0, 64), b = net.pipe(0, 64);
        net.chain(source, a, b, source);
        int exit = net.exit(b, 1);
        net.stacks = 1;

        var routes = WeightedItemPipeRoutes.scan(net, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        assertEquals(-1, routes.previous()[source]);
        assertEquals(source, routes.previous()[a]);
        assertEquals(source, routes.previous()[b]);
        assertEquals(List.of(exit), routes.exits().stream().map(Exit::exit).toList());
        assertEquals(1, net.deliver(source));
        assertEquals(1L, net.used(source));
        assertEquals(0L, net.used(a));
        assertEquals(1L, net.used(b));
    }

    @Test
    void directedLinksDoNotProvideAReverseRoute() {
        Net net = new Net();
        int source = net.pipe(), target = net.pipe();
        net.links.get(source).add(target);
        int exit = net.exit(target, 1);

        var forward = WeightedItemPipeRoutes.scan(net, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        assertEquals(List.of(exit), forward.exits().stream().map(Exit::exit).toList());
        int reverseExit = net.exit(source, 1);
        var reverse = WeightedItemPipeRoutes.scan(net, target, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        assertTrue(reverse.exits().stream().noneMatch(route -> route.exit() == reverseExit));
        assertEquals(-1, reverse.previous()[source]);
    }

    @Test
    void equalCostExitsRetainLinkAndFaceOrder() {
        Net net = new Net();
        int source = net.pipe(), a = net.pipe(), b = net.pipe();
        net.chain(source, b);
        net.chain(source, a);
        int atA = net.exit(a, 1), firstAtB = net.exit(b, 1), secondAtB = net.exit(b, 1);

        var routes = WeightedItemPipeRoutes.scan(net, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        assertEquals(
                List.of(firstAtB, secondAtB, atA),
                routes.exits().stream().map(Exit::exit).toList());
    }

    @Test
    void cachedRoutesObserveLinkChangesAndRestoredBandwidth() {
        Net net = new Net();
        int source = net.pipe(), bottleneck = net.pipe(1, 1), target = net.pipe();
        net.chain(source, bottleneck, target);
        int exit = net.exit(target, 3);
        var routes = WeightedItemPipeRoutes.scan(net, source, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
        long link = ((long) source << 32) | bottleneck;
        net.stacks = 3;
        net.closed.add(link);

        assertEquals(0, net.deliver(routes));
        assertEquals(0L, net.used(source));
        net.closed.remove(link);
        assertEquals(1, net.deliver(routes));
        assertEquals(0, net.deliver(routes));
        net.transferred.set(bottleneck, 0L);
        assertEquals(1, net.deliver(routes));
        assertEquals(2, net.got(exit));
        assertEquals(1, net.stacks);
    }
}
