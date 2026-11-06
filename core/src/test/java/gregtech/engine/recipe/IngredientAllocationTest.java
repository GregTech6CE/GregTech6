package gregtech.engine.recipe;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** The non-tagged input path must assign by availability, not by slot order. */
final class IngredientAllocationTest {
    @Test
    void broadRequirementDoesNotStrandTheOnlySlotASpecificOneAccepts() {
        int[] available = {1, 1};
        long[] demand = {1, 1};
        boolean[][] accepts = {{true, true}, {true, false}};

        var plan = IngredientAllocation.planMixed(available, demand, accepts);
        assertNotNull(plan, "a feasible assignment exists");
        assertEquals(1, plan[1][0], "the specific need keeps its only slot");
        assertEquals(1, plan[0][1], "the broad need uses the other slot");
    }

    @Test
    void feasibilityDoesNotDependOnSlotOrder() {
        int[] available = {1, 1};
        long[] demand = {1, 1};
        boolean[][] accepts = {{true, true}, {false, true}};
        assertNotNull(IngredientAllocation.planMixed(available, demand, accepts));
        assertNotNull(IngredientAllocation.planMixed(
                new int[] {1, 1}, demand, new boolean[][] {{true, false}, {true, true}}));
    }

    @Test
    void demandsAcrossSeveralSlotsAreConserved() {
        int[] available = {3, 3, 2};
        long[] demand = {4, 4};
        boolean[][] accepts = {{true, true, false}, {false, true, true}};
        var plan = IngredientAllocation.planMixed(available, demand, accepts);
        assertNotNull(plan);
        for (int slot = 0; slot < available.length; slot++) {
            int used = 0;
            for (var row : plan) used += row[slot];
            assertTrue(used <= available[slot], "slot " + slot + " over-consumed");
        }
        assertEquals(
                8,
                java.util.Arrays.stream(plan)
                        .mapToInt(row -> java.util.Arrays.stream(row).sum())
                        .sum());
    }

    @Test
    void incompleteSupplyIsRejected() {
        int[] available = {1, 0};
        long[] demand = {2};
        boolean[][] accepts = {{true, true}};
        assertNull(IngredientAllocation.planMixed(available, demand, accepts));
    }

    @Test
    void randomFeasibleCasesAreFoundRegardlessOfColumnOrder() {
        var random = new Random(0x4A11C0DEL);
        for (int testCase = 0; testCase < 500; testCase++) {
            int slots = 1 + random.nextInt(5);
            int inputs = 1 + random.nextInt(4);
            int[] available = new int[slots];
            for (int slot = 0; slot < slots; slot++) available[slot] = random.nextInt(4);
            long[] demand = new long[inputs];
            for (int input = 0; input < inputs; input++) demand[input] = random.nextInt(4);
            boolean[][] accepts = new boolean[inputs][slots];
            for (int input = 0; input < inputs; input++)
                for (int slot = 0; slot < slots; slot++) accepts[input][slot] = random.nextBoolean();

            var plan = IngredientAllocation.planMixed(available, demand, accepts);
            boolean feasible = totalWithinCapacity(available, demand, accepts);
            assertEquals(feasible, plan != null, "case " + testCase);
            if (plan != null) verifyAllocation(plan, available, demand, accepts, testCase);
        }
    }

    private static boolean totalWithinCapacity(int[] available, long[] demand, boolean[][] accepts) {
        int inputs = demand.length;
        int slots = available.length;
        int source = 0, firstInput = 1, firstSlot = firstInput + inputs, sink = firstSlot + slots;
        long[][] capacity = new long[sink + 1][sink + 1];
        for (int input = 0; input < inputs; input++) capacity[source][firstInput + input] = demand[input];
        for (int slot = 0; slot < slots; slot++) capacity[firstSlot + slot][sink] = available[slot];
        for (int input = 0; input < inputs; input++)
            for (int slot = 0; slot < slots; slot++)
                if (accepts[input][slot]) capacity[firstInput + input][firstSlot + slot] = Long.MAX_VALUE / 4;
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
        long total = 0;
        for (long need : demand) total += need;
        return flow == total;
    }

    private static void verifyAllocation(
            int[][] plan, int[] available, long[] demand, boolean[][] accepts, int testCase) {
        for (int input = 0; input < demand.length; input++) {
            long used = 0;
            for (int slot = 0; slot < available.length; slot++) {
                assertTrue(plan[input][slot] >= 0, "case " + testCase + ": negative allocation");
                if (plan[input][slot] > 0) assertTrue(accepts[input][slot], "case " + testCase + ": unaccepted slot");
                used += plan[input][slot];
            }
            assertEquals(demand[input], used, "case " + testCase + ": demand not met");
        }
        for (int slot = 0; slot < available.length; slot++) {
            int used = 0;
            for (var row : plan) used += row[slot];
            assertTrue(used <= available[slot], "case " + testCase + ": slot over-consumed");
        }
    }
}
