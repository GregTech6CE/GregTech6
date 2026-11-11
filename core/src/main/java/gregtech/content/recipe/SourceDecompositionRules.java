package gregtech.content.recipe;

import java.math.BigInteger;

/** Shared source quantity arithmetic. No registry lookup or per-material recipe special case. */
public final class SourceDecompositionRules {
    private SourceDecompositionRules() {}

    /** Returns -1 if the exact nonnegative result cannot be represented as long.
     * Intermediate material U amounts are long (45U > Integer.MAX_VALUE), never fluid int. */
    public static long units(long amount, long from, long to, boolean ceil) {
        if (amount < 0 || from <= 0 || to < 0) return -1;
        if (amount == 0 || to == 0) return 0;
        if (from == to) return amount; // original UT.Code.units identity branch
        BigInteger[] q = BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(to))
                .divideAndRemainder(BigInteger.valueOf(from));
        BigInteger value = ceil && q[1].signum() != 0 ? q[0].add(BigInteger.ONE) : q[0];
        return value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? -1 : value.longValueExact();
    }

    public static int fluidAmount(long sourceUnits, long sourceMillibuckets, long amount, boolean ceil) {
        if (sourceUnits <= 0 || sourceMillibuckets <= 0) return -1;
        long value = units(amount, sourceUnits, sourceMillibuckets, ceil);
        return value > Integer.MAX_VALUE ? -1 : (int) value;
    }
}
