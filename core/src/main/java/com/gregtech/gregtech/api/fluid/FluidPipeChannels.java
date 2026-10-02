package com.gregtech.gregtech.api.fluid;

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
