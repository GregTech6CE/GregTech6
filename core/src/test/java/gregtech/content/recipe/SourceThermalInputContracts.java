package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.List;
import java.util.Objects;

import static gregtech.content.recipe.SourceThermalInputRules.InputProof;
import static gregtech.content.recipe.SourceThermalInputRules.plan;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Decision;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Facts;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Phase;
import static gregtech.content.recipe.SourceThermalRecyclingRules.PhaseProof;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Plan;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Stats;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Truth;

/** Independent source quantity literals and explicitly synthetic runtime-stat tests.
 * These are single-component planning contracts, not native availability/accepted counts.
 */
public final class SourceThermalInputContracts {
    private SourceThermalInputContracts() {}
    private static final long U = 648648000L; // Pinned CS120; no global unit change.
    private static final InputProof VERIFIED = InputProof.VERIFIED_SINGLE_COMPONENT;
    private static final Stats NEUTRAL_STATS = new Stats(1, 293);

    public static void main(String[] args) {
        run();
        System.out.println("Thirteen-prefix thermal input contracts passed (no native coverage).");
    }
    public static void run() {
        prefixEvidence();
        independentQuantities();
        realInputMassAndRounding();
        gatesAndUnknowns();
        oldOneUnitContract();
    }

    private static void prefixEvidence() {
        var forms = SourceThermalInputRules.forms();
        eq(13, forms.size(), "one coherent scope including all five graded gems");
        var prefixes = List.of(MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72,
                MaterialPrefix.gem, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny,
                MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny,
                MaterialPrefix.gemChipped, MaterialPrefix.gemFlawed, MaterialPrefix.gemFlawless,
                MaterialPrefix.gemExquisite, MaterialPrefix.gemLegendary);
        // Literal OP155-157/173/137-140 values, not obtained from the planned row.
        long[] amounts = {162162000, 72072000, 9009000, 648648000,
                810810000, 90090000, 891891000, 99099000,
                162162000, 324324000, 1297296000, 2594592000L, 5189184000L};
        int[] lines = {155, 156, 157, 173, 137, 138, 139, 140, 174, 175, 176, 177, 178};
        for (int i = 0; i < prefixes.size(); i++) {
            var form = forms.get(i);
            eq(prefixes.get(i), form.prefix(), "stable enumeration order");
            eq(amounts[i], form.sourceUnits(), "one original item amount");
            eq(lines[i], form.sourceLine(), "explicit source prefix declaration");
            eq(true, form.recyclable(), "event RECYCLABLE permission");
            eq(false, form.ore(), "not source ORE");
            eq(false, form.dirty(), "not source DIRTY; no inherited native parent-tag inference");
            eq(Truth.YES, SourceThermalInputRules.prefixDecision(form.prefix()), "reviewed prefix permission");
            eq(form, SourceThermalInputRules.form(form.prefix()), "canonical prefix descriptor");
        }
        for (var prefix : List.of(MaterialPrefix.oreRaw, MaterialPrefix.crushed,
                MaterialPrefix.crushedTiny, MaterialPrefix.dustImpure)) {
            eq(Truth.NO, SourceThermalInputRules.prefixDecision(prefix), "source ORE/DIRTY/nonrecyclable exclusion");
            eq(Decision.SUPPORTED_NO_ROW, plan(prefix, InputProof.UNKNOWN, U, null, null, null).decision(),
                    "known excluded prefix cannot acquire thermal rows");
        }
        for (var prefix : List.of(MaterialPrefix.dust, MaterialPrefix.ingot, MaterialPrefix.nugget, MaterialPrefix.plate)) {
            eq(Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(prefix), "unreviewed scope is not a global ban");
            eq(Decision.UNSUPPORTED, plan(prefix, VERIFIED, U, positive(U, true), NEUTRAL_STATS, phase(U, 144)).decision(),
                    "old/other prefix owner is outside this extension");
        }
        eq(Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(null), "unknown source prefix");
    }

    private static void independentQuantities() {
        // MT439, OP155-157: Tin self/U, molten144/U, source FURNACE YES.
        quantity(MaterialPrefix.dustSmall, 162162000, positive(U, true), phase(U, 144), 162162000, 36, true, "Tin small");
        quantity(MaterialPrefix.dustTiny, 72072000, positive(U, true), phase(U, 144), 72072000, 16, true, "Tin tiny");
        quantity(MaterialPrefix.dustDiv72, 9009000, positive(U, true), phase(U, 144), 9009000, 2, true, "Tin div72");
        // MT209/1381 Ruby -> Alumina3U/4; Loader_Fluids214 binds504mB/U, no target FURNACE.
        quantity(MaterialPrefix.gem, 648648000, positive(486486000, false), phase(U, 504), 486486000, 378, false, "Ruby gem");
        // OP174-178: all actual graded gem forms are recyclable; their generation restrictions
        // are not additional processing gates. Chipped94 explicitly observes the final floor.
        quantity(MaterialPrefix.gemChipped, 162162000, positive(486486000, false), phase(U, 504), 121621500, 94, false, "Ruby chipped");
        quantity(MaterialPrefix.gemFlawed, 324324000, positive(486486000, false), phase(U, 504), 243243000, 189, false, "Ruby flawed");
        quantity(MaterialPrefix.gemFlawless, 1297296000, positive(486486000, false), phase(U, 504), 972972000, 756, false, "Ruby flawless");
        quantity(MaterialPrefix.gemExquisite, 2594592000L, positive(486486000, false), phase(U, 504), 1945944000, 1512, false, "Ruby exquisite");
        quantity(MaterialPrefix.gemLegendary, 5189184000L, positive(486486000, false), phase(U, 504), 3891888000L, 3024, false, "Ruby legendary");
        // MT3717 Galena -> LeadU/3; OP137-140 masses are10U/8,10U/72,11U/8,11U/72.
        quantity(MaterialPrefix.crushedPurified, 810810000, positive(216216000, true), phase(U, 144), 270270000, 60, true, "Galena purified");
        quantity(MaterialPrefix.crushedPurifiedTiny, 90090000, positive(216216000, true), phase(U, 144), 30030000, 6, true, "Galena purified tiny");
        quantity(MaterialPrefix.crushedCentrifuged, 891891000, positive(216216000, true), phase(U, 144), 297297000, 66, true, "Galena refined");
        quantity(MaterialPrefix.crushedCentrifugedTiny, 99099000, positive(216216000, true), phase(U, 144), 33033000, 7, true, "Galena refined tiny");
        eq(Decision.SUPPORTED_NO_ROW, plan(MaterialPrefix.dustDiv72, VERIFIED, 9009000,
                positive(216216000, true), NEUTRAL_STATS, phase(U, 144)).decision(), "Galena div72 floors to0mB, never1");
    }

    private static void realInputMassAndRounding() {
        // Controlled runtime density16 and temperature1893 yield a1600K delta. These are not
        // claimed native material stats: literal floor results distinguish true original mass
        // from output/target mass, and preserve source111.111111 double evaluation.
        var runtimeStats = new Stats(16, 1893);
        var runtimePhase = new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, U, 144, 1893, false);
        long[] ticks = {444, 197, 24, 1777, 2222, 246, 2444, 271, 444, 888, 3555, 7111, 14222};
        for (int i = 0; i < SourceThermalInputRules.forms().size(); i++) {
            var form = SourceThermalInputRules.forms().get(i);
            var row = plan(form.prefix(), VERIFIED, form.sourceUnits(), positive(U, false), runtimeStats, runtimePhase);
            eq(ticks[i], row.row().ticks(), "actual input amount drives complete original mass");
        }
        var sameMassLessOutput = plan(MaterialPrefix.crushedPurified, VERIFIED, 810810000,
                positive(U / 3, false), runtimeStats, runtimePhase);
        eq(2222L, sameMassLessOutput.row().ticks(), "target1/3 does not divide original input mass/time");
        eq(60, sameMassLessOutput.row().millibuckets(), "quantity still uses target conversion");
        var hotterFluid = plan(MaterialPrefix.dustSmall, VERIFIED, 162162000, positive(U, false),
                new Stats(16, 1000), new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, U, 144, 1893, false));
        eq(444L, hotterFluid.row().ticks(), "actual fluid temperature participates in maximum");

        // Synthetic ratio: floor((U/72)*1/U)=0, then fluid conversion is0. Fusing both steps
        // would yield floor((U/72)*1*72/U)=1, so a final-floor-only algorithm is observably wrong.
        var floorFirst = plan(MaterialPrefix.dustDiv72, VERIFIED, 9009000, positive(1, true),
                NEUTRAL_STATS, phase(1, 72));
        eq(Decision.SUPPORTED_NO_ROW, floorFirst.decision(), "target floor must precede fluid floor");
        eq(1L, (9009000L * 72) / U, "counterexample fused result");

        // Verified rational lava binding, controlled zero mass: U/72 gives1mB, source energy5t.
        // The lava branch is not max(16,lavaCost). This does not assert a native Lava dust holder.
        var lava = plan(MaterialPrefix.dustDiv72, VERIFIED, 9009000, positive(U, false),
                new Stats(0, 1300), new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, 9 * U, 1000, 1300, true));
        eq(1, lava.row().millibuckets(), "rational lava conversion");
        eq(5L, lava.row().ticks(), "lava source floor, not ordinary16minimum");
    }

    private static void gatesAndUnknowns() {
        var p = MaterialPrefix.dustSmall;
        var facts = positive(U, true);
        var liquid = phase(U, 144);
        eq(Decision.SUPPORTED_NO_ROW, plan(p, InputProof.ABSENT, 0, null, null, null).decision(), "missing actual input");
        eq(Decision.UNSUPPORTED, plan(p, InputProof.UNKNOWN, U / 4, facts, NEUTRAL_STATS, liquid).decision(), "unproved/multi-component input");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U, facts, NEUTRAL_STATS, liquid).decision(), "cannot pretend fractional input has1U");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, 0, facts, NEUTRAL_STATS, liquid).decision(), "zero actual amount unsupported");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, -1, facts, NEUTRAL_STATS, liquid).decision(), "negative actual amount unsupported");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, Long.MAX_VALUE, facts, NEUTRAL_STATS, liquid).decision(), "out-of-contract actual amount");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U / 4, null, NEUTRAL_STATS, liquid).decision(), "unproved material/target facts");
        eq(Decision.SUPPORTED_NO_ROW, plan(p, VERIFIED, U / 4,
                new Facts(Truth.NO, Truth.UNKNOWN, U, Truth.UNKNOWN, Truth.UNKNOWN), null, null).decision(), "source notMELTING");
        eq(Decision.SUPPORTED_NO_ROW, plan(p, VERIFIED, U / 4,
                new Facts(Truth.UNKNOWN, Truth.YES, U, Truth.UNKNOWN, Truth.UNKNOWN), null, null).decision(), "source blacklisted");
        eq(Decision.SUPPORTED_NO_ROW, plan(p, VERIFIED, U / 4, facts, null,
                new Phase(PhaseProof.PROVEN_NO_DEFAULT_PHASE, 0, 0, 0, false)).decision(), "proven no source phase");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U / 4, facts, NEUTRAL_STATS,
                new Phase(PhaseProof.UNKNOWN, 0, 0, 0, false)).decision(), "unknown or missing native phase is not source absence");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U / 4, facts, new Stats(Double.NaN, 293), liquid).decision(), "invalid density remains unsupported");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U / 4, facts, new Stats(Double.MAX_VALUE, 293), liquid).decision(), "unrepresentable mass remains unsupported");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U / 4, positive(Long.MAX_VALUE, true), NEUTRAL_STATS, liquid).decision(), "checked target multiplication overflow");
        var calcite = plan(p, VERIFIED, U / 4, new Facts(Truth.YES, Truth.NO, U, Truth.UNKNOWN, Truth.YES), NEUTRAL_STATS, liquid);
        eq(true, calcite.row().melter(), "OUTPUT target own smelting-to-Calcite branch");
        var noFurnace = plan(p, VERIFIED, U / 4, positive(U, false), NEUTRAL_STATS, liquid);
        eq(false, noFurnace.row().melter(), "target FURNACE NO leaves Smelter only");
        eq(true, noFurnace.row().smelter(), "Smelter does not require FURNACE");
        eq(Decision.UNSUPPORTED, plan(p, VERIFIED, U / 4,
                new Facts(Truth.YES, Truth.NO, U, Truth.UNKNOWN, Truth.NO), NEUTRAL_STATS, liquid).decision(), "unknown target Melter gate");
    }

    private static void oldOneUnitContract() {
        // Conditional arithmetic equivalence, not an assertion of default gem forms for every
        // material. The old public proof/entry point remains independent and unchanged.
        for (var facts : List.of(positive(U, true), positive(U / 3, false),
                new Facts(Truth.NO, Truth.NO, U, Truth.YES, Truth.NO),
                new Facts(Truth.UNKNOWN, Truth.UNKNOWN, U, Truth.UNKNOWN, Truth.UNKNOWN))) {
            var old = SourceThermalRecyclingRules.plan(SourceThermalRecyclingRules.InputProof.VERIFIED_SELF_ONE_UNIT,
                    facts, new Stats(8.912, 1728), phase(U, 144));
            var extension = plan(MaterialPrefix.gem, VERIFIED, U, facts, new Stats(8.912, 1728), phase(U, 144));
            eq(old, extension, "old1U result unchanged by shared actual-amount calculation");
        }
        eq(Decision.UNSUPPORTED, SourceThermalRecyclingRules.plan(SourceThermalRecyclingRules.InputProof.UNKNOWN,
                positive(U, true), NEUTRAL_STATS, phase(U, 144)).decision(), "old input proof not widened");
    }

    private static Facts positive(long targetAmount, boolean targetFurnace) {
        return new Facts(Truth.YES, Truth.NO, targetAmount, targetFurnace ? Truth.YES : Truth.NO, Truth.NO);
    }
    private static Phase phase(long units, long millibuckets) {
        return new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, units, millibuckets, 293, false);
    }
    private static void quantity(MaterialPrefix prefix, long actual, Facts facts, Phase phase,
                                 long expectedTarget, int expectedMb, boolean melter, String label) {
        Plan plan = plan(prefix, VERIFIED, actual, facts, NEUTRAL_STATS, phase);
        eq(Decision.SUPPORTED_ROW, plan.decision(), label + " row");
        eq(expectedTarget, plan.row().targetUnits(), label + " converted material units");
        eq(expectedMb, plan.row().millibuckets(), label + " fluid volume");
        eq(melter, plan.row().melter(), label + " target Melter permission");
        eq(true, plan.row().smelter(), label + " Smelter");
        eq(16, plan.row().power(), label + " power");
    }
    private static void eq(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
    }
}
