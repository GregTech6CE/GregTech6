package gregtech.engine.material;

import gregtech.api.addon.material.*;
import gregtech.api.material.GTMaterial;
import gregtech.content.material.MaterialDefinitions;
import gregtech.engine.addon.AddonMaterials;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Captures intrinsic definitions after every legal addon callback, independent of later native content. */
public final class MaterialViews {
    private MaterialViews() {}

    private record TagFacts(String owner, Set<DataKey> includes) {}

    private record GroupFacts(
            String owner, String displayName, Set<MaterialKey> materials, Set<MaterialGroupKey> includedGroups) {}

    private static MaterialCatalogView published;

    static void publish() {
        var builders = new LinkedHashMap<MaterialKey, MaterialMetadata.Builder>();
        var properties = new LinkedHashMap<MaterialKey, Map<PropertyKey<?>, Object>>();
        var types = new LinkedHashMap<DataKey, Class<?>>();
        var origins = new LinkedHashMap<String, String>();
        var directTagMembers = new LinkedHashMap<DataKey, Set<MaterialKey>>();
        var tagFacts = new LinkedHashMap<DataKey, TagFacts>();
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            var definition = MaterialDefinitions.find(material.getName());
            if (!material.isValid() || definition != null && definition.group()) continue;
            var builder = MaterialMetadata.builder()
                    .property(MaterialProperties.MELTING_POINT, material.getMeltingPoint())
                    .property(MaterialProperties.BOILING_POINT, material.getBoilingPoint())
                    .property(MaterialProperties.DENSITY, material.getDensity())
                    .property(MaterialProperties.FORMULA, material.getTooltipChemical());
            for (var flag : material.getProperties())
                builder.flag(new DataKey("gt6ce", flag.name().toLowerCase(java.util.Locale.ROOT)));
            if (definition != null)
                for (String tag : definition.classifications()) {
                    var key = new DataKey("gt6ce", tag);
                    builder.tag(key);
                    tagFacts.putIfAbsent(key, new TagFacts("gt6ce", Set.of()));
                }
            var key = new MaterialKey(material.getName());
            builders.put(key, builder);
            properties.put(key, new LinkedHashMap<>());
            for (var property : builder.build().keys()) types.put(property.id(), property.type());
        }
        for (var submission : AddonMaterials.metadataDeclarations()) {
            var declaration = submission.declaration();
            GTMaterial material = GTMaterialRegistry.get(declaration.materialName());
            var key = material.isValid() ? new MaterialKey(material.getName()) : null;
            if (key == null || !builders.containsKey(key))
                throw new IllegalArgumentException("Addon '" + submission.addonId()
                        + "' metadata uses unknown real material '" + declaration.materialName() + "'");
            var metadata = declaration.metadata();
            var builder = builders.get(key);
            for (var tag : metadata.tags()) {
                requireNamespace(submission.addonId(), tag);
                builder.tag(tag);
                var previous = tagFacts.putIfAbsent(tag, new TagFacts(submission.addonId(), Set.of()));
                if (previous != null && !previous.owner().equals(submission.addonId()))
                    throw new IllegalArgumentException("Addon '" + submission.addonId() + "' material tag '" + tag
                            + "' conflicts with owner '" + previous.owner() + "'");
            }
            if (!metadata.keys().isEmpty() || !metadata.flags().isEmpty()) {
                if (!AddonMaterials.owners(material).contains(submission.addonId()))
                    throw new IllegalArgumentException("Addon '" + submission.addonId()
                            + "' cannot set metadata properties or flags of '" + key.name() + "'");
            }
            for (var flag : metadata.flags()) {
                requireNamespace(submission.addonId(), flag);
                builder.flag(flag);
            }
            for (var property : metadata.keys()) {
                requireNamespace(submission.addonId(), property.id());
                var previousType = types.putIfAbsent(property.id(), property.type());
                if (previousType != null && previousType != property.type())
                    throw new IllegalArgumentException(
                            "Addon '" + submission.addonId() + "' property type conflict: " + property.id());
                addProperty(submission.addonId(), key, property, metadata, builder, properties.get(key), origins);
            }
        }
        var materials = new LinkedHashMap<MaterialKey, MaterialView>();
        for (var entry : builders.entrySet()) {
            var material = GTMaterialRegistry.get(entry.getKey().name());
            var metadata = entry.getValue().build();
            String owner = AddonMaterials.owns(material) ? AddonMaterials.owner(material) : "gt6ce";
            materials.put(
                    entry.getKey(),
                    new MaterialSnapshot(
                            entry.getKey(),
                            new MaterialInfo(
                                    material.getId(),
                                    material.getTranslationKey(),
                                    material.getDisplayNameFallback(),
                                    material.getColor(),
                                    owner),
                            metadata));
            for (var tag : metadata.tags()) {
                directTagMembers
                        .computeIfAbsent(tag, ignored -> new LinkedHashSet<>())
                        .add(entry.getKey());
            }
        }
        for (var submission : AddonMaterials.tagDeclarations()) {
            var tag = submission.declaration().tag();
            requireNamespace(submission.addonId(), tag);
            var facts =
                    new TagFacts(submission.addonId(), submission.declaration().includes());
            var previous = tagFacts.putIfAbsent(tag, facts);
            if (previous != null) {
                if (!previous.owner().equals(submission.addonId()))
                    throw new IllegalArgumentException("Addon '" + submission.addonId() + "' material tag '" + tag
                            + "' conflicts with owner '" + previous.owner() + "'");
                var includes = new LinkedHashSet<>(previous.includes());
                includes.addAll(facts.includes());
                tagFacts.put(tag, new TagFacts(submission.addonId(), Set.copyOf(includes)));
            }
        }
        for (var facts : tagFacts.values())
            for (var included : facts.includes())
                if (!tagFacts.containsKey(included))
                    throw new IllegalArgumentException(
                            "Material tag '" + included + "' referenced by a tag declaration is unknown");
        var tagMembers = new LinkedHashMap<DataKey, Set<MaterialKey>>();
        for (var tag : tagFacts.keySet())
            tagMembers.put(tag, resolveTagMembers(tag, tagFacts, directTagMembers, tagMembers, new LinkedHashSet<>()));
        var materialTags = new LinkedHashMap<MaterialKey, Set<DataKey>>();
        for (var material : materials.keySet()) materialTags.put(material, new LinkedHashSet<>());
        tagMembers.forEach((tag, members) -> {
            for (var material : members) materialTags.get(material).add(tag);
        });

        var groupFacts = new LinkedHashMap<MaterialGroupKey, GroupFacts>();
        for (var definition : MaterialDefinitions.all()) {
            if (!definition.group()) continue;
            var key = groupKey(definition.name());
            var groupMaterials = new LinkedHashSet<MaterialKey>();
            var includedGroups = new LinkedHashSet<MaterialGroupKey>();
            for (var member : definition.members()) {
                var memberDefinition = MaterialDefinitions.find(member);
                if (memberDefinition != null && memberDefinition.group()) {
                    includedGroups.add(groupKey(memberDefinition.name()));
                    continue;
                }
                var material = GTMaterialRegistry.get(member);
                if (!material.isValid())
                    throw new IllegalArgumentException("Unknown group member: " + definition.name() + " -> " + member);
                groupMaterials.add(new MaterialKey(material.getName()));
            }
            putGroup(
                    groupFacts,
                    key,
                    new GroupFacts(
                            "gt6ce", definition.displayName(), Set.copyOf(groupMaterials), Set.copyOf(includedGroups)),
                    "built-in catalog");
        }
        for (var submission : AddonMaterials.groupDeclarations()) {
            var declaration = submission.declaration();
            requireNamespace(submission.addonId(), declaration.key().id());
            putGroup(
                    groupFacts,
                    declaration.key(),
                    new GroupFacts(
                            submission.addonId(),
                            declaration.displayName(),
                            declaration.materials(),
                            declaration.includedGroups()),
                    "addon '" + submission.addonId() + "'");
        }
        for (var facts : groupFacts.values()) {
            for (var member : facts.materials())
                if (!materials.containsKey(member))
                    throw new IllegalArgumentException(
                            "Material group references unknown material '" + member.name() + "'");
            for (var included : facts.includedGroups())
                if (!groupFacts.containsKey(included))
                    throw new IllegalArgumentException(
                            "Material group references unknown group '" + included.id() + "'");
        }
        var groups = new LinkedHashMap<MaterialGroupKey, MaterialGroupView>();
        for (var entry : groupFacts.entrySet())
            groups.put(
                    entry.getKey(),
                    new MaterialGroupView(
                            entry.getKey(),
                            entry.getValue().displayName(),
                            resolveGroupMembers(entry.getKey(), groupFacts, groups, new LinkedHashSet<>())));

        var processTypes = new LinkedHashMap<DataKey, Class<?>>();
        var processRules = new LinkedHashMap<MaterialKey, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>>();
        for (var material : materials.keySet()) processRules.put(material, new LinkedHashMap<>());
        for (var material : materials.keySet()) {
            var definition = MaterialDefinitions.find(material.name());
            if (definition == null) continue;
            definition.processes().rules().forEach((key, rules) -> {
                registerProcessType(processTypes, key, "built-in catalog");
                for (var rule : rules) addProcessRule(processRules.get(material), key, rule);
            });
        }
        for (var submission : AddonMaterials.processDeclarations()) {
            var declaration = submission.declaration();
            requireNamespace(submission.addonId(), declaration.process().id());
            GTMaterial material = GTMaterialRegistry.get(declaration.materialName());
            if (!material.isValid())
                throw new IllegalArgumentException("Addon '" + submission.addonId()
                        + "' process uses unknown material '" + declaration.materialName() + "'");
            var key = new MaterialKey(material.getName());
            registerProcessType(processTypes, declaration.process(), "addon '" + submission.addonId() + "'");
            addProcessRule(
                    processRules.get(key),
                    declaration.process(),
                    new MaterialProcessRule<>(declaration.forms(), declaration.value()));
        }
        var immutableProcesses =
                new LinkedHashMap<MaterialKey, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>>();
        processRules.forEach((material, byProcess) -> {
            var copied = new LinkedHashMap<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>();
            byProcess.forEach((key, rules) -> copied.put(key, List.copyOf(rules)));
            immutableProcesses.put(material, Map.copyOf(copied));
        });
        MaterialProcessesView processView = new MaterialProcessesView() {
            public <T> List<MaterialProcessRule<T>> find(MaterialKey material, MaterialProcessKey<T> process) {
                Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> byProcess =
                        immutableProcesses.getOrDefault(java.util.Objects.requireNonNull(material), Map.of());
                List<MaterialProcessRule<?>> rules =
                        byProcess.getOrDefault(java.util.Objects.requireNonNull(process), List.of());
                return rules.stream()
                        .map(rule -> new MaterialProcessRule<T>(
                                rule.forms(), process.type().cast(rule.value())))
                        .toList();
            }

            public Set<MaterialProcessKey<?>> keys(MaterialKey material) {
                return immutableProcesses
                        .getOrDefault(java.util.Objects.requireNonNull(material), Map.of())
                        .keySet();
            }
        };
        var immutableTags = new LinkedHashMap<DataKey, Set<MaterialKey>>();
        tagMembers.forEach((key, value) -> immutableTags.put(key, Set.copyOf(value)));
        var immutableMaterialTags = new LinkedHashMap<MaterialKey, Set<DataKey>>();
        materialTags.forEach((key, value) -> immutableMaterialTags.put(key, Set.copyOf(value)));
        var tags = new MaterialTagsView() {
            public Set<DataKey> tags(MaterialKey key) {
                return immutableMaterialTags.getOrDefault(java.util.Objects.requireNonNull(key), Set.of());
            }

            public Set<MaterialKey> members(DataKey tag) {
                return immutableTags.getOrDefault(java.util.Objects.requireNonNull(tag), Set.of());
            }

            public Optional<MaterialGroupView> group(MaterialGroupKey key) {
                return Optional.ofNullable(groups.get(java.util.Objects.requireNonNull(key)));
            }
        };
        var indexed = Map.copyOf(materials);
        var ordered = List.copyOf(materials.values());
        var aliases = new LinkedHashMap<String, MaterialKey>();
        GTMaterialRegistry.registeredNames().forEach((name, material) -> {
            if (material.isValid() && indexed.containsKey(new MaterialKey(material.getName())))
                aliases.put(name, new MaterialKey(material.getName()));
        });
        published = new MaterialCatalogView() {
            public long revision() {
                return 1;
            }

            public Optional<MaterialKey> resolve(String name) {
                return Optional.ofNullable(aliases.get(GTMaterial.sanitize(java.util.Objects.requireNonNull(name))));
            }

            public MaterialTagsView tags() {
                return tags;
            }

            public Optional<MaterialView> find(MaterialKey key) {
                return Optional.ofNullable(indexed.get(java.util.Objects.requireNonNull(key)));
            }

            public List<MaterialView> materials() {
                return ordered;
            }

            public MaterialProcessesView processes() {
                return processView;
            }
        };
    }

    private static void requireNamespace(String addonId, DataKey key) {
        if (!addonId.equals(key.namespace()))
            throw new IllegalArgumentException("Addon '" + addonId + "' cannot declare key " + key);
    }

    private static void registerProcessType(Map<DataKey, Class<?>> types, MaterialProcessKey<?> key, String source) {
        var previous = types.putIfAbsent(key.id(), key.type());
        if (previous != null && previous != key.type())
            throw new IllegalArgumentException(
                    "Material process '" + key.id() + "' from " + source + " has conflicting value types");
    }

    private static void addProcessRule(
            Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> rules,
            MaterialProcessKey<?> key,
            MaterialProcessRule<?> rule) {
        List<MaterialProcessRule<?>> current = rules.computeIfAbsent(key, ignored -> new java.util.ArrayList<>());
        if (!current.contains(rule)) current.add(rule);
    }

    private static MaterialGroupKey groupKey(String name) {
        return new MaterialGroupKey(new DataKey("gt6ce", name.toLowerCase(java.util.Locale.ROOT)));
    }

    private static void putGroup(
            Map<MaterialGroupKey, GroupFacts> groups, MaterialGroupKey key, GroupFacts facts, String source) {
        var previous = groups.putIfAbsent(key, facts);
        if (previous != null)
            throw new IllegalArgumentException("Material group '" + key.id() + "' from " + source
                    + " conflicts with owner '" + previous.owner() + "'");
    }

    private static Set<MaterialKey> resolveTagMembers(
            DataKey tag,
            Map<DataKey, TagFacts> declarations,
            Map<DataKey, Set<MaterialKey>> directMembers,
            Map<DataKey, Set<MaterialKey>> resolved,
            Set<DataKey> path) {
        var memo = resolved.get(tag);
        if (memo != null) return memo;
        if (!path.add(tag)) throw new IllegalArgumentException("Material tag inclusion cycle: " + tag);
        var members = new LinkedHashSet<>(directMembers.getOrDefault(tag, Set.of()));
        for (var included : declarations.get(tag).includes())
            members.addAll(resolveTagMembers(included, declarations, directMembers, resolved, path));
        path.remove(tag);
        var immutable = Set.copyOf(members);
        resolved.put(tag, immutable);
        return immutable;
    }

    private static <T> void addProperty(
            String addonId,
            MaterialKey material,
            PropertyKey<T> property,
            MaterialMetadata metadata,
            MaterialMetadata.Builder builder,
            Map<PropertyKey<?>, Object> properties,
            Map<String, String> origins) {
        var value = metadata.property(property).orElseThrow();
        var previous = properties.putIfAbsent(property, value);
        String origin = material.name() + "/" + property.id();
        if (previous != null && !previous.equals(value))
            throw new IllegalArgumentException("Addon '" + addonId + "' metadata field " + origin
                    + " disagrees with addon '" + origins.get(origin) + "'");
        if (previous == null) {
            builder.property(property, value);
            origins.put(origin, addonId);
        }
    }

    private static Set<MaterialKey> resolveGroupMembers(
            MaterialGroupKey key,
            Map<MaterialGroupKey, GroupFacts> declarations,
            Map<MaterialGroupKey, MaterialGroupView> resolved,
            Set<MaterialGroupKey> path) {
        var done = resolved.get(key);
        if (done != null) return done.members();
        if (!path.add(key)) throw new IllegalArgumentException("Material group cycle: " + key.id());
        var result = new LinkedHashSet<MaterialKey>();
        result.addAll(declarations.get(key).materials());
        for (var included : declarations.get(key).includedGroups())
            result.addAll(resolveGroupMembers(included, declarations, resolved, path));
        path.remove(key);
        var members = Set.copyOf(result);
        resolved.put(key, new MaterialGroupView(key, declarations.get(key).displayName(), members));
        return members;
    }

    public static MaterialCatalogView catalog() {
        if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY || published == null)
            throw new IllegalStateException(
                    "Material catalog is not ready (" + GTMaterialRegistry.registrationPhase() + ")");
        return published;
    }
}
