package gregtech.engine.fluid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FluidPipeBalanceTest {
    private record Move(int source, int target, long requested) {}

    @Test
    void cycleUsesTraversalOrderAndEachComponentKeepsItsOwnFluid() {
        var neighbors = List.of(
                List.of(1, 2), List.of(0, 3), List.of(0, 3), List.of(1, 2), List.of(5), List.of(4), List.<Integer>of());
        long[] capacities = {30, 30, 30, 30, 30, 30, 30};
        long[] amounts = {22, 0, 0, 0, 3, 11, 29};
        var moves = new ArrayList<Move>();

        FluidPipeBalance.distribute(neighbors, capacities, node -> amounts[node], (from, to, requested) -> {
            moves.add(new Move(from, to, requested));
            amounts[from] -= requested;
            amounts[to] += requested;
        });

        assertEquals(List.of(new Move(0, 2, 11), new Move(2, 3, 5), new Move(0, 1, 5), new Move(5, 4, 4)), moves);
        assertArrayEquals(new long[] {6, 5, 6, 5, 7, 7, 29}, amounts);
    }

    @Test
    void partialAcceptanceChangesWhatTheParentCanForward() {
        var neighbors = List.of(List.of(1), List.of(0, 2), List.of(1));
        long[] amounts = {0, 8, 22};
        var moves = new ArrayList<Move>();

        FluidPipeBalance.distribute(
                neighbors, new long[] {30, 30, 30}, node -> amounts[node], (from, to, requested) -> {
                    moves.add(new Move(from, to, requested));
                    long accepted = Math.min(requested, 4);
                    amounts[from] -= accepted;
                    amounts[to] += accepted;
                });

        assertEquals(List.of(new Move(2, 1, 12), new Move(1, 0, 2)), moves);
        assertArrayEquals(new long[] {2, 10, 18}, amounts);
    }

    @Test
    void singleUnitSurplusAndDeficitAreLeftInPlace() {
        var neighbors = List.of(List.of(1), List.of(0), List.of(3), List.of(2));
        long[] amounts = {0, 2, 2, 0};
        var moves = new ArrayList<Move>();

        FluidPipeBalance.distribute(
                neighbors,
                new long[] {4, 4, 4, 4},
                node -> amounts[node],
                (from, to, requested) -> moves.add(new Move(from, to, requested)));

        assertEquals(List.of(), moves);
    }
}
