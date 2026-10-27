package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreShreddingRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Fixed handler/source evidence contracts; native registration counts are tested separately. */
public final class SourceOreShreddingContracts {
    private SourceOreShreddingContracts() {}
    public static void main(String[] args) {
        run();
        System.out.println("Source ore shredding contracts passed.");
    }
    public static void run() {
        orderedShapesAndCosts();
        requiredForms();
        sourceFacts();
        distinctRockAmount();
    }

    private static void orderedShapesAndCosts() {
        MaterialPrefix d = MaterialPrefix.dust, s = MaterialPrefix.dustSmall;
        MaterialPrefix t = MaterialPrefix.dustTiny, v = MaterialPrefix.dustDiv72;
        equal(List.of(MaterialPrefix.crushed, MaterialPrefix.crushedPurified, MaterialPrefix.crushedCentrifuged,
                MaterialPrefix.crushedTiny, MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.crushedCentrifugedTiny),
                shapes().stream().map(Shape::input).toList(), "six independent ordered source inputs");
        equal(List.of(List.of(d, t, v), List.of(d, s), List.of(d, s, t, v),
                List.of(t, v), List.of(t, v, v), List.of(t, v, v, v)),
                shapes().stream().map(Shape::outputs).toList(), "literal output slots including repeated Div72");
        equal(List.of(18L, 20L, 22L, 2L, 3L, 3L), costs(true, 0), "MORTAR input quality zero");
        equal(List.of(288L, 320L, 352L, 32L, 36L, 40L), costs(false, 0), "non-MORTAR input quality zero");
        equal(List.of(72L, 80L, 88L, 8L, 9L, 10L), costs(true, 3), "NetherQuartz input quality three");
        equal(List.of(54L, 60L, 66L, 6L, 7L, 8L), costs(true, 2), "Nickel input quality two");
        equal(List.of(1L, 1L, 1L, 1L, 1L, 1L), costs(true, -1), "minimum applies after quality multiplication");
        equal(List.of(1L, 1L, 1L, 1L, 1L, 1L), costs(false, -2), "no independent quality-factor clamp");
        equal(288L * (Integer.MAX_VALUE + 1L), duration(shapes().get(0), false, Integer.MAX_VALUE),
                "intermediate numerator cannot wrap for a valid runtime int quality");
        equal(16, POWER, "all source rows use 16 EU/t");
        check(!OPTIMIZE, "explicit Prefix addRecipeX disables optimization");
        for (Shape shape : shapes()) {
            equal(1, shape.inputCount(), "one physical input item");
            equal(1, shape.outputCount(), "one item per output slot, not target amount scaling");
            equal(shape, shape(shape.input()), "bounded lookup");
        }
        equal(null, shape(MaterialPrefix.rockGt), "rock is not a seventh Shredder input");
        equal(null, shape(null), "unreviewed input does not acquire a shape");
        immutable(() -> shapes().clear());
        immutable(() -> shapes().get(5).outputs().clear());
    }
    private static List<Long> costs(boolean mortar, int quality) {
        return shapes().stream().map(shape -> duration(shape, mortar, quality)).toList();
    }

    private static void requiredForms() {
        for (Shape shape : shapes()) {
            var outputs = new ArrayList<>(Collections.nCopies(shape.outputs().size(), Availability.PRESENT));
            equal(Availability.PRESENT, eligible(shape, Decision.YES, Availability.PRESENT, outputs), "all required forms");
            equal(Availability.PRESENT, eligible(shape, Decision.NO, Availability.PRESENT, outputs), "NO means slower handler");
            equal(Availability.UNKNOWN, eligible(shape, Decision.UNKNOWN, Availability.PRESENT, outputs), "unknown MORTAR defers");
            equal(Availability.ABSENT, eligible(shape, Decision.YES, Availability.ABSENT, outputs), "missing actual input");
            equal(Availability.UNKNOWN, eligible(shape, Decision.YES, Availability.UNKNOWN, outputs), "unresolved actual input");
            equal(Availability.ABSENT, eligibility(shape, false, false, true, Decision.YES,
                    Availability.PRESENT, outputs), "invalid source material");
            equal(Availability.ABSENT, eligibility(shape, true, true, true, Decision.YES,
                    Availability.PRESENT, outputs), "antimatter is excluded");
            equal(Availability.ABSENT, eligibility(shape, true, false, false, Decision.YES,
                    Availability.PRESENT, outputs), "invalid pulver target");
            for (int i = 0; i < outputs.size(); i++) {
                outputs.set(i, Availability.ABSENT);
                equal(Availability.ABSENT, eligible(shape, Decision.YES, Availability.PRESENT, outputs),
                        "every fixed output slot is required without fallback");
                equal(Availability.ABSENT, eligible(shape, Decision.UNKNOWN, Availability.UNKNOWN, outputs),
                        "known missing output proves no row even with unresolved other evidence");
                outputs.set(i, Availability.UNKNOWN);
                equal(Availability.UNKNOWN, eligible(shape, Decision.NO, Availability.PRESENT, outputs),
                        "unresolved output cannot be skipped");
                outputs.set(i, Availability.PRESENT);
            }
        }
        // No ORES, hidden, Bedrock or default-generation argument is intentionally accepted.
        // Actual native legacy forms are not prohibited merely because source generation disabled them.
        equal(Decision.YES, SourceOreMacerationFacts.mortar(260, "Iron"), "Iron handler is not a generation blacklist");
        equal(Decision.NO, SourceOreMacerationFacts.mortar(8599, "Bedrock"), "Bedrock chooses slow handler if actual forms exist");
    }
    private static Availability eligible(Shape shape, Decision mortar, Availability input, List<Availability> output) {
        return eligibility(shape, true, false, true, mortar, input, output);
    }

    private static void sourceFacts() {
        int yes = 0, no = 0;
        for (var fact : SourceRecyclingFacts.all()) {
            var id = fact.identity();
            Decision result = SourceOreMacerationFacts.mortar(id.id(), id.name());
            if (fact.portDefaults() == null) {
                equal(Decision.UNKNOWN, result, "target-only Water/Lava not silently included in input fact scope");
            } else {
                check(result != Decision.UNKNOWN, "closed factory/declaration evidence for " + id);
                if (result == Decision.YES) yes++; else no++;
            }
            equal(Decision.UNKNOWN, SourceOreMacerationFacts.mortar(id.id(), "wrong:" + id.name()), "exact canonical-name provenance");
        }
        equal(591, yes, "source-positive identities, not runtime recipe count");
        equal(411, no, "separately reviewed negative identities, not failed native lookups");
        source(Decision.YES, 300, "Zinc"); // Element factory's original <=1200 K argument.
        source(Decision.NO, 210, "Scandium");
        source(Decision.NO, 50, "Boron");
        source(Decision.YES, 40, "Beryllium"); // Explicit tag, not inherited by isotope declarations.
        source(Decision.NO, 41, "Beryllium7");
        source(Decision.YES, 280, "Nickel");
        source(Decision.YES, 8348, "ChargedCertusQuartz");
        source(Decision.YES, 9000, "Pumice"); // stonecent -> stone closure.
        source(Decision.YES, 8267, "WoodPolished");
        source(Decision.YES, 9823, "SodiumSulfide"); // dustdcmp -> dust closure.
        source(Decision.NO, 8000, "SiliconDioxide"); // oredustdcmp is not dustdcmp.
        source(Decision.NO, 8300, "Diamond");
        source(Decision.NO, 8713, "GlowstoneRefined"); // setAllToTheOutputOf does not copy MORTAR.
        source(Decision.NO, 8714, "ObsidianRefined");
        source(Decision.NO, 8752, "Tritanium"); // Optional thermal phase uncertainty is unrelated.
        source(Decision.UNKNOWN, 9516, "Superconductor");
        source(Decision.UNKNOWN, 10280, "Nickel");
        source(Decision.UNKNOWN, 280, "Ni");
        source(Decision.UNKNOWN, 280, null);
        for (String name : List.of("Cinnabar", "Copper", "Galena", "NetherQuartz", "Chalcopyrite", "Pentlandite",
                "Sphalerite", "Tetrahedrite", "Pyrite", "Nickel", "Cooperite", "YellowLimonite")) {
            var fact = SourceRecyclingFacts.all().stream().filter(f -> f.identity().name().equals(name)).findFirst().orElseThrow();
            source(Decision.YES, fact.identity().id(), name);
        }
    }
    private static void source(Decision expected, int id, String name) {
        equal(expected, SourceOreMacerationFacts.mortar(id, name), "source MORTAR " + id + "/" + name);
    }

    private static void distinctRockAmount() {
        long rock = U * 9 / 4;
        equal(36L, duration(rock, 0, 16, 0), "rock cost has no MORTAR requirement");
        equal(72L, duration(rock, 0, 16, 1), "rock input quality, not target quality");
        equal(rock / 2, PulverizationRules.amount(rock, U / 2), "rock scales pulver amount before OM selection");
        equal(rock * 2, PulverizationRules.amount(rock, U * 2), "rock doubles target amount");
        equal(0L, PulverizationRules.amount(rock, 0), "rock remains can be disabled by zero amount");
        // Six explicit Shredder descriptors expose no amount-dependent count: target identity and
        // actual stacks are native concerns. The native fixture exercises 0, U/2 and 2U targets.
        equal(1, shape(MaterialPrefix.crushed).outputCount(), "fixed Shredder output differs from rock remains");
    }
    private static void immutable(Runnable action) {
        try { action.run(); throw new AssertionError("Expected immutable source descriptor"); }
        catch (UnsupportedOperationException expected) { }
    }
}
