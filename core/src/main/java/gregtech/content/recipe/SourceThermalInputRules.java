package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.List;

import static gregtech.content.recipe.SourceThermalRecyclingRules.Decision;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Facts;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Phase;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Plan;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Stats;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Truth;

/**
 * Thirteen single-component item-input contracts for source RecyclingProcessing, not the complete
 * listener and not a global prefix policy. Pinned GT6 3703e40308c8c030763fd6297dea8b210d2a77b1:
 * OreDictManager 650-669 admits RECYCLABLE prefixes; Loader_OreProcessing 240-291 rejects ORE and
 * ORE_PROCESSING_DIRTY, converts each original component, then uses all original mass for timing.
 * Multi-component merging, native identity/composition proof, existing owners and admission stay
 * outside this class. No material generation condition or imported native flag is a source gate.
 */
public final class SourceThermalInputRules {
    private SourceThermalInputRules() {}
    private static final long U = OriginalMaterialFluidUnits.SOURCE_UNIT;

    public enum InputProof { VERIFIED_SINGLE_COMPONENT, ABSENT, UNKNOWN }

    /** A one-item source amount; OP sourceLine explicitly declares RECYCLABLE and neither excluded
     * tag. This is source processing evidence, not a promise of a live generated/native holder.
     */
    public record Form(MaterialPrefix prefix, long sourceUnits, int sourceLine) {
        public boolean recyclable() { return true; }
        public boolean ore() { return false; }
        public boolean dirty() { return false; }
    }
    private static final List<Form> FORMS = List.of(
            new Form(MaterialPrefix.dustSmall, U / 4, 155),
            new Form(MaterialPrefix.dustTiny, U / 9, 156),
            new Form(MaterialPrefix.dustDiv72, U / 72, 157),
            new Form(MaterialPrefix.gem, U, 173),
            new Form(MaterialPrefix.crushedPurified, 10 * U / 8, 137),
            new Form(MaterialPrefix.crushedPurifiedTiny, 10 * U / 72, 138),
            new Form(MaterialPrefix.crushedCentrifuged, 11 * U / 8, 139),
            new Form(MaterialPrefix.crushedCentrifugedTiny, 11 * U / 72, 140),
            new Form(MaterialPrefix.gemChipped, U / 4, 174),
            new Form(MaterialPrefix.gemFlawed, U / 2, 175),
            new Form(MaterialPrefix.gemFlawless, 2 * U, 176),
            new Form(MaterialPrefix.gemExquisite, 4 * U, 177),
            new Form(MaterialPrefix.gemLegendary, 8 * U, 178));

    public static List<Form> forms() { return FORMS; }
    public static Form form(MaterialPrefix prefix) {
        for (Form form : FORMS) if (form.prefix() == prefix) return form;
        return null;
    }

    /** A finite proof boundary. Unreviewed prefixes (including other recyclable forms and the old
     * dust/ingot producer) are UNKNOWN here, not banned from the original listener or other owners.
     * OP133 marks oreRaw ORE; OP135-136 mark crushed forms DIRTY; OP158 lacks RECYCLABLE for impure.
     */
    public static Truth prefixDecision(MaterialPrefix prefix) {
        if (form(prefix) != null) return Truth.YES;
        if (prefix == MaterialPrefix.oreRaw || prefix == MaterialPrefix.crushed
                || prefix == MaterialPrefix.crushedTiny || prefix == MaterialPrefix.dustImpure) return Truth.NO;
        return Truth.UNKNOWN;
    }

    /** InputProof covers exactly one actual original material component and an ingredable native
     * stack. Its quantity must match this one-item source contract; a composition override or an
     * incorrectly normalized stack is unsupported, not silently changed into source material.
     * Source material/target/phase evidence and runtime density/temperatures reuse the 1U planner.
     */
    public static Plan plan(MaterialPrefix prefix, InputProof input, long actualInputUnits,
                            Facts facts, Stats stats, Phase phase) {
        Truth permission = prefixDecision(prefix);
        if (permission == Truth.NO) return new Plan(Decision.SUPPORTED_NO_ROW, null);
        if (permission != Truth.YES) return new Plan(Decision.UNSUPPORTED, null);
        if (input == InputProof.ABSENT) return new Plan(Decision.SUPPORTED_NO_ROW, null);
        if (input != InputProof.VERIFIED_SINGLE_COMPONENT || actualInputUnits != form(prefix).sourceUnits())
            return new Plan(Decision.UNSUPPORTED, null);
        return SourceThermalRecyclingRules.planVerifiedAmount(actualInputUnits, facts, stats, phase);
    }
}
