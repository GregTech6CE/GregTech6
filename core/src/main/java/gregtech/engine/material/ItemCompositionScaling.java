package gregtech.engine.material;

import java.math.BigInteger;

/** Exact floor scaling for positive component amounts on a damaged item stack. */
public final class ItemCompositionScaling {
    private ItemCompositionScaling() {}

    public static long scale(long amount, int remaining, int maximum) {
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        if (remaining < 0) throw new IllegalArgumentException("remaining must not be negative");
        if (maximum <= 0) throw new IllegalArgumentException("maximum must be positive");
        if (remaining > maximum) throw new IllegalArgumentException("remaining must not exceed maximum");
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(remaining))
                .divide(BigInteger.valueOf(maximum))
                .longValueExact();
    }
}
