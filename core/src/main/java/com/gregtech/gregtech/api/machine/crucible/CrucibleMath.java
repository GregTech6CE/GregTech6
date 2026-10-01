package com.gregtech.gregtech.api.machine.crucible;

/** GT6 {@code UT.Code} helpers used by crucible thermodynamics. */
public final class CrucibleMath {
    private CrucibleMath() {}

    public static long units(long value, long divisor, long multiplier, boolean ceil) {
        if (divisor <= 0 || value <= 0 || multiplier <= 0) {
            return 0;
        }
        long product = value * multiplier;
        long result = product / divisor;
        if (ceil && product % divisor != 0) {
            result++;
        }
        return result;
    }

    public static long unitsScaled(long value, long fromAmount, long toAmount, boolean ceil) {
        return units(value, fromAmount, toAmount, ceil);
    }

    public static int scale(long value, long maxIn, int maxOut, boolean ceil) {
        if (maxIn <= 0 || value <= 0) {
            return 0;
        }
        long product = value * maxOut;
        long result = product / maxIn;
        if (ceil && product % maxIn != 0) {
            result++;
        }
        return (int) Math.min(maxOut, result);
    }
}
