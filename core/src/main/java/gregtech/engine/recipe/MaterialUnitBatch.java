package gregtech.engine.recipe;

import java.math.BigInteger;

/** Exact ratios keep inventory counts and fluid millibuckets separate from material units. */
public record MaterialUnitBatch(int inputCount, long numerator, long denominator) {
    public static MaterialUnitBatch wholeOutputs(long available, long required) {
        positive(available, required);
        long count = required > available ? 1 + (required - 1) / available : 1;
        if (count > 64) return null;
        return new MaterialUnitBatch((int) count, Math.min(128, Math.max(1, available / required)), 1);
    }

    public static MaterialUnitBatch exactRatio(long available, long required, long[] quantities) {
        positive(available, required);
        BigInteger a = BigInteger.valueOf(available), r = BigInteger.valueOf(required);
        BigInteger count = BigInteger.ONE;
        for (long quantity : quantities) {
            if (quantity <= 0) continue;
            BigInteger denominator = r.divide(r.gcd(a.multiply(BigInteger.valueOf(quantity))));
            count = count.divide(count.gcd(denominator)).multiply(denominator);
            if (count.compareTo(BigInteger.valueOf(64)) > 0) return null;
        }
        BigInteger numerator = a.multiply(count), divisor = numerator.gcd(r);
        return new MaterialUnitBatch(
                count.intValueExact(),
                numerator.divide(divisor).longValueExact(),
                r.divide(divisor).longValueExact());
    }

    public long scale(long value, boolean ceil) {
        BigInteger scaled = BigInteger.valueOf(value).multiply(BigInteger.valueOf(numerator));
        BigInteger divisor = BigInteger.valueOf(denominator);
        if (ceil && scaled.signum() > 0) scaled = scaled.add(divisor).subtract(BigInteger.ONE);
        return scaled.divide(divisor).longValueExact();
    }

    private static void positive(long a, long b) {
        if (a <= 0 || b <= 0) throw new IllegalArgumentException("Material amounts must be positive");
    }
}
