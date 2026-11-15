package gregtech.content.recipe;

import gregtech.api.addon.material.*;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;

import java.util.*;

import static gregtech.content.recipe.SpecialProcessData.Phase;
import static gregtech.content.recipe.SpecialProcessData.Tail;

/** Attaches built-in purified-acid methods as ordinary typed material process rules. */
public final class SpecialProcessMaterialFacts {
    private static final FormKey PURIFIED = form("crushedPurified");
    private static final FormKey PURIFIED_TINY = form("crushedPurifiedTiny");
    private static final FormKey CENTRIFUGED = form("crushedCentrifuged");
    private static final FormKey CENTRIFUGED_TINY = form("crushedCentrifugedTiny");
    private static final Map<FormKey, Integer> INPUT_COUNTS = Map.of(PURIFIED, 1, PURIFIED_TINY, 9);

    private SpecialProcessMaterialFacts() {}

    public static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        var processes = new LinkedHashMap<>(declaration.info().processes().rules());
        attachReagents(declaration.name(), processes);
        attachFixedTargets(declaration, processes);
        attachStaticAquaRegiaTargets(declaration.name(), processes);
        attachManganeseTarget(declaration, processes);
        if (processes.equals(declaration.info().processes().rules())) return declaration;
        var info = declaration.info();
        return new MaterialDefinition.Declaration(
                declaration.material(),
                new MaterialInfoDefinition(
                        info.tooling(),
                        info.composition(),
                        info.relations(),
                        new MaterialInfoDefinition.Processes(processes)));
    }

    private static void attachReagents(
            String name, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> processes) {
        if (name.equals("SulfuricAcid")) reagent(processes, SpecialProcessMethods.SULFURIC_ACID_LEACHING, "liquid");
        if (name.equals("AquaRegia")) reagent(processes, SpecialProcessMethods.AQUA_REGIA_LEACHING, "liquid");
        if (name.equals("HydrogenFluoride"))
            reagent(processes, SpecialProcessMethods.HYDROFLUORIC_ACID_LEACHING, "gas");
        if (name.equals("HydrochloricAcid"))
            reagent(processes, SpecialProcessMethods.HYDROCHLORIC_ACID_MANGANESE_LEACHING, "gas");
    }

    private static void attachFixedTargets(
            MaterialDefinition.Declaration declaration,
            Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> processes) {
        for (var family : SpecialProcessData.families()) {
            var member = family.members().stream()
                    .filter(identity -> identity.matches(declaration.material().id(), declaration.name()))
                    .findFirst()
                    .orElse(null);
            if (member == null) continue;
            var method = method(family.fluidInput().phase());
            if (method == null) continue;
            processes
                    .computeIfAbsent(method, ignored -> new ArrayList<>())
                    .add(new MaterialProcessRule<>(
                            INPUT_COUNTS.keySet(), fixedTarget(new MaterialKey(declaration.name()), family)));
        }
    }

    private static SpecialProcessValue.Target fixedTarget(MaterialKey target, SpecialProcessData.Family family) {
        var itemOutputs = new ArrayList<SpecialProcessValue.ItemOutput>();
        itemOutputs.add(new SpecialProcessValue.ItemOutput(
                target, List.of(new SpecialProcessValue.FormCount(CENTRIFUGED, 1)), 10_000));
        MaterialKey tail = switch (family.tail()) {
            case SELF -> target;
            case PLATINUM_GROUP_SLUDGE -> new MaterialKey(SpecialProcessData.PGS.name());
            case GRAPHITE -> new MaterialKey(SpecialProcessData.GRAPHITE.name());
        };
        int tailCount = family.tail() == Tail.PLATINUM_GROUP_SLUDGE ? 8 : 2;
        for (int index = 0; index < 5; index++)
            itemOutputs.add(new SpecialProcessValue.ItemOutput(
                    tail, List.of(new SpecialProcessValue.FormCount(CENTRIFUGED_TINY, tailCount)), 5_000));
        return new SpecialProcessValue.Target(
                INPUT_COUNTS,
                itemOutputs,
                family.fluidOutputs().stream()
                        .map(SpecialProcessMaterialFacts::fluidOutput)
                        .toList(),
                family.fluidInput().millibuckets(),
                256,
                0);
    }

    private static void attachManganeseTarget(
            MaterialDefinition.Declaration declaration,
            Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> processes) {
        var material = declaration.material();
        if (material.properties().contains(gregtech.api.material.MaterialProperty.ANTIMATTER)) return;
        boolean mnDioxideByproduct = declaration.info().processes().find(MaterialProcessKeys.ORE_PROCESSING).stream()
                .map(MaterialProcessRule::value)
                .anyMatch(rule -> rule.byproducts().stream()
                        .anyMatch(byproduct -> byproduct.name().equals("Pyrolusite")));
        if (!mnDioxideByproduct) return;
        var target = new SpecialProcessValue.Target(
                INPUT_COUNTS,
                List.of(
                        new SpecialProcessValue.ItemOutput(
                                new MaterialKey(declaration.name()),
                                List.of(new SpecialProcessValue.FormCount(CENTRIFUGED, 1)),
                                10_000),
                        new SpecialProcessValue.ItemOutput(
                                new MaterialKey(SpecialProcessData.MNCL2.name()),
                                List.of(
                                        new SpecialProcessValue.FormCount(form("dustSmall"), 6),
                                        new SpecialProcessValue.FormCount(form("dustTiny"), 13),
                                        new SpecialProcessValue.FormCount(form("dustDiv72"), 64)),
                                10_000)),
                List.of(
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("Water"),
                                List.of(new SpecialProcessValue.FormCount(form("liquid"), 1)),
                                3_000),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("Chlorine"),
                                List.of(new SpecialProcessValue.FormCount(form("gas"), 1)),
                                1_000)),
                4_000,
                256,
                0);
        processes
                .computeIfAbsent(
                        SpecialProcessMethods.HYDROCHLORIC_ACID_MANGANESE_LEACHING, ignored -> new ArrayList<>())
                .add(new MaterialProcessRule<>(INPUT_COUNTS.keySet(), target));
    }

    private static void attachStaticAquaRegiaTargets(
            String name, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> processes) {
        SpecialProcessValue.Target target = switch (name) {
            case "Gold" ->
                aquaRegiaTarget(
                        "Gold",
                        "Copper",
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("ChloroauricAcid"),
                                List.of(new SpecialProcessValue.FormCount(form("liquid"), 1)),
                                3_000),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("NitrogenMonoxide"),
                                List.of(new SpecialProcessValue.FormCount(form("gas"), 1)),
                                1_000),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("Water"),
                                List.of(new SpecialProcessValue.FormCount(form("liquid"), 1)),
                                3_000),
                        6_500);
            case "Copper" ->
                aquaRegiaTarget(
                        "Copper",
                        "Cobalt",
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("ChloroauricAcid"),
                                List.of(new SpecialProcessValue.FormCount(form("liquid"), 1)),
                                3_000),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("NitrogenMonoxide"),
                                List.of(new SpecialProcessValue.FormCount(form("gas"), 1)),
                                1_000),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("Water"),
                                List.of(new SpecialProcessValue.FormCount(form("liquid"), 1)),
                                3_000),
                        6_500);
            case "Tin", "Cassiterite" ->
                aquaRegiaTarget(
                        name,
                        "Zinc",
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("StannicChloride"),
                                List.of(new SpecialProcessValue.FormCount(form("liquid"), 1)),
                                2_500),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("HydrochloricAcid"),
                                List.of(new SpecialProcessValue.FormCount(form("gas"), 1)),
                                4_000),
                        new SpecialProcessValue.FluidOutput(
                                new MaterialKey("NitrogenDioxide"),
                                List.of(new SpecialProcessValue.FormCount(form("gas"), 1)),
                                3_000),
                        13_000);
            default -> null;
        };
        if (target == null) return;
        processes
                .computeIfAbsent(SpecialProcessMethods.AQUA_REGIA_LEACHING, ignored -> new ArrayList<>())
                .add(new MaterialProcessRule<>(INPUT_COUNTS.keySet(), target));
    }

    private static SpecialProcessValue.Target aquaRegiaTarget(
            String target,
            String byproduct,
            SpecialProcessValue.FluidOutput first,
            SpecialProcessValue.FluidOutput second,
            SpecialProcessValue.FluidOutput third,
            int acid) {
        var items = new ArrayList<SpecialProcessValue.ItemOutput>();
        items.add(new SpecialProcessValue.ItemOutput(
                new MaterialKey(target), List.of(new SpecialProcessValue.FormCount(CENTRIFUGED, 1)), 10_000));
        for (int index = 0; index < 5; index++)
            items.add(new SpecialProcessValue.ItemOutput(
                    new MaterialKey(byproduct),
                    List.of(new SpecialProcessValue.FormCount(CENTRIFUGED_TINY, 2)),
                    5_000));
        return new SpecialProcessValue.Target(INPUT_COUNTS, items, List.of(first, second, third), acid, 256, 0);
    }

    private static SpecialProcessValue.FluidOutput fluidOutput(SpecialProcessData.Fluid fluid) {
        FormKey outputForm = form(fluid.phase().state() == SpecialProcessData.State.GAS ? "gas" : "liquid");
        return new SpecialProcessValue.FluidOutput(
                new MaterialKey(fluid.phase().material().name()),
                List.of(new SpecialProcessValue.FormCount(outputForm, 1)),
                Math.toIntExact(fluid.millibuckets()));
    }

    private static MaterialProcessKey<SpecialProcessValue> method(Phase phase) {
        return switch (phase) {
            case SULFURICACID -> SpecialProcessMethods.SULFURIC_ACID_LEACHING;
            case AQUAREGIA -> SpecialProcessMethods.AQUA_REGIA_LEACHING;
            case HYDROGENFLUORIDE -> SpecialProcessMethods.HYDROFLUORIC_ACID_LEACHING;
            case HYDROCHLORICACID -> SpecialProcessMethods.HYDROCHLORIC_ACID_MANGANESE_LEACHING;
            default -> null;
        };
    }

    private static void reagent(
            Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> processes,
            MaterialProcessKey<SpecialProcessValue> method,
            String form) {
        processes
                .computeIfAbsent(method, ignored -> new ArrayList<>())
                .add(new MaterialProcessRule<>(Set.of(form(form)), new SpecialProcessValue.Reagent()));
    }

    private static FormKey form(String name) {
        return new FormKey(name);
    }
}
