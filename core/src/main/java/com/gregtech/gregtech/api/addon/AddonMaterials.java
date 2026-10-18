package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
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
    @Deprecated(since = "0.5", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MIN", removeAt = "0.9")
    public static final int ADDON_ID_MIN = 4291;

    /**
     * @deprecated Addon materials no longer pick a numeric id. Use {@code MaterialDeclaration.builder(String)}. Removal is 0.9.
     */
    @Deprecated(since = "0.5", forRemoval = true)
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

    private AddonMaterials() {}

    public record AcceptedForm(String addonId, String materialName, String prefixName) {}

    private record Submission(String addonId, List<MaterialDefinition> materials,
                              List<MaterialFormRequest> forms, Runnable link) {}

    private record Pending(String addonId, MaterialDefinition definition) {}

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
                enqueue(contributor.addonId(), definitions(declarations), contributor.forms(), contributor::link);
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
        List<MaterialDefinition> copied = List.copyOf(Objects.requireNonNull(materials, "materials"));
        for (MaterialDefinition definition : copied) {
            GTMaterialRegistry.warn("Addon '" + addonId + "' material '" + definition.name()
                    + "' declared numeric id " + definition.id() + "; the id is ignored");
        }
        enqueue(addonId, copied, forms, link);
    }

    private static synchronized void enqueue(String addonId, List<MaterialDefinition> materials,
                                             List<MaterialFormRequest> forms, Runnable link) {
        if (closed) throw new IllegalStateException("Addon material queue is closed: " + addonId);
        if (addonId == null || !addonId.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("Invalid addon mod ID: " + addonId);
        if (addonId.equals("gregtech") || addonId.equals("gregtech6") || addonId.equals("minecraft"))
            throw new IllegalArgumentException("Addon mod ID is reserved: " + addonId);
        if (QUEUE.containsKey(addonId))
            throw new IllegalStateException("Addon '" + addonId + "' already submitted materials");
        List<MaterialDefinition> copiedMaterials = List.copyOf(Objects.requireNonNull(materials, "materials"));
        List<MaterialFormRequest> copiedForms = List.copyOf(Objects.requireNonNull(forms, "forms"));
        QUEUE.put(addonId, new Submission(addonId, copiedMaterials, copiedForms, link == null ? () -> {} : link));
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
                Pending seen = exactByLower.putIfAbsent(lower, new Pending(submission.addonId, definition));
                if (seen != null && !seen.definition().name().equals(definition.name())) {
                    throw new IllegalStateException("Addon '" + submission.addonId + "' material '" + exact
                            + "' differs only by case from addon '" + seen.addonId() + "' material '"
                            + seen.definition().name() + "'");
                }
                grouped.computeIfAbsent(exact, ignored -> new ArrayList<>())
                        .add(new Pending(submission.addonId, definition));
            }
        }
        List<String> names = new ArrayList<>(grouped.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            List<Pending> group = grouped.get(name);
            MaterialDefinition first = group.get(0).definition();
            for (int i = 1; i < group.size(); i++) {
                String field = difference(first, group.get(i).definition());
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
        }
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
    }

    /** Prefix checks happen after linking so a form can name a material created during link. */
    public static void publishForms() {
        List<AcceptedForm> published = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Submission submission : snapshot()) {
            for (MaterialFormRequest request : submission.forms) {
                MaterialPrefix prefix = PrefixRegistry.byName(request.prefixName());
                GTMaterial material = GTMaterialRegistry.get(request.materialName());
                if (prefix == null)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + request.prefixName() + " / " + request.materialName() + "' uses an unknown prefix");
                if (!material.isValid())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + request.prefixName() + " / " + request.materialName() + "' uses an unknown material");
                if (!com.gregtech.gregtech.api.material.MaterialItemDefinitions.candidatePrefixes().contains(prefix))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' form '"
                            + prefix.getName() + " / " + material.resolve().getName()
                            + "' is not a generated form");
                if (!prefix.isValidFor(material.resolve()))
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
