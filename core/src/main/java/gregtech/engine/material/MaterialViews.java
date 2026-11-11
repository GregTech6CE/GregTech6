package gregtech.engine.material;

import gregtech.api.addon.material.*;
import gregtech.api.material.GTMaterial;
import gregtech.content.data.FluidCatalog;
import gregtech.content.material.MaterialDefinitions;
import gregtech.engine.addon.AddonMaterials;
import java.util.*;

/** Captures intrinsic definitions after every legal addon callback, independent of later native content. */
public final class MaterialViews {
    private MaterialViews() {}

    private record TagFacts(String owner, Set<DataKey> includes) {}

    private record GroupFacts(
            String owner, String displayName, Set<MaterialKey> materials, Set<MaterialGroupKey> includedGroups) {}

    private record MetadataSubmission(String addonId, MaterialMetadataDeclaration declaration) {}

    private record TagDeclarationSubmission(String addonId, MaterialTagDeclaration declaration) {}

    private record GroupSubmission(String addonId, MaterialGroupDeclaration declaration) {}

    private static MaterialCatalogView published;
    private static volatile MaterialFormsView formBindings = new MaterialFormsView() {
        public Set<MaterialFormBinding> find(MaterialKey material) {
            return Set.of();
        }
    };

    static void publish() {
        var builders = new LinkedHashMap<MaterialKey, MaterialMetadata.Builder>();
        var properties = new LinkedHashMap<MaterialKey, Map<PropertyKey<?>, Object>>();
        var types = new LinkedHashMap<DataKey, Class<?>>();
        var origins = new LinkedHashMap<String, String>();
        var directTagMembers = new LinkedHashMap<DataKey, Set<MaterialKey>>();
        var tagFacts = new LinkedHashMap<DataKey, TagFacts>();
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.resolve() != material) continue;
            var definition = MaterialDefinitions.find(material.getName());
            var relations = MaterialInfoRegistry.relationsForPublication(material.getName());
            if (!material.isValid() || definition != null && relations.group()) continue;
            var builder = MaterialMetadata.builder()
                    .property(MaterialProperties.MELTING_POINT, material.getMeltingPoint())
                    .property(MaterialProperties.BOILING_POINT, material.getBoilingPoint())
                    .property(MaterialProperties.DENSITY, material.getDensity())
                    .property(MaterialProperties.FORMULA, material.getTooltipChemical());
            for (var flag : material.getProperties())
                builder.flag(new DataKey("gt6ce", flag.name().toLowerCase(java.util.Locale.ROOT)));
            if (definition != null)
                for (String tag : relations.classifications()) {
                    var key = new DataKey("gt6ce", tag);
                    builder.tag(key);
                    tagFacts.putIfAbsent(key, new TagFacts("gt6ce", Set.of()));
                }
            var key = new MaterialKey(material.getName());
            builders.put(key, builder);
            properties.put(key, new LinkedHashMap<>());
            for (var property : builder.build().keys()) types.put(property.id(), property.type());
        }
        var metadataSubmissions = new java.util.ArrayList<MetadataSubmission>();
        for (var submission : AddonMaterials.metadataDeclarations())
            metadataSubmissions.add(new MetadataSubmission(submission.addonId(), submission.declaration()));
        for (var submission : MaterialInfoRegistry.metadataSubmissions())
            metadataSubmissions.add(new MetadataSubmission(submission.addonId(), submission.declaration()));
        for (var submission : metadataSubmissions) {
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
        for (var submission : MaterialInfoRegistry.tagSubmissions()) {
            GTMaterial material = GTMaterialRegistry.get(submission.material().name());
            if (!material.isValid())
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' tag uses unknown material '"
                        + submission.material().name() + "'");
            var key = new MaterialKey(material.getName());
            var builder = builders.get(key);
            if (builder == null)
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' cannot tag non-material '"
                        + submission.material().name() + "'");
            builder.tag(submission.tag());
            var previous = tagFacts.putIfAbsent(submission.tag(), new TagFacts(submission.addonId(), Set.of()));
            if (previous != null && !previous.owner().equals(submission.addonId()))
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' material tag '"
                        + submission.tag() + "' conflicts with owner '" + previous.owner() + "'");
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
        var tagDeclarationSubmissions = new java.util.ArrayList<TagDeclarationSubmission>();
        for (var submission : AddonMaterials.tagDeclarations())
            tagDeclarationSubmissions.add(new TagDeclarationSubmission(submission.addonId(), submission.declaration()));
        for (var submission : MaterialInfoRegistry.tagDeclarationSubmissions())
            tagDeclarationSubmissions.add(new TagDeclarationSubmission(submission.addonId(), submission.declaration()));
        for (var submission : tagDeclarationSubmissions) {
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
        for (var declaration : MaterialDefinitions.declarations()) {
            var definition = declaration.material();
            var relations = MaterialInfoRegistry.relationsForPublication(definition.name());
            if (!relations.group()) continue;
            var key = groupKey(definition.name());
            var groupMaterials = new LinkedHashSet<MaterialKey>();
            var includedGroups = new LinkedHashSet<MaterialGroupKey>();
            for (var member : relations.members()) {
                var memberDefinition = MaterialDefinitions.find(member);
                if (memberDefinition != null
                        && MaterialInfoRegistry.relationsForPublication(memberDefinition.name())
                                .group()) {
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
        var groupSubmissions = new java.util.ArrayList<GroupSubmission>();
        for (var submission : AddonMaterials.groupDeclarations())
            groupSubmissions.add(new GroupSubmission(submission.addonId(), submission.declaration()));
        for (var submission : MaterialInfoRegistry.groupSubmissions())
            groupSubmissions.add(new GroupSubmission(submission.addonId(), submission.declaration()));
        for (var submission : groupSubmissions) {
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

        var formRules = new LinkedHashMap<MaterialKey, Set<FormKey>>();
        var typedFormRules = new LinkedHashMap<MaterialKey, Set<MaterialFormRule>>();
        var allForms = new LinkedHashSet<FormKey>();
        for (var material : materials.keySet()) {
            formRules.put(material, new LinkedHashSet<>());
            typedFormRules.put(material, new LinkedHashSet<>());
        }
        var requestedForms = new LinkedHashSet<String>();
        for (var request : AddonMaterials.acceptedForms())
            requestedForms.add(request.materialName() + '\0' + request.prefixName());
        for (var definition : MaterialItemDefinitions.all()) {
            GTMaterial material = definition.material().resolve();
            if (!material.isValid()) continue;
            var materialKey = new MaterialKey(material.getName());
            if (!formRules.containsKey(materialKey)) continue;
            var form = new FormKey(definition.prefix().getName());
            formRules.get(materialKey).add(form);
            allForms.add(form);
            typedFormRules
                    .get(materialKey)
                    .add(new MaterialFormRule(
                            form,
                            MaterialFormKind.ITEM,
                            definition.prefix().getMaterialWeight(),
                            requestedForms.contains(materialKey.name() + '\0' + form.name()),
                            "material item definition"));
        }
        for (var definition : MaterialBlockDefinitions.all()) {
            GTMaterial material = definition.material().resolve();
            if (!material.isValid()) continue;
            var materialKey = new MaterialKey(material.getName());
            if (!typedFormRules.containsKey(materialKey)) continue;
            typedFormRules
                    .get(materialKey)
                    .add(new MaterialFormRule(
                            new FormKey(definition.prefix().getName()),
                            MaterialFormKind.BLOCK,
                            definition.prefix().getMaterialWeight(),
                            false,
                            "material block definition"));
        }
        for (var materialKey : typedFormRules.keySet()) {
            GTMaterial material = GTMaterialRegistry.get(materialKey.name());
            if (material.has(gregtech.api.material.MaterialProperty.MOLTEN))
                typedFormRules
                        .get(materialKey)
                        .add(new MaterialFormRule(
                                new FormKey("molten"), MaterialFormKind.FLUID, 0, false, "canonical MOLTEN flag"));
            if (material.has(gregtech.api.material.MaterialProperty.GAS))
                typedFormRules
                        .get(materialKey)
                        .add(new MaterialFormRule(
                                new FormKey("gas"), MaterialFormKind.FLUID, 0, false, "canonical GAS flag"));
            if (material.has(gregtech.api.material.MaterialProperty.LIQUID))
                typedFormRules
                        .get(materialKey)
                        .add(new MaterialFormRule(
                                new FormKey("liquid"), MaterialFormKind.FLUID, 0, false, "canonical LIQUID flag"));
        }
        for (var entry : FluidCatalog.all().values()) {
            String materialName = entry.materialKey() == null
                    ? FluidCatalog.boundMaterial(entry.registryName())
                    : entry.materialKey();
            if (materialName == null) continue;
            GTMaterial material = GTMaterialRegistry.get(materialName);
            if (!material.isValid()) continue;
            var materialKey = new MaterialKey(material.getName());
            if (!typedFormRules.containsKey(materialKey)) continue;
            String form = entry.textureMode() == FluidCatalog.FluidTextureMode.GENERIC_MOLTEN
                    ? "molten"
                    : entry.gas() ? "gas" : "liquid";
            var rule = new MaterialFormRule(new FormKey(form), MaterialFormKind.FLUID, 0, false, "fluid catalog");
            if (typedFormRules.get(materialKey).stream()
                    .anyMatch(existing -> existing.kind() == MaterialFormKind.FLUID
                            && existing.form().equals(rule.form()))) continue;
            typedFormRules.get(materialKey).add(rule);
        }
        var immutableFormRules = new LinkedHashMap<MaterialKey, Set<FormKey>>();
        formRules.forEach((material, forms) -> immutableFormRules.put(material, Set.copyOf(forms)));
        var immutableAllForms = Set.copyOf(allForms);
        var immutableTypedForms = new LinkedHashMap<MaterialKey, Set<MaterialFormRule>>();
        typedFormRules.forEach((material, forms) -> immutableTypedForms.put(material, Set.copyOf(forms)));
        MaterialFormRulesView formView = new MaterialFormRulesView() {
            public Set<FormKey> forms() {
                return immutableAllForms;
            }

            public Set<FormKey> forms(MaterialKey material) {
                return immutableFormRules.getOrDefault(java.util.Objects.requireNonNull(material), Set.of());
            }

            public boolean allows(MaterialKey material, FormKey form) {
                return forms(material).contains(java.util.Objects.requireNonNull(form));
            }

            public Set<MaterialFormRule> rules() {
                return immutableTypedForms.values().stream()
                        .flatMap(Set::stream)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
            }

            public Set<MaterialFormRule> rules(MaterialKey material) {
                return immutableTypedForms.getOrDefault(java.util.Objects.requireNonNull(material), Set.of());
            }
        };

        var processTypes = new LinkedHashMap<DataKey, MaterialProcessKey<?>>();
        var processRules = new LinkedHashMap<MaterialKey, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>>();
        for (var material : materials.keySet()) processRules.put(material, new LinkedHashMap<>());
        for (var material : materials.keySet()) {
            MaterialInfoRegistry.processesForPublication(material).rules().forEach((key, rules) -> {
                String source = "linked material catalog";
                registerProcessType(processTypes, key, source);
                for (var rule : rules)
                    addProcessRule(
                            processRules.get(material),
                            key,
                            MaterialInfoRegistry.normalizeProcessRule(source, key, rule));
            });
        }
        for (var submission : AddonMaterials.processDeclarations()) {
            var declaration = submission.declaration();
            GTMaterial material = GTMaterialRegistry.get(declaration.materialName());
            if (!material.isValid())
                throw new IllegalArgumentException("Addon '" + submission.addonId()
                        + "' process uses unknown material '" + declaration.materialName() + "'");
            if (!declaration.process().equals(MaterialProcessKeys.DECOMPOSITION))
                requireNamespace(submission.addonId(), declaration.process().id());
            else if (!AddonMaterials.owners(material).contains(submission.addonId()))
                throw new IllegalArgumentException("Addon '" + submission.addonId()
                        + "' cannot declare decomposition for non-owned material '" + material.getName() + "'");
            var key = new MaterialKey(material.getName());
            var targetRules = processRules.get(key);
            if (targetRules == null)
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' process uses non-material '"
                        + declaration.materialName() + "'");
            String source = "addon '" + submission.addonId() + "'";
            registerProcessType(processTypes, declaration.process(), source);
            addProcessRule(
                    targetRules,
                    declaration.process(),
                    MaterialInfoRegistry.normalizeProcessRule(
                            source,
                            declaration.process(),
                            new MaterialProcessRule<>(declaration.forms(), declaration.value())));
        }
        for (var submission : MaterialInfoRegistry.processSubmissions()) {
            GTMaterial material = GTMaterialRegistry.get(submission.material().name());
            if (!material.isValid())
                throw new IllegalArgumentException(
                        "Addon '" + submission.addonId() + "' process uses unknown material '"
                                + submission.material().name() + "'");
            if (submission.process().equals(MaterialProcessKeys.DECOMPOSITION)
                    && !AddonMaterials.owners(material).contains(submission.addonId()))
                throw new IllegalArgumentException("Addon '" + submission.addonId()
                        + "' cannot declare decomposition for non-owned material '" + material.getName() + "'");
            var key = new MaterialKey(material.getName());
            var targetRules = processRules.get(key);
            if (targetRules == null)
                throw new IllegalArgumentException("Addon '" + submission.addonId() + "' process uses non-material '"
                        + submission.material().name() + "'");
            String source = "addon '" + submission.addonId() + "'";
            registerProcessType(processTypes, submission.process(), source);
            @SuppressWarnings("unchecked")
            var process = (MaterialProcessKey<Object>) submission.process();
            @SuppressWarnings("unchecked")
            var rule = (MaterialProcessRule<Object>) submission.rule();
            addProcessRule(targetRules, process, MaterialInfoRegistry.normalizeProcessRule(source, process, rule));
        }
        validateProcessForms(processRules, immutableTypedForms);
        validateProcessCardinality(processRules);
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
            GTMaterial canonical = material.resolve();
            if (!canonical.isValid()) return;
            var key = new MaterialKey(canonical.getName());
            if (indexed.containsKey(key)) aliases.put(name, key);
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

            public MaterialFormRulesView forms() {
                return formView;
            }

            public MaterialFormsView formBindings() {
                return formBindings;
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

    private static void registerProcessType(
            Map<DataKey, MaterialProcessKey<?>> processes, MaterialProcessKey<?> key, String source) {
        var previous = processes.putIfAbsent(key.id(), key);
        if (previous != null && (previous.type() != key.type() || previous.cardinality() != key.cardinality()))
            throw new IllegalArgumentException(
                    "Material process '" + key.id() + "' from " + source + " has conflicting type or cardinality");
    }

    private static void addProcessRule(
            Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> rules,
            MaterialProcessKey<?> key,
            MaterialProcessRule<?> rule) {
        List<MaterialProcessRule<?>> current = rules.computeIfAbsent(key, ignored -> new java.util.ArrayList<>());
        if (key.cardinality() == MaterialProcessCardinality.SINGLE && !current.isEmpty())
            throw new IllegalArgumentException(
                    "Single-valued process " + key.id() + " already has a rule for this material");
        if (!current.contains(rule)) current.add(rule);
    }

    private static void validateProcessCardinality(
            Map<MaterialKey, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>> processRules) {
        processRules.forEach((material, byProcess) -> byProcess.forEach((key, rules) -> {
            if (key.cardinality() == MaterialProcessCardinality.SINGLE && rules.size() != 1)
                throw new IllegalArgumentException("Material " + material.name() + " single-valued process " + key.id()
                        + " must have exactly one rule, found " + rules.size());
        }));
    }

    private static void validateProcessForms(
            Map<MaterialKey, Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>> processRules,
            Map<MaterialKey, Set<MaterialFormRule>> formRules) {
        processRules.forEach((material, byProcess) -> byProcess.forEach((process, rules) -> {
            var allowed = formRules.getOrDefault(material, Set.of()).stream()
                    .map(MaterialFormRule::form)
                    .collect(java.util.stream.Collectors.toSet());
            for (var rule : rules)
                for (var form : rule.forms())
                    if (!allowed.contains(form))
                        throw new IllegalArgumentException("Material " + material.name() + " process " + process.id()
                                + " names missing form '" + form.name() + "'");
        }));
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

    /** Publishes registered native forms after the platform registry has finished binding content. */
    public static synchronized void publishContent(List<MaterialFormBinding> bindings) {
        if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY || published == null)
            throw new IllegalStateException("Material definitions must be READY before content bindings");
        var byMaterial = new LinkedHashMap<MaterialKey, Set<MaterialFormBinding>>();
        for (var material : published.materials()) byMaterial.put(material.key(), new LinkedHashSet<>());
        for (var binding : bindings) {
            var values = byMaterial.get(binding.material());
            if (values == null)
                throw new IllegalArgumentException("Native form binding uses unknown material "
                        + binding.material().name());
            var matchingRules = published.forms().rules(binding.material()).stream()
                    .filter(rule -> rule.form().equals(binding.form()))
                    .toList();
            if (!matchingRules.isEmpty() && matchingRules.stream().noneMatch(rule -> rule.kind() == binding.kind()))
                throw new IllegalArgumentException("Native form binding kind conflicts with definition rule: "
                        + binding.material().name() + " / " + binding.form().name() + " / " + binding.kind());
            values.add(binding);
        }
        var immutable = new LinkedHashMap<MaterialKey, Set<MaterialFormBinding>>();
        byMaterial.forEach((material, values) -> immutable.put(material, Set.copyOf(values)));
        formBindings = new MaterialFormsView() {
            public Set<MaterialFormBinding> find(MaterialKey material) {
                return immutable.getOrDefault(java.util.Objects.requireNonNull(material), Set.of());
            }
        };
    }
}
