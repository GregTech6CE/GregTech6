package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.ImportedMaterialData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Global material registry, analogous to {@code OreDictMaterial.createMaterial} + {@code MT.ALL_MATERIALS_REGISTERED_HERE}.
 */
public final class GTMaterialRegistry {
    private static java.util.function.BiConsumer<Boolean, String> logSink = (warning, message) ->
            System.getLogger("gregtech.materials").log(
                    warning ? System.Logger.Level.WARNING : System.Logger.Level.INFO, message);
    public static final int MAX_MATERIALS = 10000;

    private static final Map<String, GTMaterial> BY_NAME = new HashMap<>();
    private static final GTMaterial[] BY_ID = new GTMaterial[MAX_MATERIALS];
    private static final Set<GTMaterial> ALL_MATERIALS = new HashSet<>();

    private static boolean initialized = false;
    public enum RegistrationPhase { DEFINITIONS, LINKS, READY, FAILED }
    private static RegistrationPhase phase = RegistrationPhase.DEFINITIONS;

    public static RegistrationPhase registrationPhase() { return phase; }

    /** Binds loader logging without making the material model depend on a game logging API. */
    public static void setLogSink(java.util.function.BiConsumer<Boolean, String> sink) {
        logSink = java.util.Objects.requireNonNull(sink, "sink");
    }

    /** Strict entry point for new definitions; historical GT6 ID reuse stays confined to createMaterial. */
    public static GTMaterial registerDefinition(MaterialDefinition definition) {
        if (phase != RegistrationPhase.DEFINITIONS)
            throw new IllegalStateException("Material definitions are closed (" + phase + "): " + definition.name());
        String key = GTMaterial.sanitize(definition.name());
        if (BY_NAME.containsKey(key) || BY_ID[definition.id()] != null)
            throw new IllegalArgumentException("Material name or ID already registered: " + definition.name() + " / " + definition.id());
        return createMaterial(definition.id(), definition.name(), definition.displayName(), definition.color())
                .put(definition.properties().toArray(MaterialProperty[]::new))
                .setTextureSet(definition.texture())
                .setAtomicProperties(definition.atomicProperties()).setAlpha(definition.alpha())
                .setStats(definition.meltingPointKelvin(), definition.boilingPointKelvin(), definition.density())
                .setTooltipChemical(definition.chemicalFormula());
    }

    private GTMaterialRegistry() {}

    public static GTMaterial createMaterial(int id, String name, String localName, int color) {
        String sanitized = GTMaterial.sanitize(name);
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Material name cannot be empty after sanitization: " + name);
        }

        if (id >= MAX_MATERIALS) {
            throw new IllegalArgumentException("Material ID out of range: " + id);
        }

        GTMaterial existing = BY_NAME.get(sanitized);
        if (existing != null) {
            if (id < 0 || existing.getId() == id) {
                return existing;
            }
            logSink.accept(true, "Duplicate material name '" + sanitized + "' with different IDs " + existing.getId() + " and " + id);
            GTMaterial replacement = new GTMaterial(id, sanitized, localName, color);
            existing.setRegistration(replacement);
            register(replacement);
            return replacement;
        }

        int resolvedId = id < 0 ? GTMaterial.INVALID_ID : id;
        GTMaterial material = new GTMaterial(resolvedId, sanitized, localName, color);
        register(material);
        return material;
    }

    private static void register(GTMaterial material) {
        BY_NAME.put(material.getName(), material);
        ALL_MATERIALS.add(material);
        if (material.getId() > 0 && material.getId() < MAX_MATERIALS) {
            if (BY_ID[material.getId()] != null) {
                throw new IllegalStateException("Material ID " + material.getId() + " already used by " + BY_ID[material.getId()].getName() + "; cannot register " + material.getName());
            }
            BY_ID[material.getId()] = material;
        }
    }

    public static GTMaterial get(String name) {
        GTMaterial material = BY_NAME.get(GTMaterial.sanitize(name));
        return material == null ? MaterialSentinels.Invalid : material.resolve();
    }

    /** GT6 field name (e.g. {@code Cu}) pointing at the canonical registry name (e.g. {@code Copper}). */
    public static void registerAlias(String alias, String registryName) {
        String aliasKey = GTMaterial.sanitize(alias);
        String targetKey = GTMaterial.sanitize(registryName);
        GTMaterial material = BY_NAME.get(targetKey);
        if (material == null || !material.isValid()) {
            return;
        }

        GTMaterial existing = BY_NAME.get(aliasKey);
        if (existing != null) {
            // Keep canonical registry entries (e.g. element {@code Gold}) over wood field aliases ({@code Gold}  -> {@code Goldwood}).
            if (existing.getName().equals(aliasKey) && !aliasKey.equals(targetKey)) {
                return;
            }
            if (isAntimatter(existing) && !isAntimatter(material)) {
                BY_NAME.put(aliasKey, material);
            }
            return;
        }
        BY_NAME.put(aliasKey, material);
    }

    private static boolean isAntimatter(GTMaterial material) {
        return material.has(MaterialProperty.ANTIMATTER)
                || material.getName().startsWith("Anti");
    }

    public static void setDisplayName(String registryName, String displayName) {
        GTMaterial material = BY_NAME.get(GTMaterial.sanitize(registryName));
        if (material != null && material.isValid()) {
            material.setDisplayName(displayName);
        }
    }

    public static GTMaterial get(int id) {
        if (id <= 0 || id >= MAX_MATERIALS) return MaterialSentinels.Invalid;
        GTMaterial material = BY_ID[id];
        return material == null ? MaterialSentinels.Invalid : material.resolve();
    }

    public static Collection<GTMaterial> allMaterials() {
        return Collections.unmodifiableCollection(ALL_MATERIALS);
    }

    private static List<GTMaterial> sortedMaterialsCache = null;

    /** Materials sorted A-Z 0-9 by their internal name (e.g. "Aluminium", "Copper", "Iron"). */
    public static List<GTMaterial> sortedMaterials() {
        if (sortedMaterialsCache == null || sortedMaterialsCache.size() != ALL_MATERIALS.size()) {
            sortedMaterialsCache = new ArrayList<>(ALL_MATERIALS);
            sortedMaterialsCache.sort(Comparator.comparing(GTMaterial::getName, String.CASE_INSENSITIVE_ORDER));
        }
        return Collections.unmodifiableList(sortedMaterialsCache);
    }

    public static void init() {
        if (phase == RegistrationPhase.FAILED)
            throw new IllegalStateException("Material initialization previously failed");
        if (initialized) return;
        initialized = true;
        try {
            com.gregtech.gregtech.content.material.MaterialDefinitions.declare();
            phase = RegistrationPhase.LINKS;
            com.gregtech.gregtech.content.material.MaterialDefinitions.link();
            phase = RegistrationPhase.READY;
        } catch (RuntimeException | Error failure) {
            phase = RegistrationPhase.FAILED;
            throw failure;
        }

        logSink.accept(false, "[gregtech] Registered " + ALL_MATERIALS.size() + " materials");
    }

    public static void postInit() {
        ImportedMaterialData.applyPostInitFuelStats();
        // GT6 TD.Processing.NEVER_FURNACE (MT.java: Fe 414, Ti 410, W 463, WroughtIron 1655,
        // Steel 1713): these metals cannot be produced by furnace smelting at all — the vanilla
        // furnace recipe is rewritten to yield scrap and the metal has to be melted in a crucible.
        for (String name : new String[]{"Iron", "WroughtIron", "Steel", "Titanium", "Tungsten"}) {
            GTMaterial material = get(name);
            if (!material.isValid()) {
                throw new IllegalStateException("Missing NEVER_FURNACE material: " + name);
            }
            material.put(MaterialProperty.NEVER_FURNACE);
        }
        // Explicit MT.setPulver rows are independent from ore crushing transformations.
        for(String[] pair:new String[][]{{"Graphene","C"},{"ChargedCertusQuartz","CertusQuartz"},
                {"NetherBrick","Netherrack"},{"WroughtIron","Fe"},{"AnnealedCopper","Cu"},{"PigIron","Fe"},
                {"IronCompressed","Fe"},{"CastIron","Fe"},{"IronMagnetic","Fe"},{"SteelMagnetic","Steel"},
                {"NeodymiumMagnetic","Nd"},{"WoodCompressed","Wood"}}) {
            GTMaterial input=get(pair[0]),output=get(pair[1]);
            if(!input.isValid()||!output.isValid())throw new IllegalStateException("Missing pulverization material: "+java.util.Arrays.toString(pair));
            input.setPulver(output,GTValues.U);
        }
    }
}
