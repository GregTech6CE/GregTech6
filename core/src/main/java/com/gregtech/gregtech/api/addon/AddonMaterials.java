package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.machine.crucible.CrucibleReactions;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialComponent;
import com.gregtech.gregtech.api.material.MaterialDefinition;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

/** Queued addon materials and extra forms. GregTech drains this once, from {@link GTMaterialRegistry#init()}. */
public final class AddonMaterials {
    /**
     * @deprecated Addon materials no longer pick a numeric id. Use {@code MaterialDeclaration.builder(String)}. Removal is 0.9.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MIN", removeAt = "0.9")
    public static final int ADDON_ID_MIN = 4291;

    /**
     * @deprecated Addon materials no longer pick a numeric id. Use {@code MaterialDeclaration.builder(String)}. Removal is 0.9.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MAX", removeAt = "0.9")
    public static final int ADDON_ID_MAX = 7899;

    private static final MaterialProperty[] METAL_FLAGS = {
            MaterialProperty.METAL, MaterialProperty.INGOT, MaterialProperty.DUST, MaterialProperty.GENERATE_DUST,
            MaterialProperty.GENERATE_INGOT, MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_STICKS,
            MaterialProperty.GENERATE_PARTS, MaterialProperty.GENERATE_FOIL, MaterialProperty.GENERATE_MULTIINGOT,
            MaterialProperty.GENERATE_MULTIPLATE, MaterialProperty.GENERATE_DENSEPLATE, MaterialProperty.GENERATE_WIRE,
            MaterialProperty.SMITHABLE, MaterialProperty.UNIT
    };

    private static final TreeMap<String, Submission> QUEUE = new TreeMap<>();
    private static final Map<GTMaterial, List<String>> OWNERS = new IdentityHashMap<>();
    private static boolean collected;
    private static boolean closed;
    private static volatile List<AcceptedForm> acceptedForms = List.of();
    private static final Set<GTMaterial> ORE_FORMS = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<GTMaterial> POLYMERS = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<GTMaterial> WIRES = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<QueuedByproducts> QUEUED_BYPRODUCTS = new ArrayList<>();

    private AddonMaterials() {}

    public record AcceptedForm(String addonId, String materialName, String prefixName) {}

    /**
     * {@code polymer()} allows {@code plate} and {@code foil}. {@code wires()} allows {@code wireFine}.
     * Core materials and every other prefix keep {@code MaterialPrefix.isValidFor}.
     */
    public static boolean explicitPlateOrFoil(com.gregtech.gregtech.data.MaterialPrefix prefix, GTMaterial material) {
        if (prefix == null || material == null || !material.isValid() || GTMaterialRegistry.isCoreMaterial(material.resolve()))
            return false;
        String name = prefix.getName();
        GTMaterial resolved = material.resolve();
        if (("plate".equals(name) || "foil".equals(name)) && POLYMERS.contains(resolved)) return true;
        return "wireFine".equals(name) && WIRES.contains(resolved);
    }

    private static final Set<String> COAL = Set.of(
            "Coal", "Charcoal", "CoalCoke", "Lignite", "LigniteCoke", "Anthracite", "HydratedCoal", "PetroleumCoke");
    private static final Set<String> MEAT = Set.of("MeatRaw", "MeatCooked", "MeatRotten");

    private record Submission(String addonId, List<MaterialDefinition> materials,
                              List<MaterialFormRequest> forms, Runnable link,
                              List<MaterialComposition> compositions,
                              List<SmeltingTargetDeclaration> smeltingTargets,
                              List<CrucibleReactionDeclaration> crucibleReactions,
                              List<CrucibleReactionRemoval> crucibleRemovals,
                              List<PrefixDeclaration> prefixes,
                              List<OreDeclaration> ores,
                              List<OreVeinDeclaration> veins,
                              List<SmallOreDeclaration> smallOres,
                              List<FormTag> formTags) {}

    private record OreDeclaration(String materialName, List<String> byproducts) {}

    private record FormTag(String materialName, boolean polymer, boolean wires) {}

    private record QueuedByproducts(String addonId, GTMaterial material, List<String> byproducts) {}

    private record Pending(String addonId, MaterialDefinition definition, OreDeclaration ore, FormTag formTag) {}

    /** Flags applied by {@link MaterialDeclaration.Builder#metal()}. */
    public static MaterialProperty[] metalFlags() { return METAL_FLAGS.clone(); }

    /** Load contributors from the class loader that loaded this API, then keep the result. */
    public static void collectContributors() {
        collectContributors(GregTechMaterialContributor.class.getClassLoader());
    }

    /** Test and platform entry. A second call does not load services again. */
    public static synchronized void collectContributors(ClassLoader loader) {
        if (collected) return;
        if (closed) throw new IllegalStateException("Addon material contributors are closed");
        collected = true;
        Objects.requireNonNull(loader, "loader");
        for (GregTechMaterialContributor contributor : ServiceLoader.load(GregTechMaterialContributor.class, loader)) {
            try {
                submit(contributor.addonId(), contributor.materials(), contributor.forms(), contributor::link);
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon material contributor failed: "
                        + contributor.getClass().getName(), failure);
            }
        }
        for (AddonMaterialContributor contributor : ServiceLoader.load(AddonMaterialContributor.class, loader)) {
            try {
                List<MaterialDeclaration> declarations = contributor.materials();
                warnIgnoredDeclarationIds(contributor.addonId(), declarations);
                enqueue(contributor.addonId(), definitions(declarations), contributor.forms(), contributor::link,
                        contributor.compositions(), contributor.smeltingTargets(),
                        contributor.crucibleReactions(), contributor.crucibleRemovals(), contributor.prefixes(),
                        oreDeclarations(declarations), contributor.oreVeins(), contributor.smallOres(),
                        formTags(declarations));
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon material contributor failed: "
                        + contributor.getClass().getName(), failure);
            }
        }
    }

    private static void warnIgnoredDeclarationIds(String addonId, List<MaterialDeclaration> declarations) {
        for (MaterialDeclaration declaration : declarations) {
            if (declaration.id() == 0) continue;
            GTMaterialRegistry.warn("Addon '" + addonId + "' material '" + declaration.name()
                    + "' declared numeric id " + declaration.id() + "; the id is ignored");
        }
    }

    private static List<MaterialDefinition> definitions(List<MaterialDeclaration> declarations) {
        var definitions = new ArrayList<MaterialDefinition>();
        for (MaterialDeclaration declaration : declarations) {
            var builder = MaterialDefinition.builder(1, declaration.name())
                    .displayName(declaration.displayName())
                    .color(declaration.color())
                    .chemicalFormula(declaration.formula())
                    .meltingPointKelvin(declaration.meltingPoint())
                    .boilingPointKelvin(declaration.boilingPoint())
                    .density(declaration.density());
            if (declaration.metal()) builder.properties(METAL_FLAGS);
            else if (declaration.dust()) builder.dust();
            if (declaration.gas()) builder.gas();
            if (declaration.liquid()) builder.liquid();
            definitions.add(builder.build());
        }
        return List.copyOf(definitions);
    }

    /**
     * Direct queue used by tests and by the deprecated {@link GregTechMaterialContributor}.
     * The numeric id on each definition is ignored. Duplicate addon ids and calls after the queue closes fail immediately.
     */
    public static synchronized void submit(String addonId, List<MaterialDefinition> materials,
                                           List<MaterialFormRequest> forms, Runnable link) {
        submit(addonId, materials, forms, link, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** Test entry for a declaration that carries {@link MaterialDeclaration#ores()}. */
    public static synchronized void submitDeclared(String addonId, List<MaterialDeclaration> materials,
                                                   List<MaterialFormRequest> forms) {
        submitDeclared(addonId, materials, forms, List.of());
    }

    /** Test entry. Production addons return prefixes from {@link AddonMaterialContributor#prefixes()}. */
    public static synchronized void submitDeclared(String addonId, List<MaterialDeclaration> materials,
                                                   List<MaterialFormRequest> forms, List<PrefixDeclaration> prefixes) {
        List<MaterialDeclaration> copied = List.copyOf(Objects.requireNonNull(materials, "materials"));
        warnIgnoredDeclarationIds(addonId, copied);
        enqueue(addonId, definitions(copied), forms, () -> {}, List.of(), List.of(), List.of(), List.of(),
                List.copyOf(Objects.requireNonNull(prefixes, "prefixes")),
                oreDeclarations(copied), List.of(), List.of(), formTags(copied));
    }

    /** Test entry. Production addons return these lists from {@link AddonMaterialContributor}. */
    public static synchronized void submit(String addonId, List<MaterialDefinition> materials,
                                           List<MaterialFormRequest> forms, Runnable link,
                                           List<MaterialComposition> compositions,
                                           List<SmeltingTargetDeclaration> smeltingTargets,
                                           List<CrucibleReactionDeclaration> crucibleReactions,
                                           List<CrucibleReactionRemoval> crucibleRemovals) {
        submit(addonId, materials, forms, link, compositions, smeltingTargets, crucibleReactions, crucibleRemovals,
                List.of(), List.of(), List.of(), List.of());
    }

    /** Test entry. Production addons return prefixes from {@link AddonMaterialContributor#prefixes()}. */
    public static synchronized void submit(String addonId, List<MaterialDefinition> materials,
                                           List<MaterialFormRequest> forms, Runnable link,
                                           List<MaterialComposition> compositions,
                                           List<SmeltingTargetDeclaration> smeltingTargets,
                                           List<CrucibleReactionDeclaration> crucibleReactions,
                                           List<CrucibleReactionRemoval> crucibleRemovals,
                                           List<PrefixDeclaration> prefixes) {
        submit(addonId, materials, forms, link, compositions, smeltingTargets, crucibleReactions, crucibleRemovals,
                prefixes, List.of(), List.of(), List.of());
    }

    /** Test entry. Production addons return veins from {@link AddonMaterialContributor#oreVeins()}. */
    public static synchronized void submitWorldgen(String addonId, List<MaterialDeclaration> materials,
                                                   List<OreVeinDeclaration> veins, List<SmallOreDeclaration> smallOres) {
        List<MaterialDeclaration> copied = List.copyOf(Objects.requireNonNull(materials, "materials"));
        warnIgnoredDeclarationIds(addonId, copied);
        enqueue(addonId, definitions(copied), List.of(), () -> {}, List.of(), List.of(), List.of(), List.of(), List.of(),
                oreDeclarations(copied), veins, smallOres, formTags(copied));
    }

    private static synchronized void submit(String addonId, List<MaterialDefinition> materials,
                                            List<MaterialFormRequest> forms, Runnable link,
                                            List<MaterialComposition> compositions,
                                            List<SmeltingTargetDeclaration> smeltingTargets,
                                            List<CrucibleReactionDeclaration> crucibleReactions,
                                            List<CrucibleReactionRemoval> crucibleRemovals,
                                            List<PrefixDeclaration> prefixes,
                                            List<OreDeclaration> ores,
                                            List<OreVeinDeclaration> veins,
                                            List<SmallOreDeclaration> smallOres) {
        List<MaterialDefinition> copied = List.copyOf(Objects.requireNonNull(materials, "materials"));
        for (MaterialDefinition definition : copied) {
            GTMaterialRegistry.warn("Addon '" + addonId + "' material '" + definition.name()
                    + "' declared numeric id " + definition.id() + "; the id is ignored");
        }
        enqueue(addonId, copied, forms, link, compositions, smeltingTargets, crucibleReactions, crucibleRemovals,
                prefixes, ores, veins, smallOres, List.of());
    }

    private static synchronized void enqueue(String addonId, List<MaterialDefinition> materials,
                                             List<MaterialFormRequest> forms, Runnable link,
                                             List<MaterialComposition> compositions,
                                             List<SmeltingTargetDeclaration> smeltingTargets,
                                             List<CrucibleReactionDeclaration> crucibleReactions,
                                             List<CrucibleReactionRemoval> crucibleRemovals,
                                             List<PrefixDeclaration> prefixes,
                                             List<OreDeclaration> ores,
                                             List<OreVeinDeclaration> veins,
                                             List<SmallOreDeclaration> smallOres,
                                             List<FormTag> formTags) {
        if (closed) throw new IllegalStateException("Addon material queue is closed: " + addonId);
        if (addonId == null || !addonId.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("Invalid addon mod ID: " + addonId);
        if (addonId.equals("gt6ce") || addonId.equals("gregtech6") || addonId.equals("minecraft"))
            throw new IllegalArgumentException("Addon mod ID is reserved: " + addonId);
        if (QUEUE.containsKey(addonId))
            throw new IllegalStateException("Addon '" + addonId + "' already submitted materials");
        List<MaterialDefinition> copiedMaterials = List.copyOf(Objects.requireNonNull(materials, "materials"));
        List<MaterialFormRequest> copiedForms = List.copyOf(Objects.requireNonNull(forms, "forms"));
        QUEUE.put(addonId, new Submission(addonId, copiedMaterials, copiedForms, link == null ? () -> {} : link,
                List.copyOf(Objects.requireNonNull(compositions, "compositions")),
                List.copyOf(Objects.requireNonNull(smeltingTargets, "smeltingTargets")),
                List.copyOf(Objects.requireNonNull(crucibleReactions, "crucibleReactions")),
                List.copyOf(Objects.requireNonNull(crucibleRemovals, "crucibleRemovals")),
                List.copyOf(Objects.requireNonNull(prefixes, "prefixes")),
                List.copyOf(Objects.requireNonNull(ores, "ores")),
                List.copyOf(Objects.requireNonNull(veins, "veins")),
                List.copyOf(Objects.requireNonNull(smallOres, "smallOres")),
                List.copyOf(Objects.requireNonNull(formTags, "formTags"))));
    }

    private static List<OreDeclaration> oreDeclarations(List<MaterialDeclaration> declarations) {
        var ores = new ArrayList<OreDeclaration>();
        for (MaterialDeclaration declaration : declarations) {
            if (!declaration.ores()) continue;
            ores.add(new OreDeclaration(GTMaterial.sanitize(declaration.name()), declaration.oreByproducts()));
        }
        return List.copyOf(ores);
    }

    private static List<FormTag> formTags(List<MaterialDeclaration> declarations) {
        var tags = new ArrayList<FormTag>();
        for (MaterialDeclaration declaration : declarations) {
            if (!declaration.polymer() && !declaration.wires()) continue;
            tags.add(new FormTag(GTMaterial.sanitize(declaration.name()), declaration.polymer(), declaration.wires()));
        }
        return List.copyOf(tags);
    }

    /** True when this addon material declared {@link MaterialDeclaration.Builder#ores()}. */
    public static boolean hasOreForm(GTMaterial material) {
        return material != null && material.isValid() && ORE_FORMS.contains(material.resolve());
    }

    public static synchronized void closeQueue() {
        closed = true;
    }

    public static boolean owns(GTMaterial material) {
        return material != null && OWNERS.containsKey(material.resolve());
    }

    /** First declaring addon, in addon-id order. Null for a built-in material. */
    public static String owner(GTMaterial material) {
        List<String> owners = owners(material);
        return owners.isEmpty() ? null : owners.get(0);
    }

    /** Declaring addons, in addon-id order. Empty for a built-in material. */
    public static List<String> owners(GTMaterial material) {
        if (material == null) return List.of();
        List<String> owners = OWNERS.get(material.resolve());
        return owners == null ? List.of() : List.copyOf(owners);
    }

    public static List<AcceptedForm> acceptedForms() {
        return acceptedForms;
    }

    /** Called from material init after the built-in declarations and before linking. */
    public static void installDefinitions() {
        Map<String, List<Pending>> grouped = new LinkedHashMap<>();
        Map<String, Pending> exactByLower = new LinkedHashMap<>();
        for (Submission submission : snapshot()) {
            for (MaterialDefinition definition : submission.materials) {
                String exact = GTMaterial.sanitize(definition.name());
                String lower = exact.toLowerCase(Locale.ROOT);
                String registered = GTMaterialRegistry.registeredNameIgnoringCase(exact);
                if (registered != null) {
                    if (registered.equals(exact)) throw new IllegalStateException(coreNameCollision(submission.addonId, definition));
                    throw new IllegalStateException("Addon '" + submission.addonId + "' material '" + exact
                            + "' differs only by case from core material '" + registered + "'");
                }
                OreDeclaration ore = oreOf(submission, exact);
                FormTag formTag = formTagOf(submission, exact);
                Pending seen = exactByLower.putIfAbsent(lower, new Pending(submission.addonId, definition, ore, formTag));
                if (seen != null && !seen.definition().name().equals(definition.name())) {
                    throw new IllegalStateException("Addon '" + submission.addonId + "' material '" + exact
                            + "' differs only by case from addon '" + seen.addonId() + "' material '"
                            + seen.definition().name() + "'");
                }
                grouped.computeIfAbsent(exact, ignored -> new ArrayList<>())
                        .add(new Pending(submission.addonId, definition, ore, formTag));
            }
        }
        List<String> names = new ArrayList<>(grouped.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            List<Pending> group = grouped.get(name);
            MaterialDefinition first = group.get(0).definition();
            for (int i = 1; i < group.size(); i++) {
                String field = difference(first, group.get(i).definition());
                if (field == null) field = oreDifference(group.get(0).ore(), group.get(i).ore());
                if (field == null) field = formTagDifference(group.get(0).formTag(), group.get(i).formTag());
                if (field != null) {
                    throw new IllegalStateException("Addon '" + group.get(i).addonId() + "' material '" + name
                            + "' field " + field + " does not match addon '" + group.get(0).addonId() + "'");
                }
            }
            GTMaterial material = GTMaterialRegistry.installAddonMaterial(first, GTMaterialRegistry.allocateAddonId());
            List<String> owners = new ArrayList<>();
            for (Pending pending : group) {
                if (!owners.contains(pending.addonId())) owners.add(pending.addonId());
            }
            material.setSourceMod(new ModData(owners.get(0), owners.get(0)));
            OWNERS.put(material.resolve(), owners);
            if (group.get(0).ore() != null) {
                ORE_FORMS.add(material.resolve());
                QUEUED_BYPRODUCTS.add(new QueuedByproducts(group.get(0).addonId(), material.resolve(),
                        group.get(0).ore().byproducts()));
            }
            FormTag tag = group.get(0).formTag();
            if (tag != null && tag.polymer()) POLYMERS.add(material.resolve());
            if (tag != null && tag.wires()) WIRES.add(material.resolve());
        }
    }

    private static OreDeclaration oreOf(Submission submission, String name) {
        for (OreDeclaration ore : submission.ores) if (ore.materialName().equals(name)) return ore;
        return null;
    }

    private static FormTag formTagOf(Submission submission, String name) {
        for (FormTag tag : submission.formTags) if (tag.materialName().equals(name)) return tag;
        return new FormTag(name, false, false);
    }

    private static String formTagDifference(FormTag left, FormTag right) {
        FormTag a = left == null ? new FormTag("", false, false) : left;
        FormTag b = right == null ? new FormTag("", false, false) : right;
        if (a.polymer() != b.polymer()) return "polymer";
        if (a.wires() != b.wires()) return "wires";
        return null;
    }

    private static String oreDifference(OreDeclaration left, OreDeclaration right) {
        if ((left == null) != (right == null)) return "ores";
        if (left != null && !left.byproducts().equals(right.byproducts())) return "oreByproducts";
        return null;
    }

    /** Veins resolve after linking, so a row can name a material created in {@code link()}. */
    public static void publishOreVeins() {
        var queued = new ArrayList<com.gregtech.gregtech.worldgen.AddonOreVeins.Submission>();
        for (Submission submission : snapshot()) {
            queued.add(new com.gregtech.gregtech.worldgen.AddonOreVeins.Submission(
                    submission.addonId, submission.veins, submission.smallOres));
        }
        com.gregtech.gregtech.worldgen.AddonOreVeins.publish(queued);
    }

    /** Byproduct names resolve after linking, so a list can name a material created in {@code link()}. */
    public static void publishOreByproducts() {
        for (QueuedByproducts queued : QUEUED_BYPRODUCTS) {
            var resolved = new ArrayList<GTMaterial>();
            for (String name : queued.byproducts) {
                GTMaterial found = GTMaterialRegistry.get(name);
                if (!found.isValid()) throw new IllegalStateException("Addon '" + queued.addonId
                        + "' ore byproduct of '" + queued.material.getName() + "' uses unknown material '" + name + "'");
                GTMaterial byproduct = found.resolve();
                if (byproduct.has(MaterialProperty.HIDDEN)) throw new IllegalStateException("Addon '" + queued.addonId
                        + "' ore byproduct of '" + queued.material.getName() + "' cannot use hidden material '"
                        + byproduct.getName() + "'");
                resolved.add(byproduct);
            }
            if (queued.material.getByProducts().isEmpty()) {
                queued.material.addOreByProducts(resolved.toArray(GTMaterial[]::new));
                continue;
            }
            var existing = queued.material.getByProducts();
            if (existing.size() != resolved.size()) throw byproductMismatch(queued);
            for (int i = 0; i < existing.size(); i++) {
                if (existing.get(i) != resolved.get(i)) throw byproductMismatch(queued);
            }
        }
    }

    private static IllegalStateException byproductMismatch(QueuedByproducts queued) {
        return new IllegalStateException("Addon '" + queued.addonId + "' ore byproducts of '"
                + queued.material.getName() + "' do not match the existing byproducts");
    }

    /** Called after the built-in link, so callbacks can see core materials and link-time groups. */
    public static void linkAddons() {
        for (Submission submission : snapshot()) {
            Set<GTMaterial> before = new HashSet<>(GTMaterialRegistry.allMaterials());
            try {
                submission.link.run();
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon '" + submission.addonId + "' material link failed", failure);
            }
            for (GTMaterial created : GTMaterialRegistry.allMaterials()) {
                if (before.contains(created)) continue;
                GTMaterial canonical = created.resolve();
                if (canonical.getId() < GTMaterialRegistry.ADDON_ID_START)
                    throw new IllegalArgumentException("Addon '" + submission.addonId
                            + "' created material '" + canonical.getName() + "' id " + canonical.getId()
                            + "; addon materials are assigned from " + GTMaterialRegistry.ADDON_ID_START);
                addOwner(canonical, submission.addonId);
                if (canonical.getSourceMod() == null)
                    canonical.setSourceMod(new ModData(submission.addonId, submission.addonId));
            }
        }
        sealCrucibleAdditions();
    }

    /**
     * Addon prefixes register after every built-in prefix and before form checks.
     * A colliding name fails startup. Items for these prefixes are appended later.
     */
    public static void publishPrefixes() {
        for (Submission submission : snapshot()) {
            for (PrefixDeclaration declaration : submission.prefixes) installPrefix(submission.addonId, declaration);
        }
    }

    private static void installPrefix(String addonId, PrefixDeclaration declaration) {
        if (declaration.block()) {
            installBlockPrefix(addonId, declaration);
            return;
        }
        MaterialPrefix existing = PrefixRegistry.conflict(declaration.name());
        if (existing != null) throw new IllegalStateException("Addon '" + addonId + "' prefix '"
                + declaration.name() + "' already exists as '" + existing.getName() + "'");
        if (com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.byName(declaration.name()) != null)
            throw new IllegalStateException("Addon '" + addonId + "' prefix '" + declaration.name()
                    + "' already exists as a block prefix");
        Set<GTMaterial> selected = selectPrefixMaterials(addonId, declaration);
        long weight;
        try {
            weight = Math.multiplyExact(declaration.unitAmount(), GTValues.U);
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("Addon '" + addonId + "' prefix '" + declaration.name()
                    + "' unit amount overflows", overflow);
        }
        String label = (declaration.displayPrefix() + declaration.displaySuffix()).trim();
        MaterialPrefix.defineAddon(declaration.name(), label, declaration.displayPrefix(), declaration.displaySuffix(),
                weight, addonId, declaration.texture(), material -> selected.contains(material.resolve()));
    }

    private static void installBlockPrefix(String addonId, PrefixDeclaration declaration) {
        if (PrefixRegistry.conflict(declaration.name()) != null
                || com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.byName(declaration.name()) != null)
            throw new IllegalStateException("Addon '" + addonId + "' block prefix '" + declaration.name()
                    + "' already exists");
        Set<GTMaterial> selected = selectPrefixMaterials(addonId, declaration);
        long weight;
        try {
            weight = Math.multiplyExact(declaration.unitAmount(), GTValues.U);
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("Addon '" + addonId + "' prefix '" + declaration.name()
                    + "' unit amount overflows", overflow);
        }
        String label = (declaration.displayPrefix() + declaration.displaySuffix()).trim();
        com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.defineAddon(declaration.name(), label,
                declaration.displayPrefix(), declaration.displaySuffix(), weight, addonId, declaration.texture(),
                declaration.oreDictionary(), material -> selected.contains(material.resolve()));
    }

    private static Set<GTMaterial> selectPrefixMaterials(String addonId, PrefixDeclaration declaration) {
        boolean positive = false;
        for (PrefixCondition condition : declaration.conditions()) if (condition.positive()) positive = true;
        var selected = java.util.Collections.newSetFromMap(new IdentityHashMap<GTMaterial, Boolean>());
        for (String name : declaration.materials()) {
            GTMaterial found = GTMaterialRegistry.get(name);
            if (!found.isValid()) throw new IllegalStateException("Addon '" + addonId + "' prefix '"
                    + declaration.name() + "' uses unknown material '" + name + "'");
            GTMaterial material = found.resolve();
            if (material.has(MaterialProperty.HIDDEN)) throw new IllegalStateException("Addon '" + addonId
                    + "' prefix '" + declaration.name() + "' cannot use hidden material '" + material.getName() + "'");
            if (!positive) {
                for (PrefixCondition condition : declaration.conditions()) {
                    if (!matches(condition, material)) throw new IllegalStateException("Addon '" + addonId
                            + "' prefix '" + declaration.name() + "' material '" + material.getName()
                            + "' does not match " + condition);
                }
            }
            selected.add(material);
        }
        if (positive) {
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                if (!material.isValid() || material.resolve() != material || material.has(MaterialProperty.HIDDEN)) continue;
                boolean matches = true;
                for (PrefixCondition condition : declaration.conditions()) {
                    if (!matches(condition, material)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) selected.add(material);
            }
        }
        for (String name : declaration.excludedMaterials()) {
            GTMaterial found = GTMaterialRegistry.get(name);
            if (found.isValid()) selected.remove(found.resolve());
        }
        if (selected.isEmpty()) throw new IllegalStateException("Addon '" + addonId + "' prefix '"
                + declaration.name() + "' matches no material");
        return selected;
    }

    private static boolean matches(PrefixCondition condition, GTMaterial material) {
        return switch (condition) {
            case HAS_TOOL_STATS -> material.getToolQuality() >= 1;
            case SMITHABLE -> material.has(MaterialProperty.SMITHABLE);
            case NOT_WOOD -> !material.has(MaterialProperty.WOOD);
            case NOT_COAL -> !COAL.contains(material.getName());
            case NOT_STONE -> !material.has(MaterialProperty.STONE);
            case NOT_FOOD -> !MaterialWorkability.isFood(material);
            case NOT_MEAT -> !MEAT.contains(material.getName());
            case METAL -> material.has(MaterialProperty.METAL);
            case MOLTEN -> material.has(MaterialProperty.MOLTEN) || material.has(MaterialProperty.MELTING);
            case ALLOY -> material.has(MaterialProperty.ALLOY);
        };
    }

    /** Prefix checks happen after linking so a form can name a material created during link. */
    public static void publishForms() {
        List<AcceptedForm> published = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Submission submission : snapshot()) {
            for (MaterialFormRequest request : submission.forms) {
                MaterialPrefix prefix = PrefixRegistry.byName(request.prefixName());
                GTMaterial material = GTMaterialRegistry.get(request.materialName());
                if (prefix == null) {
                    if (com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.byName(request.prefixName()) != null)
                        throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                                + request.prefixName() + " / " + request.materialName() + "' is a block prefix");
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + request.prefixName() + " / " + request.materialName() + "' uses an unknown prefix");
                }
                if (!material.isValid())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + request.prefixName() + " / " + request.materialName() + "' uses an unknown material");
                if (!com.gregtech.gregtech.api.material.MaterialItemDefinitions.candidatePrefixes().contains(prefix))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + prefix.getName() + " / " + material.resolve().getName()
                            + "' is not a generated form");
                if (!prefix.isValidFor(material.resolve()) && !explicitPlateOrFoil(prefix, material.resolve()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + prefix.getName() + " / " + material.resolve().getName()
                            + "' is not valid for that material");
                String key = prefix.getName() + "\0" + material.resolve().getName();
                if (!seen.add(key)) continue;
                published.add(new AcceptedForm(submission.addonId, material.resolve().getName(), prefix.getName()));
            }
        }
        acceptedForms = List.copyOf(published);
    }

    /** Compositions, smelting targets and crucible rows, after every {@code link()} callback. */
    private static void sealCrucibleAdditions() {
        var explicit = new ArrayList<CrucibleReactions.AddonRow>();
        var derived = new ArrayList<CrucibleReactions.AddonRow>();
        var removals = new ArrayList<CrucibleReactions.AddonRemoval>();
        var derivedMaterials = new IdentityHashMap<GTMaterial, Boolean>();
        var crucibleFlags = new IdentityHashMap<GTMaterial, Boolean>();
        for (Submission submission : snapshot()) {
            for (MaterialComposition composition : submission.compositions) {
                GTMaterial material = applyComposition(submission.addonId, composition, crucibleFlags);
                if (!composition.crucible() || derivedMaterials.put(material, Boolean.TRUE) != null) continue;
                var parts = new ArrayList<CrucibleReactions.Part>();
                for (MaterialComposition.Part part : composition.parts()) {
                    parts.add(new CrucibleReactions.Part(
                            material(submission.addonId, part.material(), "composition"), part.amount() / GTValues.U));
                }
                derived.add(new CrucibleReactions.AddonRow(submission.addonId,
                        new CrucibleReactions.Reaction(material, composition.divider(), List.copyOf(parts))));
            }
            for (SmeltingTargetDeclaration target : submission.smeltingTargets) applySmelting(submission.addonId, target);
            for (CrucibleReactionDeclaration reaction : submission.crucibleReactions) {
                explicit.add(new CrucibleReactions.AddonRow(submission.addonId,
                        resolveReaction(submission.addonId, reaction)));
            }
            for (CrucibleReactionRemoval removal : submission.crucibleRemovals) {
                removals.add(resolveRemoval(submission.addonId, removal));
            }
        }
        CrucibleReactions.seal(explicit, derived, removals);
    }

    private static GTMaterial applyComposition(String addonId, MaterialComposition composition,
                                               IdentityHashMap<GTMaterial, Boolean> flags) {
        GTMaterial material = material(addonId, composition.material(), "composition");
        requireOwned(addonId, material, "composition");
        var components = new ArrayList<MaterialComponent>();
        for (MaterialComposition.Part part : composition.parts()) {
            GTMaterial component = material(addonId, part.material(), "composition");
            if (component == material) throw new IllegalStateException("Addon '" + addonId
                    + "' composition of '" + material.getName() + "' lists itself");
            components.add(MaterialComponent.of(component, part.amount()));
        }
        Boolean previous = flags.putIfAbsent(material, composition.crucible());
        if (previous != null && previous != composition.crucible()) {
            throw new IllegalStateException("Addon '" + addonId + "' composition of '"
                    + material.getName() + "' disagrees on the crucible flag");
        }
        if (material.hasComposition()) {
            if (!sameComposition(material, composition.divider(), components)) {
                throw new IllegalStateException("Addon '" + addonId + "' composition of '"
                        + material.getName() + "' does not match the existing composition");
            }
            return material;
        }
        material.setComposition(composition.divider(), components.toArray(MaterialComponent[]::new));
        return material;
    }

    private static boolean sameComposition(GTMaterial material, long divider, List<MaterialComponent> components) {
        if (material.getCompositionDivider() != divider) return false;
        var existing = material.getCompositionComponents();
        if (existing.size() != components.size()) return false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).material() != components.get(i).material()
                    || existing.get(i).amount() != components.get(i).amount()) return false;
        }
        return true;
    }

    private static void applySmelting(String addonId, SmeltingTargetDeclaration declaration) {
        GTMaterial material = material(addonId, declaration.material(), "smelting target");
        requireOwned(addonId, material, "smelting target");
        GTMaterial target = material(addonId, declaration.target(), "smelting target");
        boolean same = material.getTargetSmeltingMaterial().resolve() == target
                && material.getTargetSmeltingAmount() == declaration.amount();
        boolean untouched = material.getTargetSmeltingMaterial().resolve() == material
                && material.getTargetSmeltingAmount() == GTValues.U
                && !material.has(MaterialProperty.MELTING);
        if (same) {
            if (untouched) material.setSmelting(target, declaration.amount());
            return;
        }
        if (!untouched) throw new IllegalStateException("Addon '" + addonId + "' smelting target for '"
                + material.getName() + "' does not match the existing target");
        material.setSmelting(target, declaration.amount());
    }

    private static CrucibleReactions.Reaction resolveReaction(String addonId, CrucibleReactionDeclaration declaration) {
        GTMaterial output = material(addonId, declaration.output(), "crucible reaction");
        var parts = new ArrayList<CrucibleReactions.Part>();
        for (CrucibleReactionDeclaration.Part part : declaration.parts()) {
            GTMaterial input = material(addonId, part.material(), "crucible reaction");
            if (input == output) throw new IllegalStateException("Addon '" + addonId
                    + "' crucible reaction output '" + output.getName() + "' is also an input");
            parts.add(new CrucibleReactions.Part(input, part.ratio()));
        }
        return new CrucibleReactions.Reaction(output, declaration.yield(), List.copyOf(parts));
    }

    private static CrucibleReactions.AddonRemoval resolveRemoval(String addonId, CrucibleReactionRemoval removal) {
        GTMaterial output = material(addonId, removal.output(), "crucible removal");
        var inputs = new HashSet<GTMaterial>();
        for (String name : removal.inputs()) inputs.add(material(addonId, name, "crucible removal"));
        return new CrucibleReactions.AddonRemoval(addonId, output, inputs);
    }

    private static GTMaterial material(String addonId, String name, String where) {
        GTMaterial found = GTMaterialRegistry.get(name);
        if (!found.isValid()) throw new IllegalStateException("Addon '" + addonId + "' " + where
                + " uses unknown material '" + name + "'");
        return found.resolve();
    }

    private static void requireOwned(String addonId, GTMaterial material, String what) {
        if (!owners(material).contains(addonId)) throw new IllegalStateException("Addon '" + addonId
                + "' cannot set the " + what + " of '" + material.getName() + "'");
    }

    private static void addOwner(GTMaterial material, String addonId) {
        List<String> owners = OWNERS.computeIfAbsent(material.resolve(), ignored -> new ArrayList<>());
        if (!owners.contains(addonId)) owners.add(addonId);
    }

    private static String difference(MaterialDefinition left, MaterialDefinition right) {
        if (!left.displayName().equals(right.displayName())) return "displayName";
        if (left.color() != right.color()) return "color";
        if (left.texture() != right.texture()) return "texture";
        if (left.meltingPointKelvin() != right.meltingPointKelvin()) return "meltingPoint";
        if (left.boilingPointKelvin() != right.boilingPointKelvin()) return "boilingPoint";
        if (Float.floatToIntBits(left.density()) != Float.floatToIntBits(right.density())) return "density";
        if (!left.chemicalFormula().equals(right.chemicalFormula())) return "formula";
        if (!left.properties().equals(right.properties())) return "properties";
        if (!left.atomicProperties().equals(right.atomicProperties())) return "atomicProperties";
        if (left.alpha() != right.alpha()) return "alpha";
        return null;
    }

    private static String coreNameCollision(String addonId, MaterialDefinition definition) {
        GTMaterial existing = GTMaterialRegistry.get(definition.name());
        return "Addon '" + addonId + "' material '" + definition.name()
                + "' uses the same name as core material '" + existing.getName() + "' (id " + existing.getId() + ")";
    }

    private static synchronized List<Submission> snapshot() {
        return List.copyOf(QUEUE.values());
    }
}
