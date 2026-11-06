package gregtech.engine.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

/** The binary output search relies on a monotone fit probe; compare it with the old linear scan. */
final class ParallelFitTest {
    private static final int MAX_PARALLEL = 16;
    private static final int CASES = 2000;

    @Test
    void itemFitIsMonotoneAndBinarySearchMatchesLinearScan() {
        var random = new Random(0x1F35EEDL);
        for (int testCase = 0; testCase < CASES; testCase++) {
            int slots = 1 + random.nextInt(6);
            int outputs = 1 + random.nextInt(4);
            int[] slotKeys = new int[slots];
            int[] slotCounts = new int[slots];
            int[] capacities = new int[outputs];
            long[] counts = new long[outputs];
            for (int output = 0; output < outputs; output++) {
                capacities[output] = 1 + random.nextInt(8);
                // Counts above the capacity cover outputs that cannot fit at any parallel.
                counts[output] = 1 + random.nextInt(capacities[output] * 2);
            }
            for (int slot = 0; slot < slots; slot++) {
                int kind = random.nextInt(4);
                if (kind == 0) {
                    slotKeys[slot] = ParallelFit.EMPTY;
                } else if (kind == 1) {
                    slotKeys[slot] = ParallelFit.NO_MATCH;
                    slotCounts[slot] = random.nextInt(65);
                } else {
                    int key = random.nextInt(outputs);
                    slotKeys[slot] = key;
                    slotCounts[slot] = random.nextInt(capacities[key] + 1);
                }
            }
            assertMonotoneAndMatchesLinear(
                    parallel ->
                            ParallelFit.itemsFit(slotKeys.clone(), slotCounts.clone(), capacities, counts, parallel),
                    testCase);
        }
    }

    @Test
    void fluidFitIsMonotoneAndBinarySearchMatchesLinearScan() {
        var random = new Random(0x2F35EEDL);
        for (int testCase = 0; testCase < CASES; testCase++) {
            int tanks = 1 + random.nextInt(6);
            int outputs = 1 + random.nextInt(4);
            int[] tankKeys = new int[tanks];
            long[] free = new long[tanks];
            long[] amounts = new long[outputs];
            for (int output = 0; output < outputs; output++) amounts[output] = 1 + random.nextInt(32);
            for (int tank = 0; tank < tanks; tank++) {
                int kind = random.nextInt(4);
                if (kind == 0) {
                    tankKeys[tank] = ParallelFit.EMPTY;
                    free[tank] = random.nextInt(33);
                } else if (kind == 1) {
                    tankKeys[tank] = ParallelFit.NO_MATCH;
                    free[tank] = random.nextInt(33);
                } else {
                    tankKeys[tank] = random.nextInt(outputs);
                    free[tank] = random.nextInt(33);
                }
            }
            assertMonotoneAndMatchesLinear(
                    parallel -> ParallelFit.fluidsFit(tankKeys.clone(), free.clone(), amounts, parallel), testCase);
        }
    }

    /** The case the empty-slot-first order used to reject: flush merges A then uses the empty slot. */
    @Test
    void outputMergesIntoAMatchingSlotBeforeUsingAnEmptyOne() {
        int[] slotKeys = {ParallelFit.EMPTY, 0};
        int[] slotCounts = {0, 63};
        int[] capacities = {64, 64};
        long[] counts = {1, 1};
        assertTrue(ParallelFit.itemsFit(slotKeys, slotCounts, capacities, counts, 1));
        // Swapping the slot order must not change the answer.
        assertTrue(ParallelFit.itemsFit(new int[] {0, ParallelFit.EMPTY}, new int[] {63, 0}, capacities, counts, 1));
        assertTrue(ParallelFit.fluidsFit(new int[] {ParallelFit.EMPTY, 0}, new long[] {64, 1}, new long[] {1, 1}, 1));
        assertTrue(ParallelFit.fluidsFit(new int[] {0, ParallelFit.EMPTY}, new long[] {1, 64}, new long[] {1, 1}, 1));
    }

    /** Planning must accept exactly what the real flush accepts, for random slot layouts. */
    @Test
    void itemPlanningMatchesTheFlushRule() {
        var random = new Random(0x3F35EEDL);
        for (int testCase = 0; testCase < CASES; testCase++) {
            int slots = 1 + random.nextInt(6);
            int outputs = 1 + random.nextInt(4);
            int[] slotKeys = new int[slots];
            int[] slotCounts = new int[slots];
            int[] capacities = new int[outputs];
            long[] counts = new long[outputs];
            for (int output = 0; output < outputs; output++) {
                capacities[output] = 1 + random.nextInt(8);
                counts[output] = 1 + random.nextInt(capacities[output] * 2);
            }
            for (int slot = 0; slot < slots; slot++) {
                int kind = random.nextInt(4);
                if (kind == 0) slotKeys[slot] = ParallelFit.EMPTY;
                else if (kind == 1) {
                    slotKeys[slot] = ParallelFit.NO_MATCH;
                    slotCounts[slot] = random.nextInt(65);
                } else {
                    int key = random.nextInt(outputs);
                    slotKeys[slot] = key;
                    slotCounts[slot] = random.nextInt(capacities[key] + 1);
                }
            }
            for (int parallel = 0; parallel <= MAX_PARALLEL; parallel++)
                assertEquals(
                        flushItems(slotKeys.clone(), slotCounts.clone(), capacities, counts, parallel),
                        ParallelFit.itemsFit(slotKeys.clone(), slotCounts.clone(), capacities, counts, parallel),
                        "case " + testCase + " parallel " + parallel);
        }
    }

    /** Mirror of MachineWorkOutputs#flush over primitive keys. */
    private static boolean flushItems(int[] keys, int[] counts, int[] capacities, long[] outputs, int parallel) {
        for (int output = 0; output < outputs.length; output++) {
            long remaining = outputs[output] * parallel;
            while (remaining > 0) {
                long pending = Math.min(remaining, capacities[output]);
                for (int pass = 0; pass < 2 && pending > 0; pass++)
                    for (int slot = 0; slot < keys.length && pending > 0; slot++) {
                        boolean empty = keys[slot] == ParallelFit.EMPTY;
                        if (empty ? pass == 0 : keys[slot] != output) continue;
                        int space = capacities[output] - (empty ? 0 : counts[slot]);
                        int take = (int) Math.min(Math.max(0, space), pending);
                        if (take == 0) continue;
                        keys[slot] = output;
                        counts[slot] = (empty ? 0 : counts[slot]) + take;
                        pending -= take;
                    }
                if (pending > 0) return false;
                remaining -= Math.min(remaining, capacities[output]);
            }
        }
        return true;
    }

    private static void assertMonotoneAndMatchesLinear(IntPredicate fits, int testCase) {
        assertTrue(fits.test(0), "case " + testCase + ": zero parallel must fit");
        boolean previous = true;
        for (int parallel = 1; parallel <= MAX_PARALLEL; parallel++) {
            boolean current = fits.test(parallel);
            if (current) {
                assertTrue(
                        previous,
                        "case " + testCase + ": parallel " + parallel + " fits but " + (parallel - 1) + " does not");
            }
            previous = current;
        }
        int linear = MAX_PARALLEL;
        while (linear > 0 && !fits.test(linear)) linear--;
        assertEquals(
                linear,
                ParallelFit.largestFitting(MAX_PARALLEL, fits),
                "case " + testCase + ": binary search differs from the linear scan");
    }
}
