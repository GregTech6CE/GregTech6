package gregtech.content.recipe;

import gregtech.engine.recipe.SourceRecyclingMath;
import static gregtech.content.recipe.SourceThermalRecyclingRules.*;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Truth.*;

/** Standalone Java17 contracts; all production values below have independent source citations.
 * Deliberately synthetic invalid/edge fixtures are labeled and do not claim actual recipe rows.
 */
public final class SourceThermalRecyclingContracts {
    private static final long U = 648648000L; // Source CS120, independently literal.
    private static int checks;
    public static void main(String[] args) {
        run();
        System.out.println("SourceThermalRecyclingContracts passed: " + checks + " checks");
    }
    public static void run() {
        checks = 0;
        sourceExamples();
        twoConversionsAndBounds();
        decisions();
        overflowAndInvalid();
        reviewedCompatibility();
    }

    private static void sourceExamples() {
        // MT417, MT62/transmetal73, FL1075/1077, Loader_OreProcessing289-291.
        var nickel = plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(U, NO, NO),
                new Stats(8.912, 1728), liquid(144, U, 1728, false));
        row(nickel, U, 144, 888, false, "Nickel: no source FURNACE, Smelter only");
        // MT419 -> same metal factory <=1200 gives FURNACE. Native binding is separately tested.
        row(plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(U, YES, NO),
                new Stats(7.134, 692), liquid(144, U, 692, false)), U, 144, 197, true, "Zinc");
        // MT3717 Pb3/Ag3/S2; MT472/436/404; OreDictMaterial480/543-549.
        // This literal is not the current port's float and not Pb's density.
        equal(8.707875, (3 * 11.342 + 3 * 10.501 + 2 * 2.067) / 8, "source Galena density");
        row(plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(U / 3, YES, NO),
                new Stats(8.707875, 600), liquid(144, U, 600, false)), U / 3, 48, 185, true,
                "Galena to Lead, target600K not current imported601K");

        // Runtime-stat contract: correcting the formula must not silently rewrite imported601K.
        // This controlled one-degree variation is not called original source parity.
        var importedLeadStats = plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(U / 3, YES, NO),
                new Stats(8.707875, 601), liquid(144, U, 600, false));
        equal(186L, importedLeadStats.row().ticks(), "runtime target601K remains observable");

        // MT1302 self2U/3; Loader_Fluids191=1000/U at423K. Density is source C/H1:2.
        var plastic = plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(2 * U / 3, YES, NO),
                new Stats(0.7557265866666667, 423), liquid(1000, U, 423, false));
        row(plastic, 2 * U / 3, 666, 16, true, "Plastic source rational target amount");

        // Source MT1194 binds1000mB to9U, source CS216=80EU/mB, not111mB/U.
        // This is a quantity/energy fixture, not an expansion of the1U producer's input coverage.
        // Low source Lava density derives MT1881/1636/1056 and element stats400/414/402/396.
        var lava = SourceRecyclingMath.singleFluid(9 * U, U, U, 9 * U, 1000,
                0.223166125, 1300, 1300, 293, true);
        equal(9 * U, lava.targetUnits(), "Lava target quantity");
        equal(1000, lava.millibuckets(), "Lava9U stays1000, never999");
        equal(5000L, lava.ticks(), "Lava source energy minimum");
        equal(111, OriginalMaterialFluidUnits.millibuckets(U, 9 * U, 1000, false), "Lava1U floors only now");
        equal(999, 9 * OriginalMaterialFluidUnits.millibuckets(U, 9 * U, 1000, false),
                "wrong integerized-perU algorithm would lose1mB");
    }

    private static void twoConversionsAndBounds() {
        // Synthetic integer-unit fixture: floor(3*2/7)=0, then floor(0*5/3)=0.
        // Fusing would produce floor(3*2*5/(7*3))=1. These are not source material literals.
        var separate = SourceRecyclingMath.singleFluid(3, 7, 2, 3, 5, 0, 293, 293, 293, false);
        equal(0L, separate.targetUnits(), "first downward conversion remains observable");
        equal(0, separate.millibuckets(), "second downward conversion is separate");
        equal(1L, 3L * 2 * 5 / (7 * 3), "fused counterexample discriminates the algorithm");
        // Synthetic thermal samples to prove max(actualFluid,target) and final-floor behavior.
        var hotFluid = SourceRecyclingMath.singleFluid(U, U, U, U, 144, 8.912, 1728, 2000, 293, false);
        var hotTarget = SourceRecyclingMath.singleFluid(U, U, U, U, 144, 8.912, 2000, 1728, 293, false);
        equal(hotTarget.ticks(), hotFluid.ticks(), "temperature max is symmetric");
        check(hotFluid.ticks() > 888, "actual hotter fluid increases duration");
        equal(16L, SourceRecyclingMath.singleFluid(U, U, U, U, 144, 8.912, 0, 0, 293, false).ticks(),
                "negative temperature delta keeps ordinary16minimum");
        // Lava branch is NOT max(16,lavaBase,thermal): source1mB at zero density costs5ticks.
        equal(5L, SourceRecyclingMath.singleFluid(U, U, U, U, 1, 0, 293, 293, 293, true).ticks(),
                "synthetic tiny lava branch does not inherit ordinary16minimum");
        equal(0, OriginalMaterialFluidUnits.millibuckets(1, U, 144, false), "zero output remains zero");
        equal(144, OriginalMaterialFluidUnits.millibuckets(U, U, 144, false), "existing rational helper");
        // Existing helper BigInteger path safely handles an overflowing intermediate product.
        equal(Integer.MAX_VALUE, OriginalMaterialFluidUnits.millibuckets(Long.MAX_VALUE, Long.MAX_VALUE,
                Integer.MAX_VALUE, false), "representable ratio not lost to intermediate overflow");
    }

    private static void decisions() {
        var stats = new Stats(8.912, 1728);
        var bound = liquid(144, U, 1728, false);
        var positive = facts(U, NO, NO);
        var unknownPhase = new Phase(PhaseProof.UNKNOWN, 0, 0, 0, false);
        var noPhase = new Phase(PhaseProof.PROVEN_NO_DEFAULT_PHASE, 0, 0, 0, false);
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.ABSENT, null, null, null), "missing actual input");
        decision(Decision.UNSUPPORTED, plan(InputProof.UNKNOWN, positive, stats, bound), "unknown/mixed input");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, null, stats, bound), "unknown identity");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive, null, bound), "unknown stats");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive, stats, unknownPhase), "unknown phase");
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive, null, noPhase),
                "proven source no phase does not require density");
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                new Facts(UNKNOWN, YES, U / 2, UNKNOWN, UNKNOWN), null, unknownPhase),
                "known blacklist dominates unused thermal/phase facts (Graphene source1535)");
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                new Facts(NO, UNKNOWN, U, UNKNOWN, UNKNOWN), null, unknownPhase), "known notMELTING");
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                new Facts(UNKNOWN, UNKNOWN, 0, UNKNOWN, UNKNOWN), null, unknownPhase), "disabled target amount");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                new Facts(UNKNOWN, NO, U, YES, NO), stats, bound), "unknownMELTING not a positive");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                new Facts(YES, UNKNOWN, U, YES, NO), stats, bound), "unknown blacklist not a negative");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                facts(U, NO, UNKNOWN), stats, bound), "unknown targetCalcite does not become false");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                facts(U, UNKNOWN, NO), stats, bound), "unknown targetFURNACE does not become false");
        check(plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(U, UNKNOWN, YES), stats, bound).row().melter(),
                "target's own targetCalcite proves Melter even with unknownFURNACE");
        check(plan(InputProof.VERIFIED_SELF_ONE_UNIT, facts(U, YES, UNKNOWN), stats, bound).row().melter(),
                "targetFURNACE proves Melter even with unknownCalcite");
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                facts(1, YES, NO), stats, bound), "positive target units but zero fluid suppresses row");
        decision(Decision.SUPPORTED_NO_ROW, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                facts(1, UNKNOWN, UNKNOWN), stats, bound), "zero fluid does not need the laterMelter gate");
        // The current phase/type/name/gen flags have no inputs here and therefore cannot prove eligibility.
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive, stats, null),
                "no nativeMETAL to source molten inference");
    }

    private static void overflowAndInvalid() {
        var bound = liquid(144, U, 1728, false);
        var positive = facts(U, NO, NO);
        for (double density : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1, Double.MAX_VALUE})
            decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive,
                    new Stats(density, 1728), bound), "bad density " + density);
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT,
                facts(-1, YES, NO), new Stats(8.912, 1728), bound), "negative target invalid, not zero");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive,
                new Stats(8.912, -1), bound), "negative Kelvin invalid");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive,
                new Stats(8.912, 1728), liquid(144, 0, 1728, false)), "invalid binding reference");
        decision(Decision.UNSUPPORTED, plan(InputProof.VERIFIED_SELF_ONE_UNIT, positive,
                new Stats(8.912, 1728), liquid(Long.MAX_VALUE, U, 1728, false)), "mB int overflow");
        throwsArithmetic(() -> SourceRecyclingMath.singleFluid(Long.MAX_VALUE, 3, 2, 1, 1,
                0, 293, 293, 293, false), "existing checked first conversion refuses overflow");
        throwsArithmetic(() -> SourceRecyclingMath.singleFluid(U, U, U, U, 144,
                1e100, 1728, 1728, 293, false), "finite duration beyond long range is rejected");
        throwsIllegal(() -> SourceRecyclingMath.singleFluid(-1, U, U, U, 144,
                8.912, 1728, 1728, 293, false), "negative input units rejected");
    }

    private static void reviewedCompatibility() {
        // Existing reviewed helper signature stays available. MT410/414, reviewed source Cinnabar density.
        equal(519L, SourceRecyclingMath.singleLiquid(U, U, U, 144, 4.54, 1941, 1941, 293).ticks(), "Ti unchanged");
        equal(830L, SourceRecyclingMath.singleLiquid(U, U, U, 144, 7.874, 1811, 1811, 293).ticks(), "Fe unchanged");
        var cinnabar = SourceRecyclingMath.singleLiquid(U, U, U / 3, 144, 7.8003, 234, 300, 293);
        equal(48, cinnabar.millibuckets(), "Cinnabar targetMercury quantity unchanged");
        equal(16L, cinnabar.ticks(), "Cinnabar ordinary minimum unchanged");
    }
    private static Facts facts(long amount, Truth furnace, Truth calcite) { return new Facts(YES, NO, amount, furnace, calcite); }
    private static Phase liquid(long mb, long units, long temperature, boolean lava) {
        return new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, units, mb, temperature, lava);
    }
    private static void row(Plan plan, long target, int mb, long ticks, boolean melter, String label) {
        decision(Decision.SUPPORTED_ROW, plan, label);
        equal(target, plan.row().targetUnits(), label + " targetUnits");
        equal(mb, plan.row().millibuckets(), label + " mB");
        equal(ticks, plan.row().ticks(), label + " ticks");
        equal(melter, plan.row().melter(), label + " Melter");
        check(plan.row().smelter(), label + " Smelter");
        equal(16, plan.row().power(), label + " power");
    }
    private static void decision(Decision expected, Plan actual, String label) { equal(expected, actual.decision(), label); }
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
    }
    private static void throwsArithmetic(Runnable action, String label) {
        checks++; try { action.run(); } catch (ArithmeticException expected) { return; } throw new AssertionError(label);
    }
    private static void throwsIllegal(Runnable action, String label) {
        checks++; try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError(label);
    }
}
