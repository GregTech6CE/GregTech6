package gregtech.core;

import gregtech.internal.addon.AddonMaterials;
import gregtech.api.addon.AddonMaterialContributor;
import gregtech.api.addon.MaterialDeclaration;
import gregtech.api.addon.MaterialFormRequest;
import gregtech.api.addon.SmeltingTargetRemoval;
import gregtech.api.addon.CrucibleContent;
import gregtech.api.addon.CrucibleReactionDeclaration;
import gregtech.api.addon.CrucibleReactionRemoval;
import gregtech.api.addon.MaterialComposition;
import gregtech.api.addon.SmeltingTargetDeclaration;
import gregtech.internal.machine.crucible.CrucibleMaterialStack;
import gregtech.internal.machine.crucible.CrucibleInputRules;
import gregtech.internal.machine.crucible.CrucibleProcess;
import gregtech.internal.machine.crucible.CrucibleReactions;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.internal.material.MaterialDefinition;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.mod.ModData;
import gregtech.internal.prefix.PrefixRegistry;
import gregtech.content.data.ModReferences;
import gregtech.content.data.MaterialPrefix;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Crucible composition, smelting targets, reactions and removals. Each scenario is its own JVM. */
public final class CrucibleAddonContracts {
    private static int assertions;

    private CrucibleAddonContracts() {}

    public static void main(String[] args) throws Exception {
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
            case "smeltingremoval" -> smeltingRemoval();
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
        check(!CrucibleContent.EMPTY.present(), "an empty crucible view has no material");
        var view = new CrucibleContent("SampleAlloy", 2 * unit, 1400);
        check(view.present() && view.amount() == 2 * unit && view.temperature() == 1400,
                "a crucible view keeps its material, amount and temperature");
        check(new CrucibleContent(null, -1, -1).equals(CrucibleContent.EMPTY),
                "a view with no material and no positive amount is empty");
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

    /** Runs the supported public service path, rather than mutating a core material after freeze. */
    private static void smeltingRemoval() throws Exception {
        prepare();
        Path root = Files.createTempDirectory("gt-smelting-removal");
        Path service = root.resolve("META-INF/services/" + AddonMaterialContributor.class.getName());
        Files.createDirectories(service.getParent());
        Files.writeString(service, SmeltingRemovalContributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                CrucibleAddonContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        } finally {
            Files.deleteIfExists(service);
            Files.deleteIfExists(service.getParent());
            Files.deleteIfExists(service.getParent().getParent());
            Files.deleteIfExists(root);
        }
        GTMaterialRegistry.init();
        GTMaterial source = GTMaterialRegistry.get("RemovalAuditSource");
        GTMaterial target = GTMaterialRegistry.get("RemovalAuditTarget");
        GTMaterial positive = GTMaterialRegistry.get("RemovalAuditPositive");
        GTMaterial unmapped = GTMaterialRegistry.get("RemovalAuditUnmapped");
        for (GTMaterial material : List.of(source, target, positive, unmapped)) {
            check(material.isValid() && "aa_smelting_removal_audit".equals(AddonMaterials.owner(material))
                            && !GTMaterialRegistry.isCoreMaterial(material),
                    "the service must register its own isolated material: " + material.getName());
        }
        check(source.getTargetSmeltingMaterial() == source && source.getTargetSmeltingAmount() == 0
                        && !source.has(MaterialProperty.MELTING),
                "the public removal must override the earlier declared smelting target");
        check(CrucibleInputRules.smeltingPreview(source, MaterialPrefix.dust) == null,
                "an explicitly removed target must not advertise a smelting recipe");
        check(positive.getTargetSmeltingMaterial() == target && positive.getTargetSmeltingAmount() == GTValues.U2
                        && positive.has(MaterialProperty.MELTING),
                "the positive-control smelting declaration must still be active");
        check(unmapped.getTargetSmeltingMaterial() == unmapped && unmapped.getTargetSmeltingAmount() == GTValues.U,
                "an omitted declaration must retain the default identity mapping");

        var crossing = new ArrayList<>(List.of(CrucibleInputRules.materialItem(source, MaterialPrefix.dust)));
        checkRetained(crossing, source, GTValues.U, 399, 398, false, "below melting");
        checkRetained(crossing, source, GTValues.U, 400, 399, false, "crossing melting");
        checkRetained(crossing, source, GTValues.U, 500, 400, false, "remaining hot");
        checkRetained(crossing, source, GTValues.U, 399, 500, false, "cooling below melting");
        checkRetained(crossing, source, GTValues.U, 500, 399, false, "reheating");

        // An already hot crucible must also retain newly admitted content, including fractional units.
        long exactAmount = GTValues.U + 1;
        var incoming = new ArrayList<>(List.of(CrucibleMaterialStack.of(source, exactAmount)));
        checkRetained(incoming, source, exactAmount, 500, 500, true, "new hot content");
        checkRetained(incoming, source, exactAmount, 500, 500, false, "stable hot content");

        var ordinary = new ArrayList<>(List.of(CrucibleMaterialStack.of(positive, GTValues.U)));
        CrucibleProcess.process(ordinary, 500, 399, true, false);
        check(ordinary.size() == 1 && ordinary.get(0).material == target && ordinary.get(0).amount == GTValues.U2,
                "a positive target must still convert at the declared fractional yield");
        var defaultMapping = new ArrayList<>(List.of(CrucibleMaterialStack.of(unmapped, GTValues.U)));
        checkRetained(defaultMapping, unmapped, GTValues.U, 500, 399, true, "omitted target");

        // Removal disables conversion, not normal phase hazards.
        var boiling = new ArrayList<>(List.of(CrucibleMaterialStack.of(source, exactAmount)));
        var vapor = CrucibleProcess.process(boiling, 1200, 1199, false, false);
        check(boiling.isEmpty() && vapor.fizzCount() == 1 && vapor.vapors().size() == 1
                        && vapor.vapors().get(0).material() == source && vapor.vapors().get(0).amount() == exactAmount
                        && !vapor.acidDestroyedHull() && vapor.explosiveAmount() == 0,
                "normal boiling must still vaporize the retained material at its boiling point");
        check(source.getTargetSmeltingAmount() == 0 && !source.has(MaterialProperty.MELTING)
                        && CrucibleInputRules.smeltingPreview(source, MaterialPrefix.dust) == null,
                "processing must not restore a removed target or its recipe/display flag");
    }

    private static void checkRetained(List<CrucibleMaterialStack> content, GTMaterial material, long expectedAmount,
                                      long temperature, long previousTemperature, boolean newContent, String phase) {
        var result = CrucibleProcess.process(content, temperature, previousTemperature, newContent, false);
        check(content.size() == 1 && content.get(0).material == material && content.get(0).amount == expectedAmount,
                "smelting target removal must retain material identity and amount: " + phase);
        check(result.fizzCount() == 0 && result.vapors().isEmpty()
                        && !result.acidDestroyedHull() && result.explosiveAmount() == 0,
                "safe retained material must not produce hazards: " + phase);
    }

    public static final class SmeltingRemovalContributor implements AddonMaterialContributor {
        @Override public String addonId() { return "aa_smelting_removal_audit"; }
        @Override public List<MaterialDeclaration> materials() {
            return List.of(removalMaterial("RemovalAuditSource"), removalMaterial("RemovalAuditTarget"),
                    removalMaterial("RemovalAuditPositive"), removalMaterial("RemovalAuditUnmapped"));
        }
        @Override public List<MaterialFormRequest> forms() { return List.of(); }
        @Override public List<SmeltingTargetDeclaration> smeltingTargets() {
            return List.of(new SmeltingTargetDeclaration("RemovalAuditSource", "RemovalAuditTarget", GTValues.U),
                    new SmeltingTargetDeclaration("RemovalAuditPositive", "RemovalAuditTarget", GTValues.U2));
        }
        @Override public List<SmeltingTargetRemoval> smeltingTargetRemovals() {
            return List.of(new SmeltingTargetRemoval("RemovalAuditSource"));
        }
        private static MaterialDeclaration removalMaterial(String name) {
            return MaterialDeclaration.builder(name).dust().meltingPointKelvin(400)
                    .boilingPointKelvin(1200).density(3F).build();
        }
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
        gregtech.content.data.MaterialGroups.Glowstone.getClass();
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
