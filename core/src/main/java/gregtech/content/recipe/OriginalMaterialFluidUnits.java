package gregtech.content.recipe;

import java.math.BigInteger;

/**
 * Explicit source phase bindings for audited Alumina chemistry and fixed Centrifuge rows.
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
            new Binding("Fluorine", 90, "gas", "GenGas_Fluorine", "fluorine", SOURCE_UNIT, 1000),
            new Binding("Chocolate", 9783, "liquid", "GenMolten_Chocolate", "molten.chocolate", SOURCE_UNIT, 144),
            new Binding("Deuterium", 11, "gas", "Deuterium", "deuterium", SOURCE_UNIT, 1000),
            new Binding("FishOil", 9871, "liquid", "Oil_Fish", "fishoil", SOURCE_UNIT, 1000),
            new Binding("HeavyWater", 9812, "liquid", "GenGas_HeavyWater", "heavywater", SOURCE_UNIT, 1000),
            new Binding("Helium", 20, "gas", "Helium", "helium", SOURCE_UNIT, 1000),
            new Binding("Helium3", 21, "gas", "GenGas_Helium3", "helium3", SOURCE_UNIT, 1000),
            new Binding("Hydrogen", 10, "gas", "Hydrogen", "hydrogen", SOURCE_UNIT, 1000),
            new Binding("Mercury", 800, "liquid", "GenLiquid_Mercury", "mercury", SOURCE_UNIT, 144),
            new Binding("NitrogenDioxide", 9831, "gas", "GenGas_NitrogenDioxide", "nitrogendioxide", SOURCE_UNIT, 1000),
            new Binding("SemiheavyWater", 9811, "liquid", "GenGas_SemiheavyWater", "semiheavywater", SOURCE_UNIT, 1000),
            new Binding("TritiatedWater", 9813, "liquid", "GenGas_TritiatedWater", "tritiatedwater", SOURCE_UNIT, 1000),
            new Binding("Uranium235Hexafluoride", 9012, "gas", "GenGas_Uranium235Hexafluoride", "uranium235hexafluoride", SOURCE_UNIT, 1000),
            new Binding("Uranium238Hexafluoride", 9010, "gas", "GenGas_Uranium238Hexafluoride", "uranium238hexafluoride", SOURCE_UNIT, 1000),
            new Binding("UraniumHexafluoride", 9008, "gas", "GenGas_UraniumHexafluoride", "uraniumhexafluoride", SOURCE_UNIT, 1000),
            new Binding("WhaleOil", 9887, "liquid", "Oil_Whale", "whaleoil", SOURCE_UNIT, 1000)
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
