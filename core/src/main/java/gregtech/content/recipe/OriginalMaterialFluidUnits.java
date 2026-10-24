package gregtech.content.recipe;

import java.math.BigInteger;

/**
 * Explicit source phase bindings for the eight audited Alumina chemistry rows only.
 * Original CS.U=648648000; FL.createMolten uses144mB/U, createGas1000mB/U.
 * Physical temperature, texture, and current legacy fluid names do not determine this dimension.
 */
public final class OriginalMaterialFluidUnits {
    public static final long SOURCE_UNIT = 648648000L;
    private static final BigInteger MAX_AMOUNT = BigInteger.valueOf(Integer.MAX_VALUE);

    public record Binding(String materialName, int materialId, String phase, String fluidField,
                          String registryName, long referenceSourceUnits, long referenceMillibuckets) {}

    private static final Binding[] BINDINGS = {
            new Binding("Cryolite", 8009, "liquid", "GenLiquid_Cryolite", "cryolite", SOURCE_UNIT, 144),
            new Binding("AluminiumFluoride", 8010, "liquid", "GenLiquid_AluminiumFluoride", "aluminiumfluoride", SOURCE_UNIT, 144),
            new Binding("CarbonDioxide", 9836, "gas", "CarbonDioxide", "carbondioxide", SOURCE_UNIT, 1000),
            new Binding("Fluorine", 90, "gas", "GenGas_Fluorine", "fluorine", SOURCE_UNIT, 1000)
    };

    private OriginalMaterialFluidUnits() {}

    /** Unknown or conflicting source identities are deliberately unsupported, not guessed. */
    public static Binding binding(String canonicalName, int materialId, String phase) {
        for (Binding binding : BINDINGS) {
            if (binding.materialName().equals(canonicalName) && binding.materialId() == materialId
                    && binding.phase().equals(phase)) return binding;
        }
        return null;
    }

    /**
     * Original UT.Code.units ratio and final rounding, without signed multiplication overflow.
     * Returns -1 for invalid inputs or results outside native FluidStack int capacity; zero stays
     * zero, never coerced to1. This is quantity arithmetic, not zero-volume catalyst semantics.
     */
    public static int millibuckets(long sourceAmount, long sourceUnit, long referenceMillibuckets,
                                  boolean roundUp) {
        if (sourceAmount < 0 || sourceUnit <= 0 || referenceMillibuckets < 0) return -1;
        BigInteger[] ratio = BigInteger.valueOf(sourceAmount)
                .multiply(BigInteger.valueOf(referenceMillibuckets))
                .divideAndRemainder(BigInteger.valueOf(sourceUnit));
        BigInteger result = ratio[0];
        if (roundUp && ratio[1].signum() != 0) result = result.add(BigInteger.ONE);
        return result.compareTo(MAX_AMOUNT) > 0 ? -1 : result.intValue();
    }
}
