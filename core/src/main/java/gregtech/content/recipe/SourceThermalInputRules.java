package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.List;
import java.util.stream.Stream;

import static gregtech.content.recipe.SourceThermalRecyclingRules.Decision;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Facts;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Phase;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Plan;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Stats;
import static gregtech.content.recipe.SourceThermalRecyclingRules.Truth;

/**
 * Eighty-one single-component item-input contracts for source RecyclingProcessing, not the complete
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
    private static final List<Form> INITIAL_FORMS = List.of(
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

    // OP639-738 exhausts prefix secondary-material additions: none of these 68 prefixes is
    // a receiver. This table does not infer an ingredable item from native recoverable=true.
    // Native itemCasing is the registered alias of source casingSmall (OP219).
    private static final List<Form> ADDITIONAL_FORMS = List.of(
            new Form(MaterialPrefix.rockGt, 9 * (U / 4), 141),
            new Form(MaterialPrefix.nugget, U / 9, 170),
            new Form(MaterialPrefix.chunkGt, U / 4, 169),
            new Form(MaterialPrefix.billet, (U / 3) * 2, 168),
            new Form(MaterialPrefix.ingot, U, 167),
            new Form(MaterialPrefix.ingotDouble, U * 2, 165),
            new Form(MaterialPrefix.ingotTriple, U * 3, 164),
            new Form(MaterialPrefix.ingotQuadruple, U * 4, 163),
            new Form(MaterialPrefix.ingotQuintuple, U * 5, 162),
            new Form(MaterialPrefix.plateGemTiny, U / 9, 199),
            new Form(MaterialPrefix.plateGem, U, 197),
            new Form(MaterialPrefix.plateTiny, U / 9, 198),
            new Form(MaterialPrefix.plate, U, 196),
            new Form(MaterialPrefix.plateDouble, U * 2, 195),
            new Form(MaterialPrefix.plateTriple, U * 3, 194),
            new Form(MaterialPrefix.plateQuadruple, U * 4, 193),
            new Form(MaterialPrefix.plateQuintuple, U * 5, 192),
            new Form(MaterialPrefix.plateDense, U * 9, 191),
            new Form(MaterialPrefix.plateCurved, U, 200),
            new Form(MaterialPrefix.foil, U / 4, 204),
            new Form(MaterialPrefix.scrapGt, U / 9, 188),
            new Form(MaterialPrefix.stick, U / 2, 205),
            new Form(MaterialPrefix.stickLong, U, 206),
            new Form(MaterialPrefix.bolt, U / 8, 207),
            new Form(MaterialPrefix.screw, U / 9, 208),
            new Form(MaterialPrefix.gearGt, U * 4, 216),
            new Form(MaterialPrefix.gearGtSmall, U, 217),
            new Form(MaterialPrefix.ring, U / 4, 210),
            new Form(MaterialPrefix.chain, U, 211),
            new Form(MaterialPrefix.spring, U, 212),
            new Form(MaterialPrefix.springSmall, U / 4, 213),
            new Form(MaterialPrefix.rotor, U * 4 + (U / 4), 224),
            new Form(MaterialPrefix.lens, (U / 4) * 3, 186),
            new Form(MaterialPrefix.round, U / 9, 209),
            new Form(MaterialPrefix.itemCasing, U / 2, 219),
            new Form(MaterialPrefix.wireFine, U / 8, 214),
            new Form(MaterialPrefix.minecartWheels, U, 215),
            new Form(MaterialPrefix.railGt, U / 4, 218),
            new Form(MaterialPrefix.toolHeadSword, U * 2 - (U / 9), 239),
            new Form(MaterialPrefix.toolHeadRawSword, U * 2, 258),
            new Form(MaterialPrefix.toolHeadPickaxe, U * 3 - (U / 9), 240),
            new Form(MaterialPrefix.toolHeadRawPickaxe, U * 3, 259),
            new Form(MaterialPrefix.toolHeadShovel, U - (U / 9), 241),
            new Form(MaterialPrefix.toolHeadRawShovel, U, 260),
            new Form(MaterialPrefix.toolHeadSpade, U - (U / 9), 242),
            new Form(MaterialPrefix.toolHeadRawSpade, U, 261),
            new Form(MaterialPrefix.toolHeadAxe, U * 3 - (U / 9), 243),
            new Form(MaterialPrefix.toolHeadRawAxe, U * 3, 263),
            new Form(MaterialPrefix.toolHeadAxeDouble, U * 5 - 2 * (U / 9), 252),
            new Form(MaterialPrefix.toolHeadRawAxeDouble, U * 5, 264),
            new Form(MaterialPrefix.toolHeadHoe, U * 2 - (U / 9), 244),
            new Form(MaterialPrefix.toolHeadRawHoe, U * 2, 265),
            new Form(MaterialPrefix.toolHeadHammer, U * 6, 247),
            new Form(MaterialPrefix.toolHeadFile, 3 * (U / 2), 233),
            new Form(MaterialPrefix.toolHeadChisel, 3 * (U / 2) - (U / 9), 234),
            new Form(MaterialPrefix.toolHeadRawChisel, 3 * (U / 2), 257),
            new Form(MaterialPrefix.toolHeadSaw, U * 2 - (U / 9), 232),
            new Form(MaterialPrefix.toolHeadRawSaw, U * 2, 256),
            new Form(MaterialPrefix.toolHeadScrewdriver, U, 248),
            new Form(MaterialPrefix.toolHeadUniversalSpade, U - 2 * (U / 9), 253),
            new Form(MaterialPrefix.toolHeadRawUniversalSpade, U - (U / 9), 262),
            new Form(MaterialPrefix.toolHeadSense, U * 3 - (U / 9), 245),
            new Form(MaterialPrefix.toolHeadRawSense, U * 3, 266),
            new Form(MaterialPrefix.toolHeadPlow, U * 4 - (U / 9), 246),
            new Form(MaterialPrefix.toolHeadRawPlow, U * 4, 267),
            new Form(MaterialPrefix.toolHeadBuzzSaw, U * 4, 235),
            new Form(MaterialPrefix.toolHeadArrow, U / 9, 254),
            new Form(MaterialPrefix.toolHeadRawArrow, U / 8, 268));
    private static final List<Form> FORMS = Stream.concat(INITIAL_FORMS.stream(), ADDITIONAL_FORMS.stream()).toList();

    /** Preserved original thirteen contracts, for their already-established input proof. */
    public static List<Form> initialForms() { return INITIAL_FORMS; }
    /** The remaining complete 68-prefix single-primary group; native ingredable proof is separate. */
    public static List<Form> additionalForms() { return ADDITIONAL_FORMS; }
    public static boolean isAdditional(MaterialPrefix prefix) {
        for (Form form : ADDITIONAL_FORMS) if (form.prefix() == prefix) return true;
        return false;
    }

    public static List<Form> forms() { return FORMS; }
    public static Form form(MaterialPrefix prefix) {
        for (Form form : FORMS) if (form.prefix() == prefix) return form;
        return null;
    }

    /** A finite proof boundary. Unreviewed prefixes (including other recyclable forms and the old
     * plain-dust producer) are UNKNOWN here, not banned from the original listener or other owners.
     * OP133 marks oreRaw ORE; OP135-136 mark crushed forms DIRTY; OP158/166 lack RECYCLABLE
     * for impure dust/hot ingot. Quantity mismatches, source secondary materials and unproved
     * prefix identities stay UNKNOWN; neither recoverable=true nor a one-component port
     * composition silently admits those separately scoped cases.
     */
    public static Truth prefixDecision(MaterialPrefix prefix) {
        if (form(prefix) != null) return Truth.YES;
        if (prefix == MaterialPrefix.oreRaw || prefix == MaterialPrefix.crushed
                || prefix == MaterialPrefix.crushedTiny || prefix == MaterialPrefix.dustImpure
                || prefix == MaterialPrefix.ingotHot) return Truth.NO;
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
