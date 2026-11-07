package gregtech.engine.recipe.chem;

import gregtech.api.material.GTValues;
import java.math.BigInteger;

/**
 * An exact material amount in GT material units. The value is never a stack count and never a
 * millibucket: conversions to those happen only at a reviewed native boundary.
 */
public final class MaterialAmount {
    private final long units;

    private MaterialAmount(long units) {
        if (units <= 0) throw new IllegalArgumentException("Material amount must be positive");
        this.units = units;
    }

    public static MaterialAmount units(long units) {
        return new MaterialAmount(units);
    }

    /**
     * The source expression {@code numerator * U / denominator}. The result must be integral;
     * a non-integral value would have to be rounded and is therefore rejected instead of being
     * silently truncated the way thousandth tokens were.
     */
    public static MaterialAmount fraction(long numerator, long denominator) {
        if (numerator <= 0 || denominator <= 0) throw new IllegalArgumentException("Material fraction");
        BigInteger[] ratio = BigInteger.valueOf(numerator)
                .multiply(BigInteger.valueOf(GTValues.U))
                .divideAndRemainder(BigInteger.valueOf(denominator));
        if (ratio[1].signum() != 0) {
            throw new IllegalArgumentException("Non-integral material amount " + numerator + "/" + denominator + " U");
        }
        return new MaterialAmount(ratio[0].longValueExact());
    }

    public static MaterialAmount whole(int units) {
        return new MaterialAmount(Math.multiplyExact((long) units, GTValues.U));
    }

    public long units() {
        return units;
    }

    public MaterialAmount multiply(long factor) {
        return new MaterialAmount(Math.multiplyExact(units, factor));
    }

    public MaterialAmount add(MaterialAmount other) {
        return new MaterialAmount(Math.addExact(units, other.units));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MaterialAmount amount && amount.units == units;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(units);
    }

    @Override
    public String toString() {
        long divisor = gcd(units, GTValues.U);
        long numerator = units / divisor;
        long denominator = GTValues.U / divisor;
        return denominator == 1 ? numerator + "U" : numerator + "/" + denominator + "U";
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long next = a % b;
            a = b;
            b = next;
        }
        return a;
    }
}
