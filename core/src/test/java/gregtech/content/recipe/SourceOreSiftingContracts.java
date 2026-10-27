package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialItemDefinitions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static gregtech.content.recipe.SourceOreSiftingRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Pure descriptors/conditions; actual native holders and admission are separate runtime contracts. */
public final class SourceOreSiftingContracts {
    private SourceOreSiftingContracts() {}
    public static void run() {
        genericRules();
        rosterAndDefinitions();
        gradeSpecials();
        fossilSpecials();
        quartzSpecials();
        immutableSlots();
    }
    private static void genericRules() {
        equal(PRESENT, eligibility(true, false, false, PRESENT, PRESENT, PRESENT, PRESENT),
                "actual ordinary gem permits a generic row without a generation-flag argument");
        equal(ABSENT, eligibility(false, false, false, PRESENT, PRESENT, PRESENT, PRESENT), "invalid material");
        equal(ABSENT, eligibility(true, true, false, PRESENT, PRESENT, PRESENT, PRESENT), "antimatter");
        equal(ABSENT, eligibility(true, false, true, PRESENT, PRESENT, PRESENT, PRESENT), "Bedrock");
        for (int missing = 0; missing < 4; missing++) {
            var forms = new SourceOreWashingRules.Availability[]{PRESENT, PRESENT, PRESENT, PRESENT};
            forms[missing] = ABSENT;
            equal(ABSENT, eligibility(true, false, false, forms[0], forms[1], forms[2], forms[3]),
                    "each actual dust/purified/purifiedTiny/gem prerequisite " + missing);
            forms[missing] = UNKNOWN;
            equal(UNKNOWN, eligibility(true, false, false, forms[0], forms[1], forms[2], forms[3]),
                    "unknown form is not proven present " + missing);
        }
        equal(ABSENT, eligibility(true, false, false, UNKNOWN, PRESENT, PRESENT, ABSENT),
                "known absent ordinary gem dominates unknown dust");
        equal(List.of(new GenericOutput(MaterialPrefix.gemLegendary, 1, 8),
                new GenericOutput(MaterialPrefix.gemExquisite, 1, 4),
                new GenericOutput(MaterialPrefix.gemFlawless, 1, 2),
                new GenericOutput(MaterialPrefix.gem, 1, 0),
                new GenericOutput(MaterialPrefix.gemFlawed, 2, 1),
                new GenericOutput(MaterialPrefix.gemChipped, 4, 1),
                new GenericOutput(MaterialPrefix.dust, 1, 0)), genericOutputs(), "literal generic ladder/fallbacks");
        equal(List.of(9L, 90L, 360L, 1350L, 1800L, 3600L, 4500L), Size.WHOLE.genericChances(), "whole chances");
        equal(List.of(1L, 10L, 40L, 150L, 200L, 400L, 500L), Size.TINY.genericChances(), "tiny chances divide nine");
    }
    private static void rosterAndDefinitions() {
        equal(22, specialRows().size(), "all fixed calls precede the separate generic producer");
        equal(List.of(9103, 500, 9108, 8219, 9163, 8348, 8347, 8346, 8457, 8445, 8389),
                specialRows().stream().filter(row -> row.size() == Size.WHOLE).map(row -> row.input().id()).toList(),
                "source identity order, including Tin rather than a Sn name alias");
        equal(List.of(284, 285, 286, 287, 288, 289, 292, 293, 294, 295, 298, 299, 300, 301,
                302, 303, 304, 305, 306, 307, 308, 309), specialRows().stream().map(SpecialRow::sourceLine).toList(),
                "source call order");
        Set<String> defined = MaterialItemDefinitions.all().stream()
                .map(d -> d.material().getName() + "/" + d.prefix().getName()).collect(Collectors.toSet());
        for (int index = 0; index < specialRows().size(); index++) {
            var row = specialRows().get(index);
            equal(index % 2 == 0 ? Size.WHOLE : Size.TINY, row.size(), "independent whole/tiny call order");
            equal(row.size() == Size.WHOLE ? MaterialPrefix.crushedPurified : MaterialPrefix.crushedPurifiedTiny,
                    row.size().input(), "literal direct input prefix");
            equal(1, row.inputCount(), "one direct input, not nine tiny");
            equal(row.size() == Size.WHOLE ? 144 : 16, row.ticks(), "source duration");
            equal(16, row.power(), "source power"); check(row.optimize(), "source optimize=true");
            material(row.input());
            check(defined.contains(row.input().name() + "/" + row.size().input().getName()), "default input definition");
            for (Output output : row.outputs()) {
                if (output instanceof FormOutput form) definedForm(defined, form);
                if (output instanceof FossilOutput fossil) definedForm(defined, fossil.fallback());
            }
        }
        // Fixed special membership is independent of the generic own-gem/dust/paired-input gate.
        for (String name : List.of("Asbestos", "Tin", "Cassiterite", "Bone", "Dolomite")) {
            check(!defined.contains(name + "/gem"), "no default own gem for " + name);
            equal(2L, specialRows().stream().filter(row -> row.input().name().equals(name)).count(),
                    "both independent special descriptors still exist for " + name);
        }
    }
    private static void material(Material binding) {
        var material = GTMaterialRegistry.get(binding.id());
        equal(binding.name(), material.getName(), "exact current canonical identity");
        check(binding.matches(material.getId(), material.getName()), "exact identity match");
        check(!binding.matches(material.getId() + 10000, material.getName()), "no addon ID substitution");
        check(!binding.matches(material.getId(), "alias:" + material.getName()), "no name alias substitution");
    }
    private static void definedForm(Set<String> definitions, FormOutput form) {
        material(form.material());
        check(definitions.contains(form.material().name() + "/" + form.prefix().getName()), "default output definition " + form);
    }
    private static void gradeSpecials() {
        for (var row : specialRows().subList(0, 6)) {
            Material target = row.input().name().equals("Asbestos") ? new Material(8321, "Jade") : new Material(8419, "Zircon");
            equal(List.of(new FormOutput(row.input(), MaterialPrefix.crushedCentrifugedTiny, row.size() == Size.WHOLE ? 9 : 1),
                    new FormOutput(target, MaterialPrefix.gemExquisite, 1), new FormOutput(target, MaterialPrefix.gemFlawless, 1),
                    new FormOutput(target, MaterialPrefix.gem, 1), new FormOutput(target, MaterialPrefix.gemFlawed, 2),
                    new FormOutput(target, MaterialPrefix.gemChipped, 4), new FormOutput(target, MaterialPrefix.dust, 1)),
                    row.outputs(), "seven DIRECT outputs, no legendary or fallback alternatives");
            equal(row.size() == Size.WHOLE ? List.of(10000L, 90L, 360L, 1350L, 1800L, 3600L, 4500L)
                    : List.of(10000L, 10L, 40L, 150L, 200L, 400L, 500L), row.chances(), "grade special chances");
        }
    }
    private static void fossilSpecials() {
        equal(FossilChoice.PROVIDER, fossilChoice(PRESENT), "trusted present optional piece");
        equal(FossilChoice.FALLBACK, fossilChoice(ABSENT), "known absent provider");
        equal(FossilChoice.DEFER, fossilChoice(UNKNOWN), "unknown is not an absent-provider guess");
        equal(FossilChoice.DEFER, fossilChoice(null), "unsupplied evidence is unknown");
        for (var row : specialRows().subList(6, 10)) {
            equal(12, row.outputs().size(), "twelve fossil slots");
            equal(new FormOutput(row.input(), MaterialPrefix.crushedCentrifugedTiny, row.size() == Size.WHOLE ? 9 : 1),
                    row.outputs().get(0), "fossil main remains input material");
            equal(new VanillaOutput(VanillaItem.SKELETON_SKULL, 1), row.outputs().get(1), "old skull meta0 is skeleton");
            equal(new VanillaOutput(VanillaItem.BONE, 1), row.outputs().get(2), "vanilla bone");
            var boneDust = new FormOutput(new Material(8219, "Bone"), MaterialPrefix.dust, 1);
            equal(boneDust, row.outputs().get(3), "Bone dust even for Dolomite");
            var mixed = new ArrayList<String>();
            for (int index = 0; index < 8; index++) {
                var piece = (FossilOutput) row.outputs().get(index + 4);
                equal(index, piece.index(), "independent provider identity index");
                equal(boneDust, piece.fallback(), "each optional piece has its own Bone-dust1 fallback");
                mixed.add(fossilChoice(index == 0 || index == 3 || index == 7 ? PRESENT : ABSENT)
                        == FossilChoice.PROVIDER ? "explicit:" + piece.index() : piece.fallback().material().name());
            }
            equal(List.of("explicit:0", "Bone", "Bone", "explicit:3", "Bone", "Bone", "Bone", "explicit:7"), mixed,
                    "controlled optional identities do not alter neighboring fallback slots");
            equal(row.size() == Size.WHOLE ? List.of(10000L, 360L, 3600L, 3600L, 3600L, 3600L, 3600L, 3600L, 3600L, 3600L, 3600L, 3600L)
                    : List.of(10000L, 40L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L),
                    row.chances(), "fossil exact independent chances");
        }
    }
    private static void quartzSpecials() {
        for (var row : specialRows().subList(10, 22)) {
            boolean certus = row.input().name().equals("CertusQuartz") || row.input().name().equals("ChargedCertusQuartz");
            int gems = certus ? 2 : 1;
            int fines = row.size() == Size.WHOLE ? 9 : 1;
            equal(gems + fines, row.outputs().size(), "whole nine independent tiny outputs, tiny one");
            equal(gems + 9, row.chances().size(), "surplus tiny source chances are not truncated");
            Material dust = certus ? new Material(8347, "CertusQuartz") : row.input();
            equal(new FormOutput(certus ? new Material(8348, "ChargedCertusQuartz") : row.input(), MaterialPrefix.gem, 1),
                    row.outputs().get(0), "first gem identity");
            if (certus) equal(new FormOutput(dust, MaterialPrefix.gem, 1), row.outputs().get(1), "second Certus gem");
            for (int index = gems; index < row.outputs().size(); index++)
                equal(new FormOutput(dust, MaterialPrefix.dustTiny, 1), row.outputs().get(index), "separate direct dustTiny1");
            long high = row.size() == Size.WHOLE ? 7200L : 800L, low = row.size() == Size.WHOLE ? 900L : 100L;
            boolean ordinaryCertus = row.input().name().equals("CertusQuartz");
            equal(ordinaryCertus ? low : high, row.chances().get(0), "first gem chance");
            if (certus) equal(ordinaryCertus ? high : low, row.chances().get(1), "second gem chance");
            check(row.chances().subList(gems, row.chances().size()).stream().allMatch(chance -> chance == 5000L),
                    "all tiny-dust chance entries remain 5000");
        }
    }
    private static void immutableSlots() {
        var original = specialRows().get(0);
        var outputs = new ArrayList<>(original.outputs()); var chances = new ArrayList<>(original.chances());
        var copy = new SpecialRow(original.input(), original.size(), outputs, chances, original.sourceLine());
        outputs.remove(1); chances.remove(1);
        equal(7, copy.outputs().size(), "descriptor keeps every output index independent of native availability");
        equal(90L, copy.chances().get(1), "missing native output must not shift later chance positions");
        equal(MaterialPrefix.gemFlawless, ((FormOutput) copy.outputs().get(2)).prefix(), "third slot remains flawless");
        boolean immutable = false;
        try { copy.outputs().clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "immutable fixed output slots");
    }
}
