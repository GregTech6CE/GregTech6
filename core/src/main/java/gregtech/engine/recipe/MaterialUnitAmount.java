package gregtech.engine.recipe;

import gregtech.api.material.GTValues;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact fractions for material recipe amounts, independent of item stack counts. */
public final class MaterialUnitAmount {
    private MaterialUnitAmount() {}

    public static String format(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Material amount must be positive");
        long divisor = gcd(amount, GTValues.U);
        long numerator = amount / divisor;
        long denominator = GTValues.U / divisor;
        return denominator == 1 ? numerator + "U" : numerator + "/" + denominator + "U";
    }

    public static String decimal(long amount) {
        return BigDecimal.valueOf(amount)
                .divide(BigDecimal.valueOf(GTValues.U), 6, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static long parse(String text) {
        String value = text.strip();
        if (value.endsWith("U") || value.endsWith("u")) value = value.substring(0, value.length() - 1);
        String[] parts = value.split("/", -1);
        if (parts.length > 2) throw new IllegalArgumentException("Material fraction");
        BigDecimal numerator = new BigDecimal(parts[0]);
        BigDecimal denominator = parts.length == 1 ? BigDecimal.ONE : new BigDecimal(parts[1]);
        if (numerator.signum() <= 0 || denominator.signum() <= 0) throw new IllegalArgumentException("Material amount");
        return numerator
                .multiply(BigDecimal.valueOf(GTValues.U))
                .divide(denominator, 0, RoundingMode.UNNECESSARY)
                .longValueExact();
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
