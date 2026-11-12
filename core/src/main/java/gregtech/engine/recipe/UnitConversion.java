package gregtech.engine.recipe;

import java.math.BigInteger;

/** Exact integer unit conversion shared by recipes and content projections. */
public final class UnitConversion {
    private UnitConversion() {}

    /**
     * Returns -1 if the exact nonnegative result cannot be represented as a long.
     *
     * <p>Intermediate material amounts stay as {@code long}; truncating to an {@code int} is a
     * separate caller decision.</p>
     */
    public static long units(long amount, long from, long to, boolean ceil) {
        if (amount < 0 || from <= 0 || to < 0) return -1;
        if (amount == 0 || to == 0) return 0;
        if (from == to) return amount;
        BigInteger[] quotient = BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(to))
                .divideAndRemainder(BigInteger.valueOf(from));
        BigInteger value = ceil && quotient[1].signum() != 0 ? quotient[0].add(BigInteger.ONE) : quotient[0];
        return value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? -1 : value.longValueExact();
    }

    public static int fluidAmount(long sourceUnits, long sourceMillibuckets, long amount, boolean ceil) {
        if (sourceUnits <= 0 || sourceMillibuckets <= 0) return -1;
        long value = units(amount, sourceUnits, sourceMillibuckets, ceil);
        return value > Integer.MAX_VALUE ? -1 : (int) value;
    }
}
