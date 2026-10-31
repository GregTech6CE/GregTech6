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
