package gregtech.engine.material;

import gregtech.api.addon.AddonNames;
import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialFormRequest;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.material.CrushingMaterialHelper;
import gregtech.api.material.FuelMaterialHelper;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialToolProfile;
import gregtech.api.material.OreProcessingMaterialHelper;
import gregtech.api.material.PulverizationMaterialHelper;
import gregtech.api.material.SmeltingMaterialHelper;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Independent indexes for auxiliary material facts, keyed by the canonical material identity. */
public final class MaterialInfoRegistry implements MaterialInfoTool.Host {
    private static final MaterialInfoRegistry HOST = new MaterialInfoRegistry();

    private MaterialInfoRegistry() {}

    private static final Map<String, MaterialToolProfile> TOOLS = new LinkedHashMap<>();
    private static final Map<String, MaterialInfoDefinition.Composition> COMPOSITIONS = new LinkedHashMap<>();
    private static final Map<String, Boolean> COMPOSITION_CRUCIBLE = new LinkedHashMap<>();
    private static final Map<String, MaterialInfoDefinition.Relations> RELATIONS = new LinkedHashMap<>();
    private static final Map<String, MaterialInfoDefinition.Processes> PROCESSES = new LinkedHashMap<>();
    private static final List<ToolSubmission> ADDON_TOOLS = new ArrayList<>();
    private static final List<CompositionSubmission> ADDON_COMPOSITIONS = new ArrayList<>();
    private static final List<FormSubmission> ADDON_FORMS = new ArrayList<>();
    private static final List<TagSubmission> ADDON_TAGS = new ArrayList<>();
    private static final List<ProcessSubmission> ADDON_PROCESSES = new ArrayList<>();
    private static boolean registrationsClosed;
    private static volatile Snapshot snapshot;

    public static MaterialInfoTool.Host host() {
        return HOST;
    }

    public static MaterialInfoTool.Registrar addonRegistrar(String addonId) {
        return HOST.registrar(addonId);
    }

    public record FormSubmission(String addonId, MaterialKey material, FormKey form) {}

    public record TagSubmission(String addonId, MaterialKey material, gregtech.api.addon.material.DataKey tag) {}

    public record ProcessSubmission(
            String addonId, MaterialKey material, MaterialProcessKey<?> process, MaterialProcessRule<?> rule) {}

    private record ToolSubmission(String addonId, MaterialKey material, MaterialToolProfile profile) {}

    private record CompositionSubmission(String addonId, MaterialKey material, MaterialComposition composition) {}

    private record Snapshot(
            Map<String, MaterialToolProfile> tools,
            Map<String, MaterialInfoDefinition.Composition> compositions,
            Map<String, Boolean> compositionCrucible,
            Map<String, MaterialInfoDefinition.Relations> relations,
            Map<String, MaterialInfoDefinition.Processes> processes) {}

    static void install(MaterialDefinition.Declaration declaration) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        String key = declaration.name();
        var info = declaration.info();
        if (TOOLS.putIfAbsent(key, info.toolProfile()) != null)
            throw new IllegalArgumentException("Duplicate material tool profile: " + key);
        if (COMPOSITIONS.putIfAbsent(key, info.composition()) != null)
            throw new IllegalArgumentException("Duplicate material composition: " + key);
        COMPOSITION_CRUCIBLE.put(key, false);
        if (RELATIONS.putIfAbsent(key, info.relations()) != null)
            throw new IllegalArgumentException("Duplicate material relations: " + key);
        if (PROCESSES.putIfAbsent(key, info.processes()) != null)
            throw new IllegalArgumentException("Duplicate material process rules: " + key);
    }

    static void validateInstall(MaterialDefinition.Declaration declaration) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        String key = declaration.name();
        if (TOOLS.containsKey(key)
                || COMPOSITIONS.containsKey(key)
                || COMPOSITION_CRUCIBLE.containsKey(key)
                || RELATIONS.containsKey(key)
                || PROCESSES.containsKey(key))
            throw new IllegalArgumentException("Duplicate material auxiliary declaration: " + key);
    }

    static void freeze() {
        if (snapshot != null) return;
        registrationsClosed = true;
        captureLinkedFacts();
        snapshot = new Snapshot(
                Map.copyOf(TOOLS),
                Map.copyOf(COMPOSITIONS),
                Map.copyOf(COMPOSITION_CRUCIBLE),
                Map.copyOf(RELATIONS),
                Map.copyOf(PROCESSES));
    }

    /** Preserve the legacy SPI's crucible permission separately from its final linked composition. */
    public static void recordLinkedComposition(GTMaterial material, boolean crucible) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        COMPOSITION_CRUCIBLE.put(material.resolve().getName(), crucible);
    }

    private static MaterialToolProfile linkedTool(GTMaterial material) {
        return new MaterialToolProfile(
                material.getToolTypes(),
                material.getToolSpeed(),
                material.getToolDurability(),
                material.getToolQuality());
    }

    private static void captureLinkedFacts() {
        // Legacy callbacks still write the runtime model until the remaining domains migrate.
        // Publish their final results, including implicit defaults and explicit target removals.
        for (ToolSubmission submission : ADDON_TOOLS) {
            GTMaterial material = requireOwned(submission.addonId(), submission.material(), "tool profile");
            if (!linkedTool(material).equals(submission.profile()))
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' tool profile for '"
                        + material.getName() + "' conflicts with the linked profile");
        }
        for (String name : TOOLS.keySet()) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (!material.isValid()) continue;
            TOOLS.put(name, linkedTool(material));
            COMPOSITIONS.put(
                    name,
                    new MaterialInfoDefinition.Composition(
                            material.getCompositionDivider(),
                            material.getCompositionComponents().stream()
                                    .map(part -> new MaterialInfoDefinition.Part(
                                            part.material().resolve().getName(), part.amount()))
                                    .toList()));
            var rules = new LinkedHashMap<>(PROCESSES.get(name).rules());
            replaceProcess(rules, MaterialProcessKeys.SMELTING, SmeltingMaterialHelper.target(material));
            replaceProcess(rules, MaterialProcessKeys.CRUSHING, CrushingMaterialHelper.target(material));
            replaceProcess(rules, MaterialProcessKeys.PULVERIZING, PulverizationMaterialHelper.target(material));
            replaceProcess(rules, MaterialProcessKeys.FUEL, FuelMaterialHelper.rule(material));
            replaceProcess(
                    rules, MaterialProcessKeys.ORE_PROCESSING, Optional.of(OreProcessingMaterialHelper.rule(material)));
            PROCESSES.put(name, new MaterialInfoDefinition.Processes(rules));
        }
    }

    private static <T> void replaceProcess(
            Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> rules,
            MaterialProcessKey<T> key,
            Optional<T> value) {
        rules.remove(key);
        value.ifPresent(result -> rules.put(key, List.of(MaterialProcessRule.anyForm(result))));
    }

    static void closeRegistrations() {
        registrationsClosed = true;
    }

    @Override
    public MaterialInfoTool.Registrar registrar(String addonId) {
        AddonNames.requireAddonId(addonId);
        return new Registrar(addonId);
    }

    /** Applies addon-owned unique facts after identities and ownership are known. */
    static void resolveAddonRegistrations() {
        for (ToolSubmission submission : ADDON_TOOLS) {
            GTMaterial material = requireOwned(submission.addonId(), submission.material(), "tool profile");
            MaterialToolProfile existing = TOOLS.get(submission.material().name());
            MaterialToolProfile expected = new MaterialToolProfile(0, 1F, 0, 0);
            if (!existing.equals(expected) && !existing.equals(submission.profile()))
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' tool profile for '"
                        + submission.material().name() + "' conflicts with the existing profile");
            TOOLS.put(submission.material().name(), submission.profile());
            material.setToolStats(
                    submission.profile().types(),
                    submission.profile().speed(),
                    submission.profile().durability(),
                    submission.profile().quality());
        }
        for (CompositionSubmission submission : ADDON_COMPOSITIONS) {
            GTMaterial material = requireOwned(submission.addonId(), submission.material(), "composition");
            MaterialComposition composition = submission.composition();
            var components = composition.parts().stream()
                    .map(part -> {
                        GTMaterial component = GTMaterialRegistry.get(part.material());
                        if (!component.isValid())
                            throw new IllegalArgumentException("Addon '" + submission.addonId()
                                    + "' composition uses unknown material '" + part.material() + "'");
                        if (component.resolve() == material)
                            throw new IllegalArgumentException("Addon '" + submission.addonId() + "' composition of '"
                                    + material.getName() + "' lists itself");
                        return MaterialComponent.of(component, part.amount());
                    })
                    .toArray(MaterialComponent[]::new);
            if (material.hasComposition()) {
                if (material.getCompositionDivider() != composition.divider() || !sameComposition(material, components))
                    throw new IllegalArgumentException("Addon '" + submission.addonId() + "' composition of '"
                            + submission.material().name() + "' conflicts with the existing composition");
            } else {
                material.setComposition(composition.divider(), components);
            }
            COMPOSITIONS.put(
                    submission.material().name(),
                    new MaterialInfoDefinition.Composition(
                            composition.divider(),
                            composition.parts().stream()
                                    .map(part -> new MaterialInfoDefinition.Part(part.material(), part.amount()))
                                    .toList()));
            COMPOSITION_CRUCIBLE.put(submission.material().name(), composition.crucible());
        }
    }

    static Map<String, List<MaterialFormRequest>> formRequests() {
        var result = new LinkedHashMap<String, List<MaterialFormRequest>>();
        for (FormSubmission submission : ADDON_FORMS) {
            result.computeIfAbsent(submission.addonId(), ignored -> new ArrayList<>())
                    .add(new MaterialFormRequest(
                            submission.material().name(), submission.form().name()));
        }
        result.replaceAll((ignored, forms) -> List.copyOf(forms));
        return Map.copyOf(result);
    }

    public static Map<String, List<MaterialComposition>> compositionRequests() {
        var result = new LinkedHashMap<String, List<MaterialComposition>>();
        for (CompositionSubmission submission : ADDON_COMPOSITIONS) {
            result.computeIfAbsent(submission.addonId(), ignored -> new ArrayList<>())
                    .add(submission.composition());
        }
        result.replaceAll((ignored, compositions) -> List.copyOf(compositions));
        return Map.copyOf(result);
    }

    static List<TagSubmission> tagSubmissions() {
        return List.copyOf(ADDON_TAGS);
    }

    static List<ProcessSubmission> processSubmissions() {
        return List.copyOf(ADDON_PROCESSES);
    }

    private static Set<FormKey> normalizeProcessForms(
            String source, MaterialProcessKey<?> process, Set<FormKey> sourceForms) {
        if (sourceForms.isEmpty()) return sourceForms;
        var forms = new LinkedHashSet<FormKey>();
        for (var form : sourceForms) {
            var prefix = PrefixRegistry.byName(form.name());
            if (prefix == null)
                throw new IllegalArgumentException(
                        "Unknown material form '" + form.name() + "' in process " + process.id() + " from " + source);
            forms.add(new FormKey(prefix.getName()));
        }
        return Set.copyOf(forms);
    }

    static MaterialProcessRule<?> normalizeProcessRule(
            String source, MaterialProcessKey<?> process, MaterialProcessRule<?> rule) {
        return new MaterialProcessRule<>(normalizeProcessForms(source, process, rule.forms()), rule.value());
    }

    @Override
    public MaterialToolProfile tool(MaterialKey key) {
        var frozen = requireReadySnapshot();
        return frozen.tools().getOrDefault(Objects.requireNonNull(key).name(), new MaterialToolProfile(0, 1F, 0, 0));
    }

    @Override
    public Optional<MaterialToolProfile> toolIfReady(MaterialKey key) {
        if (GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.FAILED)
            throw new IllegalStateException("Material initialization previously failed");
        Snapshot frozen = snapshot;
        if (frozen == null || GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY)
            return Optional.empty();
        return Optional.of(
                frozen.tools().getOrDefault(Objects.requireNonNull(key).name(), new MaterialToolProfile(0, 1F, 0, 0)));
    }

    @Override
    public Optional<MaterialComposition> composition(MaterialKey material) {
        var composition = compositionDefinition(material);
        if (composition.parts().isEmpty()) return Optional.empty();
        var parts = composition.parts().stream()
                .map(part -> new MaterialComposition.Part(part.material(), part.amount()))
                .toList();
        boolean crucible = requireReadySnapshot().compositionCrucible().getOrDefault(material.name(), false);
        return Optional.of(new MaterialComposition(material.name(), composition.divider(), parts, crucible));
    }

    public static MaterialInfoDefinition.Composition compositionDefinition(MaterialKey key) {
        var frozen = requireReadySnapshot();
        return frozen.compositions()
                .getOrDefault(
                        Objects.requireNonNull(key).name(),
                        new MaterialInfoDefinition.Composition(0, java.util.List.of()));
    }

    public static MaterialInfoDefinition.Relations relations(MaterialKey key) {
        return relations(Objects.requireNonNull(key).name());
    }

    public static MaterialInfoDefinition.Relations relations(String name) {
        var frozen = requireReadySnapshot();
        return frozen.relations()
                .getOrDefault(
                        Objects.requireNonNull(name),
                        new MaterialInfoDefinition.Relations(
                                false, java.util.Set.of(), java.util.Set.of(), java.util.List.of()));
    }

    static MaterialInfoDefinition.Relations relationsForPublication(String name) {
        var frozen = requireSnapshotForPublication();
        return frozen.relations()
                .getOrDefault(
                        Objects.requireNonNull(name),
                        new MaterialInfoDefinition.Relations(
                                false, java.util.Set.of(), java.util.Set.of(), java.util.List.of()));
    }

    public static MaterialInfoDefinition.Processes processes(MaterialKey key) {
        var frozen = requireReadySnapshot();
        return frozen.processes()
                .getOrDefault(Objects.requireNonNull(key).name(), new MaterialInfoDefinition.Processes(Map.of()));
    }

    static MaterialInfoDefinition.Processes processesForPublication(MaterialKey key) {
        var frozen = requireSnapshotForPublication();
        return frozen.processes()
                .getOrDefault(Objects.requireNonNull(key).name(), new MaterialInfoDefinition.Processes(Map.of()));
    }

    static MaterialInfoDefinition.Relations relationsBeforeFreeze(String name) {
        return RELATIONS.getOrDefault(
                name,
                new MaterialInfoDefinition.Relations(
                        false, java.util.Set.of(), java.util.Set.of(), java.util.List.of()));
    }

    static MaterialInfoDefinition.Processes processesBeforeFreeze(String name) {
        return PROCESSES.getOrDefault(name, new MaterialInfoDefinition.Processes(Map.of()));
    }

    private static Snapshot requireReadySnapshot() {
        Snapshot frozen = requireSnapshotForPublication();
        GTMaterialRegistry.RegistrationPhase phase = GTMaterialRegistry.registrationPhase();
        if (phase != GTMaterialRegistry.RegistrationPhase.READY)
            throw new IllegalStateException("Material auxiliary indexes are not ready (" + phase + ")");
        return frozen;
    }

    private static Snapshot requireSnapshotForPublication() {
        Snapshot frozen = snapshot;
        if (frozen == null)
            throw new IllegalStateException(
                    "Material auxiliary indexes are not frozen (" + GTMaterialRegistry.registrationPhase() + ")");
        return frozen;
    }

    private static GTMaterial requireOwned(String addonId, MaterialKey key, String what) {
        GTMaterial found = GTMaterialRegistry.get(key.name());
        if (!found.isValid())
            throw new IllegalArgumentException(
                    "Addon '" + addonId + "' " + what + " uses unknown material '" + key.name() + "'");
        GTMaterial material = found.resolve();
        if (!AddonMaterials.owners(material).contains(addonId))
            throw new IllegalArgumentException(
                    "Addon '" + addonId + "' cannot set the " + what + " of '" + material.getName() + "'");
        return material;
    }

    private static boolean sameComposition(GTMaterial material, MaterialComponent[] components) {
        var existing = material.getCompositionComponents();
        if (existing.size() != components.length) return false;
        for (int i = 0; i < components.length; i++)
            if (existing.get(i).material() != components[i].material()
                    || existing.get(i).amount() != components[i].amount()) return false;
        return true;
    }

    private final class Registrar implements MaterialInfoTool.Registrar {
        private final String addonId;

        private Registrar(String addonId) {
            this.addonId = addonId;
        }

        @Override
        public MaterialInfoTool.Registrar registerTool(MaterialKey material, MaterialToolProfile profile) {
            requireOpen();
            ADDON_TOOLS.add(
                    new ToolSubmission(addonId, Objects.requireNonNull(material), Objects.requireNonNull(profile)));
            return this;
        }

        @Override
        public MaterialInfoTool.Registrar registerComposition(MaterialKey material, MaterialComposition composition) {
            requireOpen();
            Objects.requireNonNull(material);
            Objects.requireNonNull(composition);
            if (!material.name().equals(composition.material()))
                throw new IllegalArgumentException("Composition key and material name disagree: " + material.name());
            ADDON_COMPOSITIONS.add(new CompositionSubmission(addonId, material, composition));
            return this;
        }

        @Override
        public MaterialInfoTool.Registrar registerForm(MaterialKey material, FormKey form) {
            requireOpen();
            ADDON_FORMS.add(
                    new FormSubmission(addonId, Objects.requireNonNull(material), Objects.requireNonNull(form)));
            return this;
        }

        @Override
        public MaterialInfoTool.Registrar registerTag(MaterialKey material, gregtech.api.addon.material.DataKey tag) {
            requireOpen();
            Objects.requireNonNull(tag);
            if (!addonId.equals(tag.namespace()))
                throw new IllegalArgumentException("Addon '" + addonId + "' cannot declare tag " + tag);
            ADDON_TAGS.add(new TagSubmission(addonId, Objects.requireNonNull(material), tag));
            return this;
        }

        @Override
        public <T> MaterialInfoTool.Registrar registerProcess(
                MaterialKey material, MaterialProcessKey<T> process, MaterialProcessRule<T> rule) {
            requireOpen();
            Objects.requireNonNull(process);
            Objects.requireNonNull(rule);
            if (!addonId.equals(process.id().namespace()))
                throw new IllegalArgumentException("Addon '" + addonId + "' cannot declare process " + process.id());
            if (!process.type().isInstance(rule.value()))
                throw new IllegalArgumentException("Process value type mismatch for " + process.id());
            rule = new MaterialProcessRule<>(
                    normalizeProcessForms("addon '" + addonId + "'", process, rule.forms()), rule.value());
            ADDON_PROCESSES.add(new ProcessSubmission(addonId, Objects.requireNonNull(material), process, rule));
            return this;
        }

        private static void requireOpen() {
            if (registrationsClosed) throw new IllegalStateException("Material auxiliary registrations are closed");
        }
    }
}
