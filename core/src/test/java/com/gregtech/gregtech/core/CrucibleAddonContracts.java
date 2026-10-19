package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.addon.CrucibleReactionDeclaration;
import com.gregtech.gregtech.api.addon.CrucibleReactionRemoval;
import com.gregtech.gregtech.api.addon.MaterialComposition;
import com.gregtech.gregtech.api.addon.SmeltingTargetDeclaration;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.machine.crucible.CrucibleReactions;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialDefinition;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.ModReferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Crucible composition, smelting targets, reactions and removals. Each scenario is its own JVM. */
public final class CrucibleAddonContracts {
    private static int assertions;

    private CrucibleAddonContracts() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "crucible" -> crucible();
            case "cruciblecore" -> coreComposition();
            case "crucibleunknown" -> unknown();
            case "crucibledup" -> duplicate();
            case "cruciblesteal" -> steal();
            case "crucibleearly" -> early();
            case "crucibleself" -> selfInput();
            case "cruciblebad" -> badDeclaration();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void crucible() {
        prepare();
        var warnings = new ArrayList<String>();
        GTMaterialRegistry.setLogSink((warning, message) -> { if (warning) warnings.add(message); });
        long unit = GTValues.U;
        AddonMaterials.submit("aa_crucible", List.of(
                MaterialDefinition.builder(1, "SampleAlloy").displayName("Sample Alloy").color(0x8A9BB0)
                        .meltingPointKelvin(450).boilingPointKelvin(2000).density(7F)
                        .properties(AddonMaterials.metalFlags()).build(),
                MaterialDefinition.builder(1, "SampleCrucibleAlloy").displayName("Sample Crucible Alloy").color(0xC4A574)
                        .meltingPointKelvin(1400).boilingPointKelvin(2500).density(8F)
                        .properties(AddonMaterials.metalFlags()).build(),
                MaterialDefinition.builder(1, "SampleMix").displayName("Sample Mix").color(0x668866)
                        .meltingPointKelvin(600).boilingPointKelvin(1200).density(3F).dust().build()),
                List.of(), null,
                List.of(new MaterialComposition("SampleCrucibleAlloy", 4, List.of(
                                new MaterialComposition.Part("SampleAlloy", 3 * unit),
                                new MaterialComposition.Part("Copper", unit)), true),
                        new MaterialComposition("SampleMix", 1, List.of(
                                new MaterialComposition.Part("Carbon", unit)), false)),
                List.of(new SmeltingTargetDeclaration("SampleAlloy", "SampleAlloy", unit)),
                List.of(new CrucibleReactionDeclaration("SampleCrucibleAlloy", 2, List.of(
                        new CrucibleReactionDeclaration.Part("SampleAlloy", 1),
                        new CrucibleReactionDeclaration.Part("Tin", 1)))),
                List.of(new CrucibleReactionRemoval("StainlessSteel",
                                List.of("WroughtIron", "Nichrome", "Chromium", "Manganese")),
                        new CrucibleReactionRemoval("Tin", List.of("Stone"))));
        GTMaterialRegistry.init();
        check(CrucibleReactions.recipes().size() == 46, "explicit built-in rows stay 46");
        var rows = CrucibleReactions.allRecipes();
        check(rows.size() == 138, "46 explicit, 91 composition, plus 2 addon rows, minus stainless: expected 138, actual "
                + rows.size());
        check(rows.stream().filter(row -> row.output().getName().equals("SampleCrucibleAlloy")).count() == 2,
                "the alloy has the composition row and the explicit row");
        check(rows.stream().noneMatch(row -> row.output().getName().equals("SampleMix")),
                "a composition without the crucible flag adds no reaction");
        check(rows.stream().noneMatch(row -> row.output().getName().equals("StainlessSteel")
                        && inputNames(row).equals(Set.of("WroughtIron", "Nichrome", "Chromium", "Manganese"))),
                "the wrought-iron stainless row is removed");
        GTMaterial alloy = GTMaterialRegistry.get("SampleCrucibleAlloy");
        var composition = alloy.getCompositionComponents();
        check(alloy.getCompositionDivider() == 4 && composition.size() == 2
                        && composition.get(0).material().getName().equals("SampleAlloy")
                        && composition.get(0).amount() == 3 * unit
                        && composition.get(1).material().getName().equals("Copper")
                        && composition.get(1).amount() == unit,
                "the alloy composition is stored in order");
        GTMaterial mix = GTMaterialRegistry.get("SampleMix");
        check(mix.hasComposition() && mix.getCompositionComponents().size() == 1, "the mix keeps its composition");
        GTMaterial sample = GTMaterialRegistry.get("SampleAlloy");
        check(sample.has(MaterialProperty.MELTING) && sample.getTargetSmeltingMaterial() == sample
                        && sample.getTargetSmeltingAmount() == unit,
                "a declared smelting target marks the material as melting");
        check(!alloy.has(MaterialProperty.MELTING), "an omitted smelting target does not add the melting flag");
        var mixed = stacks(sample, 3 * unit, GTMaterialRegistry.get("Copper"), unit);
        check(CrucibleReactions.react(mixed, 1400) && mixed.size() == 1 && amount(mixed, alloy) == 4 * unit,
                "3 sample alloy and 1 copper yield 4 crucible alloy");
        var alternate = stacks(GTMaterialRegistry.get("SampleAlloy"), unit, GTMaterialRegistry.get("Tin"), unit);
        check(CrucibleReactions.react(alternate, 1400) && amount(alternate, alloy) == 2 * unit,
                "the explicit row yields 2");
        var bronze = stacks(GTMaterialRegistry.get("Copper"), 3 * unit, GTMaterialRegistry.get("Tin"), unit);
        check(CrucibleReactions.react(bronze, 1357) && amount(bronze, GTMaterialRegistry.get("Bronze")) == 4 * unit,
                "copper and tin still make bronze");
        check(warnings.stream().anyMatch(message -> message.contains("removed crucible reaction")
                        && message.contains("StainlessSteel")),
                "a removal is logged");
        check(warnings.stream().anyMatch(message -> message.contains("matched nothing") && message.contains("Tin")),
                "a removal that hits nothing warns");
        check(warnings.stream().noneMatch(message -> message.contains("can take that reaction over")),
                "distinct input rows do not warn about taking over");
    }

    private static void coreComposition() {
        prepare();
        AddonMaterials.submit("aa_core", List.of(), List.of(), null,
                List.of(new MaterialComposition("Iron", 1, List.of(
                        new MaterialComposition.Part("Carbon", GTValues.U)), false)),
                List.of(), List.of(), List.of());
        expectThrows("cannot set the composition", "Iron", "aa_core");
    }

    private static void unknown() {
        prepare();
        AddonMaterials.submit("aa_unknown", List.of(), List.of(), null, List.of(), List.of(),
                List.of(new CrucibleReactionDeclaration("NoSuchMetal", 1, List.of(
                        new CrucibleReactionDeclaration.Part("Iron", 1)))),
                List.of());
        expectThrows("unknown material", "NoSuchMetal", "aa_unknown");
    }

    private static void duplicate() {
        prepare();
        AddonMaterials.submit("aa_dup", List.of(), List.of(), null, List.of(), List.of(),
                List.of(new CrucibleReactionDeclaration("Bronze", 4, List.of(
                        new CrucibleReactionDeclaration.Part("AnnealedCopper", 3),
                        new CrucibleReactionDeclaration.Part("Tin", 1)))),
                List.of());
        expectThrows("duplicates", "built-in", "aa_dup");
    }

    private static void steal() {
        prepare();
        var warnings = new ArrayList<String>();
        GTMaterialRegistry.setLogSink((warning, message) -> { if (warning) warnings.add(message); });
        AddonMaterials.submit("aa_steal", List.of(), List.of(), null, List.of(), List.of(),
                List.of(new CrucibleReactionDeclaration("Bronze", 8, List.of(
                        new CrucibleReactionDeclaration.Part("AnnealedCopper", 3),
                        new CrucibleReactionDeclaration.Part("Tin", 1)))),
                List.of());
        GTMaterialRegistry.init();
        check(CrucibleReactions.allRecipes().size() == 138, "a higher yield is kept and the built-in row stays");
        check(warnings.stream().anyMatch(message -> message.contains("can take that reaction over")
                        && message.contains("aa_steal")),
                "the same inputs with a different yield warn");
    }

    private static void early() {
        expectThrows(IllegalStateException.class, CrucibleReactions::allRecipes,
                "Crucible reactions were read before addon linking finished");
    }

    private static void selfInput() {
        prepare();
        AddonMaterials.submit("aa_self", List.of(MaterialDefinition.builder(1, "SampleAlloy").dust().build()),
                List.of(), null, List.of(), List.of(),
                List.of(new CrucibleReactionDeclaration("SampleAlloy", 1, List.of(
                        new CrucibleReactionDeclaration.Part("SampleAlloy", 1)))),
                List.of());
        expectThrows("also an input", "SampleAlloy", "aa_self");
    }

    private static void badDeclaration() {
        expectThrows(IllegalArgumentException.class,
                () -> new MaterialComposition.Part("Copper", 1), "multiple of U");
        expectThrows(IllegalArgumentException.class,
                () -> new CrucibleReactionDeclaration("Bronze", 0, List.of(
                        new CrucibleReactionDeclaration.Part("Tin", 1))),
                "positive");
        expectThrows(IllegalArgumentException.class,
                () -> new CrucibleReactionDeclaration("Bronze", 1, List.of()), "needs inputs");
    }

    private static Set<String> inputNames(CrucibleReactions.Reaction reaction) {
        var names = new java.util.HashSet<String>();
        for (CrucibleReactions.Part part : reaction.parts()) names.add(part.material().resolve().getName());
        return names;
    }

    private static ArrayList<CrucibleMaterialStack> stacks(GTMaterial left, long leftAmount,
                                                           GTMaterial right, long rightAmount) {
        return new ArrayList<>(List.of(CrucibleMaterialStack.of(left, leftAmount),
                CrucibleMaterialStack.of(right, rightAmount)));
    }

    private static long amount(List<CrucibleMaterialStack> contents, GTMaterial material) {
        return contents.stream().filter(stack -> stack.material == material).mapToLong(stack -> stack.amount).sum();
    }

    private static void prepare() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
    }

    private static void expectThrows(String... parts) {
        expectThrows(IllegalStateException.class, GTMaterialRegistry::init, String.join(" ", parts));
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(message, failure);
            String text = failure.getMessage() == null ? "" : failure.getMessage();
            for (String part : message.split(" ")) {
                if (!text.contains(part)) throw new AssertionError(message + " but was: " + text, failure);
            }
            return;
        }
        throw new AssertionError(message);
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
