package gregtech.engine.fluid;

import java.util.List;
import java.util.function.IntPredicate;

/** GT6 pipe channels belong to fluids, not faces or neighbouring pipe slot numbers. */
public final class FluidPipeChannels {
    private FluidPipeChannels() {}

    /** Original MultiTileEntityPipeFluid.getFluidTankFillable2: matching before empty. */
    public static int select(int count, IntPredicate matches, IntPredicate empty) {
        int firstEmpty = -1;
        for (int i = 0; i < count; i++) {
            if (matches.test(i)) return i;
            if (firstEmpty < 0 && empty.test(i)) firstEmpty = i;
        }
        return firstEmpty;
    }

    /** Original distribute: count the source and accepting machines, whose stored amounts are excluded. */
    public static long distributionLevel(long source, long[] pipeAmounts, int machines) {
        if (source < 0 || machines < 0) throw new IllegalArgumentException("negative fluid amount or target count");
        long count = 1L + pipeAmounts.length + machines;
        long quotient = source / count;
        long remainder = source % count;
        for (long amount : pipeAmounts) {
            if (amount < 0) throw new IllegalArgumentException("negative fluid amount");
            // Divide before summing: even several Long.MAX_VALUE tanks cannot overflow.
            quotient += amount / count;
            remainder += amount % count;
        }
        return quotient + remainder / count + (remainder % count == 0 ? 0 : 1);
    }

    /** A second push relieves pressure above half the source channel's capacity. */
    public static long pressureShare(long remaining, long capacity, int pipes) {
        return pipes <= 0 || remaining <= capacity / 2 ? 0 : (remaining - capacity / 2) / pipes;
    }

    /** Levels the supplied total within channel capacities, giving leftover units out in traversal order. */
    public static long[] balancedTargets(List<Integer> order, long[] capacity, long total) {
        long high = 0;
        for (int node : order) high = Math.max(high, capacity[node]);
        long low = 0;
        while (low < high) {
            long middle = low + (high - low) / 2 + ((high - low) & 1);
            if (fillsAtMost(order, capacity, middle, total)) low = middle;
            else high = middle - 1;
        }
        long[] target = new long[capacity.length];
        long remaining = total;
        for (int node : order) {
            target[node] = Math.min(low, capacity[node]);
            remaining -= target[node];
        }
        for (int node : order) {
            if (remaining <= 0) break;
            if (capacity[node] > low) {
                target[node]++;
                remaining--;
            }
        }
        return target;
    }

    private static boolean fillsAtMost(List<Integer> order, long[] capacity, long level, long total) {
        long sum = 0;
        for (int node : order) {
            long share = Math.min(level, capacity[node]);
            if (share > total - sum) return false;
            sum += share;
        }
        return true;
    }

    /** Cauldron increments cost ceil(increments * 1000 / 3), paid in one operation. */
    public static int cauldronCost(int currentLevel, long available) {
        if (currentLevel < 0 || currentLevel >= 3) return 0;
        for (int increments = 3 - currentLevel; increments > 0; increments--) {
            int cost = (increments * 1000 + 2) / 3;
            if (available >= cost) return cost;
        }
        return 0;
    }
}
