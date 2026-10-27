package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MaterialPrefixes;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialBlockDefinitions;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.ArrayList;
import java.util.List;

import static gregtech.content.recipe.SourceOreMagneticRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceOreWashingRules.FormSelection;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Pure source contracts; the native fixtures separately verify admitted recipes and reload ownership. */
public final class SourceOreMagneticContracts {
    private SourceOreMagneticContracts() {}
    public static void run() {
        gates();
        orderedSlots();
        fallbackAndQuantities();
        physicalRows();
    }

    private static void gates() {
        equal(Availability.PRESENT, eligibility(true, false, false,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "actual dust and both purified inputs");
        for (int index = 0; index < 3; index++) {
            Availability[] forms = {Availability.PRESENT, Availability.PRESENT, Availability.PRESENT};
            forms[index] = Availability.ABSENT;
            equal(Availability.ABSENT, eligibility(true, false, false, forms[0], forms[1], forms[2]),
                    "each source form is independently required");
            forms[index] = Availability.UNKNOWN;
            equal(Availability.UNKNOWN, eligibility(true, false, false, forms[0], forms[1], forms[2]),
                    "unresolved input evidence cannot authorize a row");
        }
        equal(Availability.ABSENT, eligibility(false, false, false,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "invalid source");
        equal(Availability.ABSENT, eligibility(true, true, false,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "antimatter excluded");
        equal(Availability.ABSENT, eligibility(true, false, true,
                Availability.PRESENT, Availability.PRESENT, Availability.PRESENT), "generic Bedrock excluded");
        equal(Availability.ABSENT, eligibility(true, false, false,
                Availability.ABSENT, Availability.UNKNOWN, Availability.PRESENT), "known absent gate dominates unknown");
    }

    private static void orderedSlots() {
        // Local, unregistered fixtures: no global magnetic facts or material directory mutations.
        GTMaterial plain = material(15000, "Plain");
        GTMaterial first = material(15001, "MagneticFirst").put(MaterialProperty.MAGNETIC);
        GTMaterial second = material(15002, "MagneticSecond").put(MaterialProperty.MAGNETIC);
        GTMaterial sameClass = material(15003, "NonMagneticByproduct");
        plain.ores(sameClass, first, first, second);
        equal(List.of(first, first, second), OreSourcePolicy.magneticCandidates(plain),
                "opposite magnetic class, declaration order and duplicate positions unchanged");
        equal(List.of(first, first, second, plain, plain), byproducts(plain, OreSourcePolicy.magneticCandidates(plain)),
                "first five opposite candidates, self padding instead of cycling");
        first.ores(second, sameClass, plain);
        equal(List.of(sameClass, plain), OreSourcePolicy.magneticCandidates(first),
                "magnetic input selects only nonmagnetic byproducts");
        equal(List.of(), byproducts(plain, List.of()), "empty opposite list means no generic magnetic row");
        equal(List.of(1, 2, 3, 4, 5), byproducts(0, List.of(1, 2, 3, 4, 5, 6, 7)), "only first five candidates");
        var mutable = new ArrayList<>(List.of(first));
        var selected = byproducts(plain, mutable);
        mutable.clear();
        equal(List.of(first, plain, plain, plain, plain), selected, "selection is immutable snapshot");
        immutable(() -> selected.clear());
    }

    private static GTMaterial material(int id, String name) {
        return new GTMaterial(id, name, name, 0xFFFFFF);
    }

    private static void fallbackAndQuantities() {
        equal(16, POWER, "literal native recipe power");
        check(OPTIMIZE, "source optimize=true");
        equal(List.of(10000L, 600L, 600L, 600L, 600L, 600L), CHANCES, "six independent source slots");
        for (Size size : Size.values()) {
            boolean whole = size == Size.WHOLE;
            equal(whole ? MaterialPrefix.crushedPurified : MaterialPrefix.crushedPurifiedTiny, size.input(), "direct input");
            equal(whole ? MaterialPrefix.crushedCentrifuged : MaterialPrefix.crushedCentrifugedTiny,
                    size.refined(), "preferred main output");
            equal(whole ? MaterialPrefix.dust : MaterialPrefix.dustTiny, size.dust(), "same-size direct dust fallback");
            equal(whole ? 18 : 2, size.byproductCount(), "each of five tiny outputs");
            equal(whole ? 144 : 16, size.ticks(), "no tool-quality multiplier");
            List<MaterialPrefix> queried = new ArrayList<>();
            equal(new FormSelection(Availability.PRESENT, size.refined()), refinedPrefix(size, prefix -> {
                queried.add(prefix); return Availability.PRESENT;
            }), "present refined form wins");
            equal(List.of(size.refined()), queried, "no lower-priority lookup after present form");
            equal(new FormSelection(Availability.PRESENT, size.dust()), refinedPrefix(size,
                    prefix -> prefix == size.refined() ? Availability.ABSENT : Availability.PRESENT),
                    "same-count dust fallback, not OM.dust mass conversion");
            equal(new FormSelection(Availability.UNKNOWN, null), refinedPrefix(size,
                    prefix -> prefix == size.refined() ? Availability.UNKNOWN : Availability.PRESENT),
                    "unknown refined form cannot silently become dust");
            equal(new FormSelection(Availability.ABSENT, null), refinedPrefix(size, prefix -> Availability.ABSENT),
                    "absent output is a source empty slot; does not change input eligibility");
        }
    }

    private static void physicalRows() {
        MaterialPrefixes.bootstrap();
        equal(List.of(new Material(8102, "SluiceSand"), new Material(8599, "Bedrock"), new Material(8514, "MoonTurf")),
                blockRows().stream().map(BlockRow::input).toList(), "all and only three direct physical rows");
        List<List<String>> names = List.of(
                List.of("Stone", "Iron", "Neodymium", "Nickel", "Cobalt", "Manganese"),
                List.of("Deepslate", "Adamantine", "Atlarus", "RareEarth", "Neodymium", "VanadiumPentoxide"),
                List.of("Basalt", "MeteoricIron", "Neodymium", "Nickel", "Cobalt", "Manganese"));
        List<List<Integer>> counts = List.of(List.of(9, 2, 1, 1, 1, 1), List.of(9, 9, 9, 18, 27, 27), List.of(9, 9, 1, 1, 1, 1));
        List<List<Long>> chances = List.of(List.of(9640L, 5832L, 5832L, 5832L, 5832L, 5832L),
                List.of(7000L, 3000L, 3000L, 3000L, 3000L, 3000L), List.of(3000L, 972L, 972L, 972L, 972L, 972L));
        for (int index = 0; index < 3; index++) {
            BlockRow row = blockRows().get(index);
            equal(1, row.inputCount(), "physical block count, not dust9");
            equal(1296, row.ticks(), "direct block duration");
            equal(index == 1 ? 64 : 16, row.power(), "Bedrock uses64, other blocks16");
            check(row.optimize(), "direct source optimization flag");
            equal(names.get(index), row.outputs().stream().map(out -> out.material().name()).toList(), "ordered direct products");
            equal(counts.get(index), row.outputs().stream().map(Output::count).toList(), "literal output counts");
            equal(chances.get(index), row.chances(), "literal source probabilities");
            var material = GTMaterialRegistry.get(row.input().id());
            check(row.input().matches(material.getId(), material.getName()), "input exact ID and canonical name");
            check(!row.input().matches(material.getId() + 10000, material.getName()), "no ID substitution");
            check(!row.input().matches(material.getId(), "alias"), "no name substitution");
            equal(1L, MaterialBlockDefinitions.all().stream().filter(definition ->
                    definition.prefix() == BlockMaterialPrefix.blockDust && definition.material() == material).count(),
                    "one core physical definition; actual holder availability checked natively");
            for (int slot = 0; slot < row.outputs().size(); slot++) {
                Output output = row.outputs().get(slot);
                var outputMaterial = GTMaterialRegistry.get(output.material().id());
                equal(output.material().name(), outputMaterial.getName(), "output exact canonical identity");
                equal(slot == 0 ? MaterialPrefix.dust : MaterialPrefix.dustTiny, output.prefix(), "direct prefix, no OM fallback");
            }
            immutable(() -> row.outputs().clear());
            immutable(() -> row.chances().clear());
        }
        // Bedrock is excluded only from the generic loop, not its explicit physical-block recipe.
        check(blockRows().stream().anyMatch(row -> row.input().id() == 8599), "special Bedrock survives generic gate");
        immutable(() -> blockRows().clear());
    }

    private static void immutable(Runnable mutation) {
        try { mutation.run(); throw new AssertionError("immutable source data"); }
        catch (UnsupportedOperationException expected) { }
    }
}
