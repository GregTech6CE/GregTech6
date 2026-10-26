package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gregtech.content.recipe.SourceOreWashingRules.*;
import static gregtech.content.recipe.SourceOreWashingData.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Dependency-free contracts: source facts and pure rules, not native registration coverage. */
public final class SourceOreWashingContracts {
    private SourceOreWashingContracts() {}
    public static void main(String[] args) {
        gregtech.api.mod.ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        run();
        System.out.println("Ore washing source contracts passed (no native runtime coverage).");
    }
    public static void run() {
        sourceFacts();
        sourceGates();
        exactQuantitiesAndShapes();
        directFallback();
        orderedSelection();
        reviewedCandidateCases();
    }

    private static void sourceFacts() {
        equal(168, bindings().size(), "59 positive plus 109 bounded source-negative identities");
        equal(168, new HashSet<>(bindings().stream().map(Binding::materialId).toList()).size(), "unique IDs");
        equal(59L, bindings().stream().filter(b -> !b.tags().isEmpty()).count(), "positive identities");
        equal(20L, bindings().stream().filter(b -> b.tags().contains(Tag.MERCURY)).count(), "Mercury tags");
        equal(12L, bindings().stream().filter(b -> b.tags().contains(Tag.PERSULFATE)).count(), "Persulfate tags");
        equal(29L, bindings().stream().filter(b -> b.tags().contains(Tag.FIRESTONE)).count(), "Firestone tags");
        for (Binding binding : bindings()) {
            GTMaterial current = GTMaterialRegistry.get(binding.materialId());
            check(current != null, "source material exists " + binding.materialId());
            equal(binding.materialName(), current.getName(), "exact canonical source identity");
            check(binding.sourceLine() > 0, "declaration provenance");
            for (Tag tag : Tag.values()) {
                equal(binding.tags().contains(tag) ? Decision.YES : Decision.NO,
                        decision(current.getId(), current.getName(), tag), "reviewed membership");
                equal(Decision.UNKNOWN, decision(current.getId(), "alias:" + current.getName(), tag), "names are exact");
                equal(Decision.UNKNOWN, decision(current.getId() + 10000, current.getName(), tag), "IDs are exact");
            }
        }
        for (int id : new int[]{220, 230, 240, 400, 410, 420, 720, 730, 740}) {
            GTMaterial material = GTMaterialRegistry.get(id);
            equal(Decision.YES, decision(id, material.getName(), Tag.FIRESTONE), "refractmetal inherited tag");
        }
        equal(Set.of(Tag.MERCURY, Tag.PERSULFATE), expectedTags(300, "Zinc"), "Zinc has two distinct tags");
        equal(Decision.NO, decision(9114, "Cinnabar", Tag.MERCURY), "source NO, not chemical intuition");
        equal(Decision.NO, decision(8342, "Firestone", Tag.FIRESTONE), "reagent is not washing target tag");
        equal(Decision.UNKNOWN, decision(8213, "Cryotheum", Tag.MERCURY), "unreviewed supplemental identity");
        equal(null, expectedTags(10000, "Gold"), "addon ID does not inherit name-based facts");
        immutable(() -> bindings().clear());
        immutable(() -> expectedTags(300, "Zinc").clear());
    }

    private static void sourceGates() {
        equal(Availability.PRESENT, oresEligibility(true, false, false,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "all actual forms present");
        for (int position = 0; position < 3; position++) {
            Availability[] forms = {Availability.PRESENT, Availability.PRESENT, Availability.PRESENT};
            forms[position] = Availability.ABSENT;
            equal(Availability.ABSENT, oresEligibility(true, false, false, forms[0], forms[1], forms[2]),
                    "dust and both crushed forms required independently");
            forms[position] = Availability.UNKNOWN;
            equal(Availability.UNKNOWN, oresEligibility(true, false, false, forms[0], forms[1], forms[2]),
                    "unknown actual registration must defer");
        }
        equal(Availability.ABSENT, oresEligibility(false, false, false,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "invalid excluded");
        equal(Availability.ABSENT, oresEligibility(true, true, false,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "antimatter excluded");
        equal(Availability.ABSENT, oresEligibility(true, false, true,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "Bedrock excluded");
        equal(Availability.ABSENT, requireAll(Availability.UNKNOWN, Availability.ABSENT), "known failed gate");
        equal(Availability.PRESENT, petroEligibility(true, false, Availability.PRESENT),
                "Petrotheum only needs this prefix; no dust, paired prefix or Bedrock gate");
        equal(Availability.ABSENT, petroEligibility(true, true, Availability.PRESENT), "Petrotheum antimatter gate");
        equal(Availability.UNKNOWN, petroEligibility(true, false, Availability.UNKNOWN), "unknown prefix defers");
        equal(Availability.ABSENT, petroEligibility(true, false, Availability.ABSENT), "absent prefix skips only itself");
    }

    private static void exactQuantitiesAndShapes() {
        equal(16, SLUICE_POWER, "Sluice and Petrotheum power");
        equal(0, BATH_POWER, "Bath zero power");
        equal(List.of(10000L, 300L, 300L, 300L, 300L, 300L, 300L, 300L, 300L),
                SLUICE_CHANCES, "main + eight separate 3 percent source slots");
        equal(List.of(10000L, 5000L, 10000L), PETROTHEUM_CHANCES, "Petrotheum main/bonus/sand");
        equal(List.of(2, 2, 2, 2, 1), WHOLE_BATH_BYPRODUCT_COUNTS, "no merging five guaranteed slots");
        equal(List.of(1), TINY_BATH_BYPRODUCT_COUNTS, "tiny guaranteed slot");
        for (Size size : Size.values()) {
            boolean whole = size == Size.WHOLE;
            equal(whole ? MaterialPrefix.crushed : MaterialPrefix.crushedTiny, size.input(), "direct input");
            equal(whole ? MaterialPrefix.crushedPurified : MaterialPrefix.crushedPurifiedTiny,
                    size.purified(), "preferred main form");
            equal(whole ? MaterialPrefix.dust : MaterialPrefix.dustTiny, size.sand(), "SluiceSand form");
            equal(whole ? 9 : 1, size.byproductCount(), "whole/tiny bonus count");
            equal(whole ? 900 : 100, size.waterMb(), "water and Sluice output mB");
            equal(whole ? 144 : 16, size.mercuryMb(), "Mercury mB");
            equal(whole ? 45 : 5, size.petrotheumMb(), "Petrotheum mB");
            equal(whole ? 144 : 16, size.ticks(), "literal duration");
            equal(whole ? 144000 : 16000, size.steamMb(), "steam160 per water");
        }
        equal(List.of(
                new BathShape(MaterialPrefix.crushed, 1, MaterialPrefix.dust, 1, Size.WHOLE),
                new BathShape(MaterialPrefix.crushedTiny, 9, MaterialPrefix.dust, 1, Size.WHOLE),
                new BathShape(MaterialPrefix.crushed, 1, MaterialPrefix.dustTiny, 9, Size.WHOLE),
                new BathShape(MaterialPrefix.crushedTiny, 1, MaterialPrefix.dustTiny, 1, Size.TINY)),
                bathShapes(), "four source shapes; no invented 9tiny+9tiny shape");
        check(WATER_OPTIMIZE && BATH_OPTIMIZE && REAGENT_BATH_OPTIMIZE, "source optimize=true");
        check(!PETROTHEUM_OPTIMIZE, "listener optimize=false");
        equal(List.of(new DustForm(MaterialPrefix.dust, 1), new DustForm(MaterialPrefix.dustSmall, 4),
                new DustForm(MaterialPrefix.dustTiny, 9), new DustForm(MaterialPrefix.dustDiv72, 64)),
                sandForms(Size.WHOLE), "OM dust whole sand falls through with source bindStack clamp");
        equal(List.of(new DustForm(MaterialPrefix.dustTiny, 1), new DustForm(MaterialPrefix.dustDiv72, 8)),
                sandForms(Size.TINY), "OM dust tiny sand fallback");
        immutable(() -> sandForms(Size.WHOLE).clear());
        immutable(() -> bathShapes().clear());
    }

    private static void directFallback() {
        for (Size size : Size.values()) {
            List<MaterialPrefix> order = size == Size.WHOLE
                    ? List.of(MaterialPrefix.crushedPurified, MaterialPrefix.crushedCentrifuged, MaterialPrefix.dust)
                    : List.of(MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.crushedCentrifugedTiny, MaterialPrefix.dustTiny);
            for (int target = 0; target < 3; target++) {
                final int selected = target;
                List<MaterialPrefix> queried = new ArrayList<>();
                FormSelection result = purifiedPrefix(size, prefix -> {
                    queried.add(prefix);
                    return order.indexOf(prefix) == selected ? Availability.PRESENT : Availability.ABSENT;
                });
                equal(new FormSelection(Availability.PRESENT, order.get(target)), result, "direct same-count fallback");
                equal(order.subList(0, target + 1), queried, "stop after first actual registration");
            }
            equal(new FormSelection(Availability.UNKNOWN, null), purifiedPrefix(size,
                    prefix -> prefix == order.get(0) ? Availability.UNKNOWN : Availability.PRESENT),
                    "unknown higher-priority form cannot silently fall through");
            equal(new FormSelection(Availability.ABSENT, null), purifiedPrefix(size, prefix -> Availability.ABSENT),
                    "no form, no fake dust conversion");
        }
    }

    private static void orderedSelection() {
        Map<String, Decision> tags = Map.of("no", Decision.NO, "absent", Decision.YES,
                "first", Decision.YES, "second", Decision.YES, "self", Decision.YES, "unknown", Decision.UNKNOWN);
        var tiny = (java.util.function.Function<String, Availability>) value ->
                value.equals("absent") ? Availability.ABSENT : Availability.PRESENT;
        equal(new Selection<>(Availability.PRESENT, "first"), selectByproduct("self",
                List.of("no", "absent", "first", "second"), tags::get, tiny), "first YES with valid OM tiny wins");
        equal(new Selection<>(Availability.PRESENT, "self"), selectByproduct("self", List.of("no", "absent"),
                tags::get, tiny), "self only after every byproduct");
        equal(new Selection<>(Availability.UNKNOWN, null), selectByproduct("self", List.of("unknown", "first"),
                tags::get, tiny), "unknown tag before winner defers");
        equal(new Selection<>(Availability.PRESENT, "first"), selectByproduct("self", List.of("unknown", "first"),
                tags::get, value -> value.equals("unknown") ? Availability.ABSENT : Availability.PRESENT),
                "known absent OM tiny disqualifies even an unknown-tag candidate");
        equal(new Selection<>(Availability.UNKNOWN, null), selectByproduct("self", List.of("first", "second"),
                tags::get, value -> Availability.UNKNOWN), "unknown OM tiny before winner defers");
        equal(new Selection<>(Availability.PRESENT, "first"), selectByproduct("self", List.of("first", "unknown"),
                tags::get, tiny), "unknown after first winner is irrelevant");
        List<String> moreThanEight = new ArrayList<>();
        for (int i = 0; i < 10; i++) moreThanEight.add("no");
        moreThanEight.add("first");
        equal("first", selectByproduct("self", moreThanEight, tags::get, tiny).material(),
                "reagent scans all byproducts, not Sluice eight-slot limit");
        equal(new Selection<>(Availability.ABSENT, null), selectByproduct("no", List.of("no", "absent"),
                tags::get, tiny), "all known no/absent means no row");
        equal(List.of("first", "first", "self", "self", "self", "self", "self", "self"),
                waterByproducts("self", List.of("first", "first")), "water duplicates and self padding");
        equal(8, waterByproducts("self", moreThanEight).size(), "water only first eight");
        immutable(() -> waterByproducts("self", List.of()).clear());
    }

    private static void reviewedCandidateCases() {
        int reviewed = 0;
        for (String row : CASES) {
            String[] fields = row.split(":");
            GTMaterial source = GTMaterialRegistry.get(Integer.parseInt(fields[0]));
            Tag tag = Tag.valueOf(fields[1]);
            int selectedId = Integer.parseInt(fields[2]);
            Selection<GTMaterial> selected = selectByproduct(source, source.getByProducts(),
                    candidate -> decision(candidate.getId(), candidate.getName(), tag), candidate -> Availability.PRESENT);
            equal(Availability.PRESENT, selected.availability(), "bounded source-negative evidence resolves " + row);
            equal(selectedId, selected.material().getId(), "ordered source candidate " + row);
            reviewed++;
        }
        equal(159, reviewed, "reviewed positive material/tag cases; availability still tested separately");
    }

    private static void immutable(Runnable mutation) {
        try { mutation.run(); throw new AssertionError("immutable source data"); }
        catch (UnsupportedOperationException expected) { }
    }

    // Independently captured source-tag candidates from the pinned material/byproduct directory.
    private static final String[] CASES = {
        "30:MERCURY:8029",
        "31:MERCURY:31",
        "160:PERSULFATE:9130",
        "220:FIRESTONE:220",
        "230:FIRESTONE:230",
        "240:FIRESTONE:9113",
        "250:FIRESTONE:9113",
        "270:PERSULFATE:9115",
        "278:PERSULFATE:9115",
        "280:MERCURY:9116",
        "280:PERSULFATE:9115",
        "290:MERCURY:790",
        "290:PERSULFATE:9115",
        "300:MERCURY:300",
        "300:PERSULFATE:300",
        "310:MERCURY:300",
        "310:PERSULFATE:300",
        "320:FIRESTONE:9113",
        "340:PERSULFATE:9130",
        "400:FIRESTONE:720",
        "410:FIRESTONE:410",
        "420:FIRESTONE:9128",
        "470:MERCURY:9210",
        "480:PERSULFATE:9130",
        "500:FIRESTONE:9123",
        "500:MERCURY:9212",
        "510:MERCURY:300",
        "510:PERSULFATE:300",
        "720:FIRESTONE:8419",
        "730:FIRESTONE:730",
        "740:FIRESTONE:740",
        "750:FIRESTONE:9123",
        "760:MERCURY:780",
        "770:MERCURY:780",
        "780:MERCURY:780",
        "780:PERSULFATE:280",
        "790:MERCURY:790",
        "790:PERSULFATE:290",
        "791:MERCURY:791",
        "820:MERCURY:470",
        "830:FIRESTONE:9196",
        "8014:FIRESTONE:9120",
        "8016:PERSULFATE:9115",
        "8026:FIRESTONE:8026",
        "8029:MERCURY:30",
        "8234:FIRESTONE:8234",
        "8303:FIRESTONE:9113",
        "8340:PERSULFATE:290",
        "8341:MERCURY:790",
        "8346:MERCURY:8747",
        "8368:MERCURY:790",
        "8369:MERCURY:790",
        "8370:MERCURY:790",
        "8371:MERCURY:790",
        "8372:MERCURY:790",
        "8419:FIRESTONE:720",
        "8420:FIRESTONE:8419",
        "8420:PERSULFATE:9156",
        "8421:FIRESTONE:8419",
        "8456:MERCURY:790",
        "8457:MERCURY:8747",
        "8461:FIRESTONE:8461",
        "8462:FIRESTONE:8462",
        "8600:MERCURY:790",
        "8610:PERSULFATE:290",
        "8620:MERCURY:300",
        "8620:PERSULFATE:290",
        "8649:MERCURY:780",
        "8649:PERSULFATE:280",
        "8676:MERCURY:470",
        "8677:MERCURY:790",
        "8678:MERCURY:780",
        "8736:FIRESTONE:9217",
        "8736:MERCURY:9212",
        "8744:FIRESTONE:9195",
        "8744:MERCURY:8747",
        "8747:FIRESTONE:8419",
        "8747:MERCURY:8747",
        "8747:PERSULFATE:9145",
        "8750:FIRESTONE:9246",
        "8750:MERCURY:8029",
        "8750:PERSULFATE:9115",
        "8761:PERSULFATE:9156",
        "8762:FIRESTONE:9113",
        "8763:FIRESTONE:8234",
        "8765:PERSULFATE:9115",
        "8767:PERSULFATE:9145",
        "8769:MERCURY:8769",
        "8769:PERSULFATE:9132",
        "8770:MERCURY:300",
        "8770:PERSULFATE:300",
        "8774:PERSULFATE:9115",
        "8776:FIRESTONE:9217",
        "8779:MERCURY:8779",
        "8785:PERSULFATE:9132",
        "8786:FIRESTONE:9195",
        "8786:PERSULFATE:9145",
        "9003:MERCURY:790",
        "9004:MERCURY:790",
        "9104:FIRESTONE:9120",
        "9105:FIRESTONE:9120",
        "9106:PERSULFATE:9156",
        "9107:PERSULFATE:9156",
        "9108:FIRESTONE:9123",
        "9108:MERCURY:9212",
        "9109:PERSULFATE:9115",
        "9111:MERCURY:790",
        "9111:PERSULFATE:9115",
        "9113:FIRESTONE:9113",
        "9113:MERCURY:9210",
        "9115:PERSULFATE:270",
        "9116:MERCURY:9116",
        "9116:PERSULFATE:280",
        "9117:MERCURY:470",
        "9117:PERSULFATE:9130",
        "9118:MERCURY:9212",
        "9118:PERSULFATE:280",
        "9120:FIRESTONE:9120",
        "9122:MERCURY:790",
        "9123:FIRESTONE:9128",
        "9124:FIRESTONE:9123",
        "9126:FIRESTONE:9113",
        "9126:MERCURY:9210",
        "9128:FIRESTONE:9123",
        "9130:MERCURY:300",
        "9130:PERSULFATE:300",
        "9132:MERCURY:300",
        "9132:PERSULFATE:290",
        "9133:FIRESTONE:9133",
        "9133:MERCURY:470",
        "9136:FIRESTONE:9128",
        "9137:PERSULFATE:280",
        "9143:FIRESTONE:8234",
        "9145:MERCURY:9212",
        "9145:PERSULFATE:9115",
        "9146:MERCURY:8029",
        "9148:FIRESTONE:9246",
        "9149:MERCURY:8029",
        "9156:PERSULFATE:290",
        "9192:FIRESTONE:8419",
        "9193:FIRESTONE:9133",
        "9194:FIRESTONE:9133",
        "9195:FIRESTONE:9217",
        "9196:FIRESTONE:9133",
        "9197:FIRESTONE:9133",
        "9201:MERCURY:9210",
        "9207:MERCURY:9210",
        "9210:FIRESTONE:9113",
        "9210:MERCURY:470",
        "9210:PERSULFATE:9211",
        "9211:MERCURY:300",
        "9211:PERSULFATE:300",
        "9212:MERCURY:9116",
        "9212:PERSULFATE:290",
        "9216:FIRESTONE:9195",
        "9216:MERCURY:790",
        "9217:FIRESTONE:9133",
        "9246:FIRESTONE:9148",
        "9247:FIRESTONE:9246",
    };
}
