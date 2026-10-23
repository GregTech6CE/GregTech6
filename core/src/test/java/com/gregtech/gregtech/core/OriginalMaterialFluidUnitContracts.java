package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.recipe.OriginalMaterialFluidUnits;

/** Exact source arithmetic and bounded binding contracts; not a native recipe test. */
public final class OriginalMaterialFluidUnitContracts {
    private static int assertions;
    public static void main(String[] args) {
        long u = OriginalMaterialFluidUnits.SOURCE_UNIT;
        check(u == 648648000L && u == com.gregtech.gregtech.api.material.GTValues.U,
                "original source unit equals current port unit, unlike legacy transpiler mU");
        check(convert(u / 72, 144, true) == 2 && convert(u / 36, 144, true) == 4, "ordinary inputs");
        check(convert(u / 24, 144, true) == 6 && convert(u / 12, 144, true) == 12, "block inputs round once");
        check(convert(u / 144, 144, true) == 1, "small cryolite");
        check(convert(7 * (u / 240), 1000, false) == 29, "ordinary fluorine");
        check(convert(7 * (u / 80), 1000, false) == 87, "block fluorine");
        check(convert(7 * (u / 480), 1000, false) == 14, "small fluorine");
        check(convert(9 * u, 1000, false) == 9000 && convert(27 * u, 1000, false) == 27000,
                "source amounts above int range stay long");
        check(convert(1, 144, false) == 0 && convert(1, 144, true) == 1, "final rounding only");
        check(convert(0, 144, true) == 0 && convert(-1, 144, false) == -1, "zero and negative");
        check(convert(u, 504, false) == 504 && convert(u, 144, false) == 144,
                "nonstandard Alumina and Mercury reference volumes, no phase heuristic");
        check(OriginalMaterialFluidUnits.millibuckets(Long.MAX_VALUE, Long.MAX_VALUE, 1000, true) == 1000,
                "intermediate overflow does not corrupt representable ratio");
        check(OriginalMaterialFluidUnits.millibuckets(Integer.MAX_VALUE, 1, 1, false) == Integer.MAX_VALUE,
                "native upper boundary");
        check(OriginalMaterialFluidUnits.millibuckets((long) Integer.MAX_VALUE + 1, 1, 1, false) == -1,
                "native overflow rejects rather than clamps");
        check(OriginalMaterialFluidUnits.millibuckets(Long.MIN_VALUE, u, 144, false) == -1
                && OriginalMaterialFluidUnits.millibuckets(1, 0, 144, false) == -1
                && OriginalMaterialFluidUnits.millibuckets(1, u, -1, false) == -1, "invalid ratio rejects");
        var cryolite = OriginalMaterialFluidUnits.binding("Cryolite", 8009, "liquid");
        check(cryolite != null && cryolite.fluidField().equals("GenLiquid_Cryolite")
                && cryolite.registryName().equals("cryolite") && cryolite.referenceMillibuckets() == 144,
                "legacy native identity retained independently of source molten volume");
        check(OriginalMaterialFluidUnits.binding("Cryolite", 8009, "gas") == null
                && OriginalMaterialFluidUnits.binding("Cryolite", 8010, "liquid") == null
                && OriginalMaterialFluidUnits.binding("Alumina", 8011, "liquid") == null,
                "wrong phase, identity and unaudited bindings rejected");
        check(OriginalMaterialFluidUnits.binding("AluminiumFluoride", 8010, "liquid") != null
                && OriginalMaterialFluidUnits.binding("CarbonDioxide", 9836, "gas") != null
                && OriginalMaterialFluidUnits.binding("Fluorine", 90, "gas") != null, "all four audited bindings");
        System.out.println("Original material fluid unit contracts passed: " + assertions);
    }
    private static int convert(long amount, long volume, boolean up) {
        return OriginalMaterialFluidUnits.millibuckets(amount, OriginalMaterialFluidUnits.SOURCE_UNIT, volume, up);
    }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
}
