package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static gregtech.content.recipe.SourceAlloyRecipes.*;

/** Dependency-free source-table contracts; native selector retention and lookup need runtime tests. */
public final class SourceAlloyContracts {
    private SourceAlloyContracts() {}

    public static void main(String[] args) {
        run();
        System.out.println("Alloy source contracts passed (101 Mixer, 2 Smelter; no native runtime coverage).");
    }

    public static void run() {
        equal(16, POWER, "source power");
        equal(101, mixer().size(), "52 source mix calls expand through all four source loops");
        equal(101, new HashSet<>(mixer()).size(), "no duplicate Mixer rows");
        equal(List.of(
                new Row(List.of(amount("molten.copper", 1)), amount("molten.annealedcopper", 1), 16, 0),
                new Row(List.of(amount("molten.iron", 1)), amount("molten.wroughtiron", 1), 16, 0)),
                smelter(), "two 1 mB to 1 mB annealing rows, 16 ticks, no selector");
        rawUnitsAndOptimization();
        representativeRows();
        immutableRows();
    }

    private static void rawUnitsAndOptimization() {
        int[] selectors = new int[5];
        for (Row row : mixer()) {
            equal(row.inputs().size(), row.selector(), "selector equals source input arity");
            selectors[row.selector()]++;
            long total = 0;
            int divisor = row.output().millibuckets();
            for (Amount input : row.inputs()) {
                check(input.millibuckets() > 0 && input.millibuckets() <= 36, "literal input mB, not 144 mB units");
                total += input.millibuckets();
                divisor = gcd(divisor, input.millibuckets());
                check(!input.fluidName().equals("molten.osmium"), "source uses osmiumelemental, not material alias");
                check(!input.fluidName().equals("molten.hslasteel"), "source HSLA does not use material alias");
            }
            check(row.output().millibuckets() <= 36, "literal output mB, not 144 mB units");
            equal(Math.max(total, row.output().millibuckets()), row.ticks(), "source ticks, no multiplier");
            // Original ST.tag has count zero; a native positive representative has count one.
            // All fluid coefficients already have gcd 1, so either representation leaves every
            // optimize=true source row unchanged. A future divisible row must revisit that adapter.
            equal(1, divisor, "count-one selector must not prevent a source fluid optimization");
        }
        equal(57, selectors[2], "two-input selectors");
        equal(26, selectors[3], "three-input selectors");
        equal(18, selectors[4], "four-input selectors");
    }

    private static void representativeRows() {
        for (String copper : List.of("molten.copper", "molten.annealedcopper")) {
            contains(row("molten.constantan", 2, 2, 2, amount(copper, 1), amount("molten.nickel", 1)));
            contains(row("molten.bronze", 4, 4, 2, amount(copper, 3), amount("molten.tin", 1)));
            contains(row("molten.redalloy", 1, 5, 2, amount(copper, 1), amount("molten.redstone", 4)));
            contains(row("molten.purplealloy", 2, 10, 4, amount(copper, 1), amount("molten.silver", 1),
                    amount("molten.redstone", 4), amount("molten.nikolite", 4)));
            contains(row("molten.signalum", 4, 14, 3, amount(copper, 3), amount("molten.silver", 1),
                    amount("molten.redstone", 10)));
            contains(row("molten.signalum", 8, 8, 3, amount(copper, 1), amount("molten.silver", 2),
                    amount("molten.redalloy", 5)));
        }
        for (String iron : List.of("molten.iron", "molten.wroughtiron", "molten.pigiron", "molten.meteoriciron")) {
            contains(row("molten.invar", 3, 3, 2, amount(iron, 2), amount("molten.nickel", 1)));
            contains(row("molten.stainlesssteel", 36, 36, 4, amount(iron, 24), amount("molten.nichrome", 5),
                    amount("molten.chromium", 3), amount("molten.manganese", 4)));
            contains(row("molten.electrotinealloy", 1, 9, 2, amount(iron, 1), amount("molten.nikolite", 8)));
        }
        for (String steel : List.of("molten.steel", "molten.meteoricsteel", "molten hsla", "molten.knightmetal")) {
            contains(row("molten.tungstensteel", 2, 2, 2, amount(steel, 1), amount("molten.tungsten", 1)));
        }
        equal(4L, mixer().stream().filter(r -> r.output().fluidName().equals("molten.tungstensteel")).count(),
                "exact four steel branches, including source HSLA literal");
        for (String glowstone : List.of("molten.glowstone", "molten.glowstoneceres", "molten.glowstoneio",
                "molten.glowstoneenceladus", "molten.glowstoneproteus", "molten.glowstonepluto", "molten.gloomstone")) {
            contains(row("molten.lumium", 4, 8, 3, amount("molten.tin", 3), amount("molten.silver", 1),
                    amount(glowstone, 4)));
        }
        equal(7L, mixer().stream().filter(r -> r.output().fluidName().equals("molten.lumium")).count(),
                "all and only seven pinned ANY.Glowstone members");
        contains(row("molten.redstonealloy", 1, 2, 2, amount("molten.silicon", 1), amount("molten.redstone", 1)));
        contains(row("molten.goldinductive", 2, 2, 2, amount("molten.gold", 1), amount("molten.redstone", 1)));
        contains(row("molten.netherite", 1, 8, 2, amount("molten.gold", 4), amount("molten.ancientdebris", 4)));
        contains(row("molten.ultimet", 36, 36, 4, amount("molten.cobalt", 20), amount("molten.chromium", 7),
                amount("molten.nichrome", 5), amount("molten.molybdenum", 4)));
        contains(row("molten.osmiridium", 2, 2, 2, amount("molten.osmiumelemental", 1), amount("molten.iridium", 1)));
        contains(row("molten.hsss", 9, 9, 3, amount("molten.hssg", 6), amount("molten.osmiumelemental", 1),
                amount("molten.iridium", 2)));
    }

    private static void immutableRows() {
        List<Amount> inputs = new ArrayList<>(List.of(amount("molten.iron", 1)));
        Row row = new Row(inputs, amount("molten.wroughtiron", 1), 16, 0);
        inputs.clear();
        equal(1, row.inputs().size(), "Row defensively copies inputs");
        immutable(() -> row.inputs().clear());
        immutable(() -> mixer().clear());
        immutable(() -> smelter().clear());
    }

    private static Row row(String output, int mb, long ticks, int selector, Amount... inputs) {
        return new Row(List.of(inputs), amount(output, mb), ticks, selector);
    }

    private static Amount amount(String fluid, int mb) { return new Amount(fluid, mb); }

    private static void contains(Row row) {
        check(mixer().contains(row), "missing exact source row: " + row);
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int remainder = a % b; a = b; b = remainder; }
        return a;
    }

    private static void immutable(Runnable mutation) {
        try { mutation.run(); }
        catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError("source lists must be immutable");
    }

    private static void equal(Object expected, Object actual, String reason) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError(reason + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
