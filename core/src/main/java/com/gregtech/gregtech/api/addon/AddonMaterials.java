package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialDefinition;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

/** Queued addon materials and extra forms. GregTech drains this once, from {@link GTMaterialRegistry#init()}. */
public final class AddonMaterials {
    /**
     * Inclusive numeric band for addon materials.
     * Core ids occupy 1–4290, the vanilla-matter block 7900–7912, and ore ids from 8000 upward.
     * The gap 4291–7899 has no built-in material. A collision inside the band still fails.
     */
    public static final int ADDON_ID_MIN = 4291;
    public static final int ADDON_ID_MAX = 7899;

    private static final TreeMap<String, Submission> QUEUE = new TreeMap<>();
    private static final Map<GTMaterial, String> OWNERS = new IdentityHashMap<>();
    private static boolean collected;
    private static boolean closed;
    private static volatile List<AcceptedForm> acceptedForms = List.of();

    private AddonMaterials() {}

    public record AcceptedForm(String addonId, String materialName, String prefixName) {}

    private record Submission(String addonId, List<MaterialDefinition> materials,
                              List<MaterialFormRequest> forms, Runnable link) {}

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
                submit(contributor.addonId(), definitions(contributor.materials()), contributor.forms(), contributor::link);
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon material contributor failed: "
                        + contributor.getClass().getName(), failure);
            }
        }
    }

    private static List<MaterialDefinition> definitions(List<MaterialDeclaration> declarations) {
        var definitions = new ArrayList<MaterialDefinition>();
        for (MaterialDeclaration declaration : declarations) {
            var builder = MaterialDefinition.builder(declaration.id(), declaration.name())
                    .displayName(declaration.displayName())
                    .color(declaration.color())
                    .chemicalFormula(declaration.formula())
                    .meltingPointKelvin(declaration.meltingPoint())
                    .boilingPointKelvin(declaration.boilingPoint())
                    .density(declaration.density());
            if (declaration.dust()) builder.dust();
            if (declaration.gas()) builder.gas();
            if (declaration.liquid()) builder.liquid();
            definitions.add(builder.build());
        }
        return List.copyOf(definitions);
    }

    /**
     * Direct queue used by tests and by {@link #collectContributors(ClassLoader)}.
     * Duplicate addon ids and calls after the queue closes fail immediately.
     */
    public static synchronized void submit(String addonId, List<MaterialDefinition> materials,
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

    public static String owner(GTMaterial material) {
        return material == null ? null : OWNERS.get(material.resolve());
    }

    public static List<AcceptedForm> acceptedForms() {
        return acceptedForms;
    }

    /** Called from material init after the built-in declarations and before linking. */
    public static void installDefinitions() {
        for (Submission submission : snapshot()) {
            for (MaterialDefinition definition : submission.materials) {
                rejectCollision(submission.addonId, definition);
                if (definition.id() < ADDON_ID_MIN || definition.id() > ADDON_ID_MAX)
                    throw new IllegalArgumentException("Addon '" + submission.addonId + "' material '"
                            + definition.name() + "' id " + definition.id() + " is outside the addon range "
                            + ADDON_ID_MIN + ".." + ADDON_ID_MAX);
                GTMaterial material = definition.register();
                material.setSourceMod(new ModData(submission.addonId, submission.addonId));
                OWNERS.put(material.resolve(), submission.addonId);
            }
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
                if (canonical.getId() < ADDON_ID_MIN || canonical.getId() > ADDON_ID_MAX)
                    throw new IllegalArgumentException("Addon '" + submission.addonId
                            + "' created material '" + canonical.getName() + "' id " + canonical.getId()
                            + " outside the addon range " + ADDON_ID_MIN + ".." + ADDON_ID_MAX);
                OWNERS.put(canonical, submission.addonId);
                if (canonical.getSourceMod() == null)
                    canonical.setSourceMod(new ModData(submission.addonId, submission.addonId));
            }
        }
    }

    /** Prefix checks happen after linking so a form can name a material created during link. */
    public static void publishForms() {
        List<AcceptedForm> published = new ArrayList<>();
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
                published.add(new AcceptedForm(submission.addonId, material.resolve().getName(), prefix.getName()));
            }
        }
        acceptedForms = List.copyOf(published);
    }

    private static void rejectCollision(String addonId, MaterialDefinition definition) {
        GTMaterial named = GTMaterialRegistry.get(definition.name());
        if (named.isValid()) {
            throw new IllegalStateException(collision(addonId, definition, named, "name"));
        }
        GTMaterial numbered = GTMaterialRegistry.get(definition.id());
        if (numbered.isValid()) {
            throw new IllegalStateException(collision(addonId, definition, numbered, "id"));
        }
    }

    private static String collision(String addonId, MaterialDefinition definition, GTMaterial existing, String kind) {
        String otherOwner = OWNERS.get(existing.resolve());
        String other = otherOwner == null
                ? "core material '" + existing.getName() + "' (id " + existing.getId() + ")"
                : "addon '" + otherOwner + "' material '" + existing.getName() + "' (id " + existing.getId() + ")";
        return "Addon '" + addonId + "' material '" + definition.name() + "' (id " + definition.id()
                + ") uses the same " + kind + " as " + other;
    }

    private static synchronized List<Submission> snapshot() {
        return List.copyOf(QUEUE.values());
    }
}
