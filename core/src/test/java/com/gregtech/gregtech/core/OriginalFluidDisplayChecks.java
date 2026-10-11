package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.content.fluid.OriginalFluidDisplayRules;
import com.gregtech.gregtech.content.fluid.OriginalFluidDisplayRules.*;
import com.gregtech.gregtech.data.FluidCatalog.FluidFlags;
import java.math.BigInteger;
import java.util.List;

/** Independent ItemFluidDisplay branch samples; no Minecraft startup or catalog scan. */
final class OriginalFluidDisplayChecks {
    private static int assertions;

    static int run() {
        assertions = 0;
        var lava = OriginalFluidDisplayRules.fuelPower(-80, 1, 1);
        expect(lava.equals(BigInteger.valueOf(80L * GTValues.U)), "Original lava has 80 GU/L");
        expect(OriginalFluidDisplayRules.fuelPower(-1280, 1, 1).equals(BigInteger.valueOf(1280L * GTValues.U)),
                "Original volcanic lava has 1280 GU/L");
        var fractional = OriginalFluidDisplayRules.fuelPower(-1, 1, 4);
        expect(fractional.equals(BigInteger.valueOf(GTValues.U / 4)), "Fractional per-liter energy is retained until formatting");
        expect(OriginalFluidDisplayRules.fuelPower(-100, 20, 0).signum() == 0, "Malformed zero input cannot divide by zero");
        expect(OriginalFluidDisplayRules.fuelPower(Long.MIN_VALUE, Long.MAX_VALUE, 1).signum() > 0,
                "Extreme valid recipe energy cannot wrap negative");

        var water = facts(1000, false, false, false, false, 1000, 0, 1000, FluidFlags.SIMPLE, false, Owner.VANILLA);
        var lines = OriginalFluidDisplayRules.describe(water, List.of());
        expect(!has(lines, "luminosity"), "Non-luminous water has no guessed RGB brightness");
        expect(has(lines, "owner_vanilla") && !has(lines, "owner_gt6"), "Vanilla fluid ownership is retained");
        expect(has(lines, "simple") && !has(lines, "castable"), "Simple water is not castable");
        expect(args(lines, "temperature").equals(List.of("300", "27")), "Runtime 300 K is shown as original 27 Celsius");
        expect(args(lines, "viscosity").equals(List.of("1000")), "Viscosity comes from runtime facts");
        expect(args(lines, "amount").equals(List.of("1000")), "Recipe quantity is retained");

        lines = OriginalFluidDisplayRules.describe(facts(0, false, false, false, true, 0, 10, 250000, 0, false, Owner.GT6), List.of());
        expect(!has(lines, "amount"), "Unbound creative display does not invent a 1000 L quantity");
        expect(has(lines, "density_equal") && !has(lines, "density_lighter"), "Zero density still normally sinks in original");
        expect(args(lines, "luminosity").equals(List.of("10")), "Light level is the real 0..15 property");
        expect(has(lines, "castable"), "Actual ingot availability enables the source mold hint");

        lines = OriginalFluidDisplayRules.describe(facts(0, true, false, false, false, -10, 0, 0, 0, false, Owner.OTHER), List.of());
        expect(has(lines, "gas") && has(lines, "native_liquid"), "Heavy native gas disagreement keeps source warning");
        expect(has(lines, "density_lighter") && !has(lines, "viscosity"), "Negative density rises; zero viscosity is omitted");
        expect(has(lines, "owner_other"), "External fluid has its own ownership line");
        lines = OriginalFluidDisplayRules.describe(facts(0, true, true, true, true, 1, 1, 1, 0, false, Owner.GT6), List.of());
        expect(has(lines, "plasma") && has(lines, "native_gas_note") && !has(lines, "castable"), "Plasma state has priority over gas and molds");
        lines = OriginalFluidDisplayRules.describe(facts(0, false, false, true, false, 1, 1, 1, 0, false, Owner.GT6), List.of());
        expect(has(lines, "native_gas_warning"), "GT liquid/native gas mismatch has a separate warning");

        var fuels = List.of(new Fuel("gt.recipe.fuels.hot", lava), new Fuel("fractional", fractional));
        lines = OriginalFluidDisplayRules.describe(water, fuels);
        expect(literals(fuelLine(lines, "gt.recipe.fuels.hot")).contains("80_000"), "1000 L times 80 GU/L gives 80000 GU total");
        expect(literals(fuelLine(lines, "fractional")).containsAll(List.of("0", "250")), "Original whole GU/L presentation does not lose fractional total");
        lines = OriginalFluidDisplayRules.describe(facts(1000, false, false, false, false, 1, 0, 1,
                FluidFlags.SIMPLE | FluidFlags.POWER_CONDUCTING | FluidFlags.COOKING_OIL, true, Owner.GT6), fuels);
        expect(has(lines, "industrial") && has(lines, "not_flammable"), "Industrial lubricant source warnings remain");
        expect(lines.stream().flatMap(l -> l.parts().stream()).noneMatch(p -> "gt.recipe.fuels.hot".equals(p.key())),
                "Lubricant suppresses all fuel map rows even if a recipe exists");
        expect(has(lines, "simple") && has(lines, "cooking_oil") && has(lines, "conducting") && has(lines, "no_tanks"),
                "Source flags produce independent hints, not a guessed exclusive material category");
        var hazard = new Facts("acid", true, true, 0, "HCl", 300, true, false, false, false, 1, 0, 1, 0, true, true, false, Owner.GT6);
        lines = OriginalFluidDisplayRules.describe(hazard, List.of());
        expect(has(lines, "registry") && has(lines, "nonstandard") && has(lines, "acid") && has(lines, "magic"),
                "Advanced identity, nonstandard and hazards are independent");
        expect(lines.stream().flatMap(l -> l.parts().stream()).anyMatch(p -> p.key() == null && p.arguments().equals(List.of("HCl"))),
                "Chemical formula remains literal data");
        expect(OriginalFluidDisplayRules.number(BigInteger.valueOf(9999)).equals("9999"), "Original four digits have no separator");
        expect(OriginalFluidDisplayRules.number(BigInteger.valueOf(-10000)).equals("-10_000"), "Original grouping retains sign");
        System.out.println("Original fluid display checks passed: " + assertions);
        return assertions;
    }

    private static Facts facts(long amount, boolean gas, boolean plasma, boolean nativeGas, boolean castable,
                               int density, int light, int viscosity, long flags, boolean lubricant, Owner owner) {
        return new Facts("fixture", false, false, amount, "", 300, gas, plasma, nativeGas, castable,
                density, light, viscosity, flags, false, false, lubricant, owner);
    }
    private static boolean has(List<Line> lines, String key) {
        return lines.stream().flatMap(l -> l.parts().stream()).anyMatch(p -> (OriginalFluidDisplayRules.PREFIX + key).equals(p.key()));
    }
    private static List<Object> args(List<Line> lines, String key) {
        return lines.stream().flatMap(l -> l.parts().stream()).filter(p -> (OriginalFluidDisplayRules.PREFIX + key).equals(p.key()))
                .findFirst().orElseThrow().arguments();
    }
    private static Line fuelLine(List<Line> lines, String key) {
        return lines.stream().filter(l -> key.equals(l.parts().get(0).key())).findFirst().orElseThrow();
    }
    private static List<String> literals(Line line) {
        return line.parts().stream().filter(p -> p.key() == null).map(p -> (String) p.arguments().get(0)).toList();
    }
    private static void expect(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
