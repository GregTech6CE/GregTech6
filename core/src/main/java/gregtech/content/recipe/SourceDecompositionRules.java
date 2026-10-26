package gregtech.content.recipe;

import java.math.BigInteger;

/** Shared source quantity/phase policy. No registry lookup or per-material recipe special case. */
public final class SourceDecompositionRules {
    public static final long SOURCE_U = 648648000L;
    public record Binding(String registry, long sourceUnits, long millibuckets) {}
    public record Profile(int id, String name, Binding liquid, Binding gas, boolean keepDust) {}
    public record Thermal(int id, String name, long melting, long boiling, long plasma,
                          Binding liquid, Binding gas, Binding plasmaFluid) {}
    private SourceDecompositionRules() {}

    /** Returns -1 if the exact nonnegative result cannot be represented as long.
     * Intermediate material U amounts are long (45U > Integer.MAX_VALUE), never fluid int. */
    public static long units(long amount, long from, long to, boolean ceil) {
        if (amount < 0 || from <= 0 || to < 0) return -1;
        if (amount == 0 || to == 0) return 0;
        if (from == to) return amount; // original UT.Code.units identity branch
        BigInteger[] q = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(to))
                .divideAndRemainder(BigInteger.valueOf(from));
        BigInteger value = ceil && q[1].signum() != 0 ? q[0].add(BigInteger.ONE) : q[0];
        return value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? -1 : value.longValueExact();
    }
    public static int fluidAmount(Binding binding, long amount, boolean ceil) {
        if (binding == null) return -1;
        long value = units(amount, binding.sourceUnits(), binding.millibuckets(), ceil);
        return value > Integer.MAX_VALUE ? -1 : (int) value;
    }
    /** Selected absent phase stays absent; never try a lower-temperature phase as fallback. */
    public static Binding ambient(Thermal material, long temperature) {
        if (material == null) return null;
        if (temperature >= material.plasma()) return material.plasmaFluid();
        if (temperature >= material.boiling()) return material.gas();
        if (temperature >= material.melting()) return material.liquid();
        return null;
    }
    public static boolean solidFallback(boolean liquidAvailable, boolean gasAvailable, boolean keepDust) {
        return keepDust || !liquidAvailable && !gasAvailable;
    }
}
