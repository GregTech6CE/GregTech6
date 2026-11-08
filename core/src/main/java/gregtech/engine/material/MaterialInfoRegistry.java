package gregtech.engine.material;

import gregtech.api.addon.AddonNames;
import gregtech.api.addon.material.*;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialToolProfile;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.prefix.PrefixRegistry;

import java.util.*;

/** One mutable auxiliary-info entry per canonical material and one frozen publication snapshot. */
public final class MaterialInfoRegistry implements MaterialInfoTool.Host {
    private static final MaterialInfoRegistry HOST = new MaterialInfoRegistry();

    private MaterialInfoRegistry() {}

    private static final Map<MaterialKey, MutableMaterialInfo> MATERIALS = new LinkedHashMap<>();
    private static final Map<String, MaterialInfoDefinition.Relations> GROUP_RELATIONS = new LinkedHashMap<>();
    private static final List<ToolSubmission> ADDON_TOOLS = new ArrayList<>();
    private static final List<CompositionSubmission> ADDON_COMPOSITIONS = new ArrayList<>();
    private static final List<FormSubmission> ADDON_FORMS = new ArrayList<>();
    private static final List<TagSubmission> ADDON_TAGS = new ArrayList<>();
    private static final List<MetadataSubmission> ADDON_METADATA = new ArrayList<>();
    private static final List<TagDeclarationSubmission> ADDON_TAG_DECLARATIONS = new ArrayList<>();
    private static final List<GroupSubmission> ADDON_GROUPS = new ArrayList<>();
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

    public record TagSubmission(String addonId, MaterialKey material, DataKey tag) {}

    public record MetadataSubmission(String addonId, MaterialMetadataDeclaration declaration) {}

    public record TagDeclarationSubmission(String addonId, MaterialTagDeclaration declaration) {}

    public record GroupSubmission(String addonId, MaterialGroupDeclaration declaration) {}

    public record ProcessSubmission(
            String addonId, MaterialKey material, MaterialProcessKey<?> process, MaterialProcessRule<?> rule) {}

    private record ToolSubmission(String addonId, MaterialKey material, MaterialToolProfile profile) {}

    private record CompositionSubmission(String addonId, MaterialKey material, MaterialComposition composition) {}

    private record PublishedMaterialInfo(MaterialInfoDefinition definition) {}

    private record Snapshot(
            Map<MaterialKey, PublishedMaterialInfo> materials, Map<String, MaterialInfoDefinition.Relations> groups) {}

    private static final class MutableMaterialInfo {
        private MaterialInfoDefinition.Tooling tooling;
        private MaterialInfoDefinition.Composition composition;
        private MaterialInfoDefinition.Relations relations;
        private final LinkedHashMap<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> processes;

        private MutableMaterialInfo(MaterialInfoDefinition info) {
            tooling = info.tooling();
            composition = info.composition();
            relations = info.relations();
            processes = copyProcesses(info.processes().rules());
        }

        private PublishedMaterialInfo freeze() {
            var definition = new MaterialInfoDefinition(
                    tooling, composition, relations, new MaterialInfoDefinition.Processes(processes));
            return new PublishedMaterialInfo(definition);
        }

        private static LinkedHashMap<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> copyProcesses(
                Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> source) {
            var copy = new LinkedHashMap<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>();
            source.forEach((key, rules) -> copy.put(key, new ArrayList<>(rules)));
            return copy;
        }

        private void replaceProcess(MaterialProcessKey<?> key, Optional<? extends MaterialProcessRule<?>> value) {
            processes.remove(key);
            value.ifPresent(rule -> processes.put(key, List.of(rule)));
        }
    }

    static void install(MaterialDefinition.Declaration declaration) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        if (declaration.info().relations().group()) {
            if (GROUP_RELATIONS.putIfAbsent(
                            declaration.name(), declaration.info().relations())
                    != null)
                throw new IllegalArgumentException("Duplicate material group relations: " + declaration.name());
            return;
        }
        var key = new MaterialKey(declaration.name());
        if (MATERIALS.putIfAbsent(key, new MutableMaterialInfo(declaration.info())) != null)
            throw new IllegalArgumentException("Duplicate material auxiliary declaration: " + key.name());
    }

    static void validateInstall(MaterialDefinition.Declaration declaration) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        if (declaration.info().relations().group()) {
            if (GROUP_RELATIONS.containsKey(declaration.name()))
                throw new IllegalArgumentException("Duplicate material group relations: " + declaration.name());
            return;
        }
        if (MATERIALS.containsKey(new MaterialKey(declaration.name())))
            throw new IllegalArgumentException("Duplicate material auxiliary declaration: " + declaration.name());
    }

    static void freeze() {
        if (snapshot != null) return;
        registrationsClosed = true;
        normalizeLinkedFlags();
        validateLinkedToolRegistrations();
        var published = new LinkedHashMap<MaterialKey, PublishedMaterialInfo>();
        MATERIALS.forEach((key, info) -> published.put(key, info.freeze()));
        snapshot = new Snapshot(Map.copyOf(published), Map.copyOf(GROUP_RELATIONS));
    }

    private static void normalizeLinkedFlags() {
        for (var entry : MATERIALS.entrySet()) {
            GTMaterial material = GTMaterialRegistry.get(entry.getKey().name());
            if (!material.isValid()) continue;
            Set<gregtech.api.material.MaterialProperty> normalized =
                    MaterialPropertyRules.normalize(material.getProperties());
            for (var flag : normalized) if (!material.has(flag)) material.put(flag);
            boolean decomposition = material.has(gregtech.api.material.MaterialProperty.CENTRIFUGE)
                    || material.has(gregtech.api.material.MaterialProperty.ELECTROLYSER);
            if (decomposition
                    && AddonMaterials.owns(material)
                    && entry.getValue().composition.parts().isEmpty())
                throw new IllegalArgumentException(
                        "Addon material '" + material.getName() + "' declares decomposition without a composition");
        }
    }

    private static void validateLinkedToolRegistrations() {
        for (ToolSubmission submission : ADDON_TOOLS) {
            GTMaterial material = requireOwned(submission.addonId(), submission.material(), "tool profile");
            MaterialInfoDefinition.Tooling linked = MATERIALS.get(new MaterialKey(material.getName())).tooling;
            if (!linked.profile().equals(submission.profile()))
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' tool profile for '"
                        + material.getName() + "' conflicts with the linked profile");
        }
    }

    /** Preserve the legacy SPI's crucible permission on the one authoritative composition. */
    public static void recordLinkedComposition(GTMaterial material, boolean crucible) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        var key = new MaterialKey(material.resolve().getName());
        var info = MATERIALS.get(key);
        if (info == null) throw new IllegalArgumentException("Unknown material auxiliary declaration: " + key.name());
        info.composition =
                new MaterialInfoDefinition.Composition(info.composition.divider(), info.composition.parts(), crucible);
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
            var info = MATERIALS.get(new MaterialKey(material.getName()));
            MaterialToolProfile existing = info.tooling.profile();
            MaterialToolProfile expected = new MaterialToolProfile(0, 1F, 0, 0);
            if (!existing.equals(expected) && !existing.equals(submission.profile()))
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' tool profile for '"
                        + material.getName() + "' conflicts with the existing profile");
            info.tooling = new MaterialInfoDefinition.Tooling(submission.profile(), info.tooling.correctedToolStats());
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

    static List<MetadataSubmission> metadataSubmissions() {
        return List.copyOf(ADDON_METADATA);
    }

    static List<TagDeclarationSubmission> tagDeclarationSubmissions() {
        return List.copyOf(ADDON_TAG_DECLARATIONS);
    }

    static List<GroupSubmission> groupSubmissions() {
        return List.copyOf(ADDON_GROUPS);
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
    public Optional<MaterialToolProfile> tool(MaterialKey key) {
        var published = publishedMaterial(key);
        return published.map(info -> info.definition().toolProfile());
    }

    @Override
    public Optional<MaterialComposition> composition(MaterialKey material) {
        var published = publishedMaterial(material);
        if (published.isEmpty()) return Optional.empty();
        var composition = published.get().definition().composition();
        if (composition.parts().isEmpty()) return Optional.empty();
        var parts = composition.parts().stream()
                .map(part -> new MaterialComposition.Part(part.material(), part.amount()))
                .toList();
        return Optional.of(
                new MaterialComposition(material.name(), composition.divider(), parts, composition.crucible()));
    }

    public static MaterialInfoDefinition.Composition compositionDefinition(MaterialKey key) {
        var published = publishedMaterial(key, true);
        return published.isEmpty()
                ? new MaterialInfoDefinition.Composition(0, List.of(), false)
                : published.get().definition().composition();
    }

    @Override
    public Optional<MaterialToolProfile> currentTool(MaterialKey key) {
        if (GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.FAILED)
            return Optional.empty();
        var ready = snapshot;
        if (ready != null) {
            var published = ready.materials().get(key);
            return published == null
                    ? Optional.empty()
                    : Optional.of(published.definition().toolProfile());
        }
        var current = MATERIALS.get(key);
        return current == null ? Optional.empty() : Optional.of(current.tooling.profile());
    }

    @Override
    public Optional<GTMaterial> findMaterial(MaterialKey key) {
        GTMaterial material = GTMaterialRegistry.get(key.name());
        return material.isValid() ? Optional.of(material.resolve()) : Optional.empty();
    }

    @Override
    public Optional<MaterialComposition> currentComposition(MaterialKey material) {
        if (GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.FAILED)
            return Optional.empty();
        var ready = snapshot;
        MaterialInfoDefinition.Composition composition;
        if (ready != null) {
            var published = ready.materials().get(material);
            if (published == null) return Optional.empty();
            composition = published.definition().composition();
        } else {
            var current = MATERIALS.get(material);
            if (current == null) return Optional.empty();
            composition = current.composition;
        }
        if (composition.parts().isEmpty()) return Optional.empty();
        return Optional.of(new MaterialComposition(
                material.name(),
                composition.divider(),
                composition.parts().stream()
                        .map(part -> new MaterialComposition.Part(part.material(), part.amount()))
                        .toList(),
                composition.crucible()));
    }

    @Override
    public <T> Optional<MaterialProcessRule<T>> currentProcess(MaterialKey material, MaterialProcessKey<T> process) {
        if (GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.FAILED)
            return Optional.empty();
        var ready = snapshot;
        if (ready != null) {
            var published = ready.materials().get(material);
            return published == null
                    ? Optional.empty()
                    : published.definition().processes().findSingle(process);
        }
        var current = MATERIALS.get(material);
        if (current == null) return Optional.empty();
        var rules = current.processes.get(process);
        if (rules == null || rules.size() != 1) return Optional.empty();
        MaterialProcessRule<?> rule = rules.get(0);
        return process.type().isInstance(rule.value())
                ? Optional.of(
                        new MaterialProcessRule<>(rule.forms(), process.type().cast(rule.value())))
                : Optional.empty();
    }

    @Override
    public void updateTool(MaterialKey material, MaterialToolProfile profile) {
        requireMutable(material);
        var info = MATERIALS.get(material);
        info.tooling = new MaterialInfoDefinition.Tooling(profile, info.tooling.correctedToolStats());
    }

    @Override
    public void updateComposition(MaterialKey material, Optional<MaterialComposition> composition) {
        requireMutable(material);
        if (composition.isEmpty()) {
            MATERIALS.get(material).composition = new MaterialInfoDefinition.Composition(0, List.of(), false);
            return;
        }
        MaterialComposition value = composition.get();
        if (!material.name().equals(value.material()))
            throw new IllegalArgumentException("Composition key and material name disagree: " + material.name());
        MATERIALS.get(material).composition = new MaterialInfoDefinition.Composition(
                value.divider(),
                value.parts().stream()
                        .map(part -> new MaterialInfoDefinition.Part(part.material(), part.amount()))
                        .toList(),
                value.crucible());
    }

    @Override
    public <T> void updateProcess(
            MaterialKey material, MaterialProcessKey<T> process, Optional<MaterialProcessRule<T>> rule) {
        requireMutable(material);
        if (rule.isPresent() && !process.type().isInstance(rule.get().value()))
            throw new IllegalArgumentException("Process value type mismatch for " + process.id());
        MATERIALS.get(material).replaceProcess(process, rule);
    }

    private static void requireMutable(MaterialKey material) {
        if (snapshot != null) throw new IllegalStateException("Material auxiliary registrations are frozen");
        if (!MATERIALS.containsKey(material))
            throw new IllegalArgumentException("Unknown material auxiliary declaration: " + material.name());
    }

    public static MaterialInfoDefinition.Relations relations(MaterialKey key) {
        return relations(Objects.requireNonNull(key).name());
    }

    public static MaterialInfoDefinition.Relations relations(String name) {
        var key = safeMaterialKey(name);
        var published = key == null ? Optional.<PublishedMaterialInfo>empty() : publishedMaterial(key, true);
        if (published.isPresent()) return published.get().definition().relations();
        var ready = snapshot;
        if (ready == null || GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY)
            return emptyRelations();
        return ready.groups().getOrDefault(name, emptyRelations());
    }

    static MaterialInfoDefinition.Relations relationsForPublication(String name) {
        var ready = snapshot;
        if (ready == null) return emptyRelations();
        var key = safeMaterialKey(name);
        if (key != null) {
            var published = ready.materials().get(key);
            if (published != null) return published.definition().relations();
        }
        return ready.groups().getOrDefault(name, emptyRelations());
    }

    /**
     * Internal form lookup used while the catalog and legacy prefix predicates are being published.
     * Returns no facts after a failed material lifecycle.
     */
    public static Set<String> formsForPublication(String name) {
        if (GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.FAILED) return Set.of();
        return relationsForPublication(name).forms();
    }

    public static MaterialInfoDefinition.Processes processes(MaterialKey key) {
        var published = publishedMaterial(key, true);
        return published.isEmpty()
                ? new MaterialInfoDefinition.Processes(Map.of())
                : published.get().definition().processes();
    }

    static MaterialInfoDefinition.Processes processesForPublication(MaterialKey key) {
        var published = publishedMaterial(key, false);
        return published.isEmpty()
                ? new MaterialInfoDefinition.Processes(Map.of())
                : published.get().definition().processes();
    }

    private static Optional<PublishedMaterialInfo> publishedMaterial(MaterialKey key) {
        return publishedMaterial(key, true);
    }

    private static Optional<PublishedMaterialInfo> publishedMaterial(MaterialKey key, boolean requireReady) {
        Objects.requireNonNull(key, "key");
        Snapshot published = snapshot;
        if (published == null) return Optional.empty();
        if (requireReady && GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY)
            return Optional.empty();
        return Optional.ofNullable(published.materials().get(key));
    }

    private static MaterialInfoDefinition.Relations emptyRelations() {
        return new MaterialInfoDefinition.Relations(false, Set.of(), Set.of(), List.of());
    }

    private static MaterialKey safeMaterialKey(String name) {
        try {
            return new MaterialKey(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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

    private static void requireNamespace(String addonId, DataKey key) {
        if (!addonId.equals(key.namespace()))
            throw new IllegalArgumentException("Addon '" + addonId + "' cannot declare key " + key);
    }

    private static final class Registrar implements MaterialInfoTool.Registrar {
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
        public MaterialInfoTool.Registrar registerTag(MaterialKey material, DataKey tag) {
            requireOpen();
            Objects.requireNonNull(tag);
            requireNamespace(addonId, tag);
            ADDON_TAGS.add(new TagSubmission(addonId, Objects.requireNonNull(material), tag));
            return this;
        }

        @Override
        public MaterialInfoTool.Registrar registerMetadata(MaterialMetadataDeclaration declaration) {
            requireOpen();
            ADDON_METADATA.add(new MetadataSubmission(addonId, Objects.requireNonNull(declaration)));
            return this;
        }

        @Override
        public MaterialInfoTool.Registrar registerTagDeclaration(MaterialTagDeclaration declaration) {
            requireOpen();
            Objects.requireNonNull(declaration);
            requireNamespace(addonId, declaration.tag());
            ADDON_TAG_DECLARATIONS.add(new TagDeclarationSubmission(addonId, declaration));
            return this;
        }

        @Override
        public MaterialInfoTool.Registrar registerGroup(MaterialGroupDeclaration declaration) {
            requireOpen();
            Objects.requireNonNull(declaration);
            requireNamespace(addonId, declaration.key().id());
            ADDON_GROUPS.add(new GroupSubmission(addonId, declaration));
            return this;
        }

        @Override
        public <T> MaterialInfoTool.Registrar registerProcess(
                MaterialKey material, MaterialProcessKey<T> process, MaterialProcessRule<T> rule) {
            requireOpen();
            Objects.requireNonNull(material);
            Objects.requireNonNull(process);
            Objects.requireNonNull(rule);
            requireNamespace(addonId, process.id());
            if (!process.type().isInstance(rule.value()))
                throw new IllegalArgumentException("Process value type mismatch for " + process.id());
            if (process.cardinality() == MaterialProcessCardinality.SINGLE) {
                for (ProcessSubmission submission : ADDON_PROCESSES) {
                    if (submission.material().equals(material)
                            && submission.process().equals(process))
                        throw new IllegalArgumentException("Single-valued process " + process.id()
                                + " is already registered for material " + material.name());
                }
            }
            ADDON_PROCESSES.add(new ProcessSubmission(addonId, material, process, rule));
            return this;
        }

        private static void requireOpen() {
            if (registrationsClosed) throw new IllegalStateException("Material auxiliary registrations are closed");
        }
    }
}
