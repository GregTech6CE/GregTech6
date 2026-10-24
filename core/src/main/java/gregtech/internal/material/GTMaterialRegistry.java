package gregtech.internal.material;

import gregtech.api.material.GTValues;

import gregtech.internal.addon.AddonMaterials;
import gregtech.content.material.Materials;
import gregtech.content.data.ImportedMaterialData;

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
            System.getLogger("gt6ce.materials").log(
                    warning ? System.Logger.Level.WARNING : System.Logger.Level.INFO, message);
    /**
     * Exclusive ceiling of built-in numeric ids. The registry itself is not capped:
     * addon ids start at {@link #ADDON_ID_START} and grow with the materials that are loaded.
     */
    public static final int MAX_MATERIALS = 10000;
    /** First id assigned to addon materials. Every built-in id is below this. */
    public static final int ADDON_ID_START = MAX_MATERIALS;

    private static final Map<String, GTMaterial> BY_NAME = new HashMap<>();
    private static final Map<Integer, GTMaterial> BY_ID = new HashMap<>();
    private static final Set<GTMaterial> PERIODIC_TABLE = new HashSet<>();
    private static final Set<GTMaterial> VANILLA_MATTER = new HashSet<>();
    private static int nextAddonId = ADDON_ID_START;
    private static final Set<GTMaterial> ALL_MATERIALS = new HashSet<>();

    private static boolean initialized = false;
    /** True only while built-in declare() and the queued addon install are registering definitions. */
    private static boolean definitionsOpen = false;
    /**
     * Built-in materials present when {@link #init()} finished linking.
     * Link-time groups stay in this set. Queued addon materials, including ones created by an addon link callback, do not.
     */
    private static Set<GTMaterial> coreMaterials = Set.of();
    public enum RegistrationPhase { DEFINITIONS, LINKS, READY, FAILED }
    private static RegistrationPhase phase = RegistrationPhase.DEFINITIONS;

    public static RegistrationPhase registrationPhase() { return phase; }

    /**
     * Whether this material belongs to the built-in directory.
     * The snapshot is taken after linking, so link-time groups stay core.
     * Queued addon materials are excluded from that snapshot.
     */
    public static boolean isCoreMaterial(GTMaterial material) {
        return material != null && coreMaterials.contains(material.resolve());
    }

    /** Binds loader logging without making the material model depend on a game logging API. */
    public static void setLogSink(java.util.function.BiConsumer<Boolean, String> sink) {
        logSink = java.util.Objects.requireNonNull(sink, "sink");
    }

    public static void warn(String message) { logSink.accept(true, message); }

    /** Next addon id. Built-in registration does not consume this sequence. */
    public static int allocateAddonId() {
        if (nextAddonId <= 0) throw new IllegalStateException("No material ids left");
        return nextAddonId++;
    }

    /** Strict entry point for new definitions; historical GT6 ID reuse stays confined to createMaterial. */
    public static GTMaterial registerDefinition(MaterialDefinition definition) {
        if (!definitionsOpen || phase != RegistrationPhase.DEFINITIONS)
            throw new IllegalStateException("Material definitions are closed (" + phase + "): " + definition.name());
        String key = GTMaterial.sanitize(definition.name());
        if (BY_NAME.containsKey(key) || BY_ID.containsKey(definition.id()))
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
        if (material.getId() > 0) {
            GTMaterial previous = BY_ID.putIfAbsent(material.getId(), material);
            if (previous != null) {
                throw new IllegalStateException("Material ID " + material.getId() + " already used by " + previous.getName() + "; cannot register " + material.getName());
            }
        }
    }

    /**
     * Registers one addon material at an id from {@link #allocateAddonId()}.
     * {@code definition.id()} is not used.
     */
    public static GTMaterial installAddonMaterial(MaterialDefinition definition, int id) {
        if (!definitionsOpen || phase != RegistrationPhase.DEFINITIONS)
            throw new IllegalStateException("Material definitions are closed (" + phase + "): " + definition.name());
        if (id < ADDON_ID_START)
            throw new IllegalArgumentException("Addon material id " + id + " is below " + ADDON_ID_START);
        return createMaterial(id, definition.name(), definition.displayName(), definition.color())
                .put(definition.properties().toArray(MaterialProperty[]::new))
                .setTextureSet(definition.texture())
                .setAtomicProperties(definition.atomicProperties()).setAlpha(definition.alpha())
                .setStats(definition.meltingPointKelvin(), definition.boilingPointKelvin(), definition.density())
                .setTooltipChemical(definition.chemicalFormula());
    }

    /** Registry key equal to {@code name} ignoring case, including aliases. Null when nothing matches. */
    public static String registeredNameIgnoringCase(String name) {
        String lower = GTMaterial.sanitize(name).toLowerCase(java.util.Locale.ROOT);
        for (String key : BY_NAME.keySet()) {
            if (key.toLowerCase(java.util.Locale.ROOT).equals(lower)) return key;
        }
        return null;
    }

    /**
     * Built-in periodic-table element, stamped from the old {@code id < 8000} rule.
     * Addon materials are not stamped, so a runtime id cannot make one look like an element.
     */
    public static boolean isPeriodicTableElement(GTMaterial material) {
        return material != null && PERIODIC_TABLE.contains(material.resolve());
    }

    /** Built-in vanilla-matter material, stamped from the old {@code 7900..7912} rule. */
    public static boolean isVanillaMatter(GTMaterial material) {
        return material != null && VANILLA_MATTER.contains(material.resolve());
    }

    private static void stampBuiltinIdentity() {
        for (GTMaterial material : ALL_MATERIALS) {
            GTMaterial canonical = material.resolve();
            if (AddonMaterials.owns(canonical)) continue;
            if (canonical.has(MaterialProperty.ELEMENT) && canonical.getId() > 0 && canonical.getId() < 8000
                    && !canonical.has(MaterialProperty.ANTIMATTER)) {
                PERIODIC_TABLE.add(canonical);
            }
            if (canonical.getId() >= 7900 && canonical.getId() <= 7912) {
                VANILLA_MATTER.add(canonical);
            }
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
        if (id <= 0) return MaterialSentinels.Invalid;
        GTMaterial material = BY_ID.get(id);
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

    public static synchronized void init() {
        if (phase == RegistrationPhase.FAILED)
            throw new IllegalStateException("Material initialization previously failed");
        if (initialized) return;
        initialized = true;
        try {
            AddonMaterials.collectContributors();
            AddonMaterials.closeQueue();
            definitionsOpen = true;
            try {
                gregtech.content.material.MaterialDefinitions.declare();
                AddonMaterials.installDefinitions();
            } finally {
                definitionsOpen = false;
            }
            phase = RegistrationPhase.LINKS;
            gregtech.content.material.MaterialDefinitions.link();
            stampBuiltinIdentity();
            AddonMaterials.linkAddons();
            AddonMaterials.publishOreByproducts();
            AddonMaterials.publishOreVeins();
            var resolved = new HashSet<GTMaterial>();
            for (GTMaterial material : ALL_MATERIALS) {
                GTMaterial canonical = material.resolve();
                if (AddonMaterials.owns(canonical)) continue;
                resolved.add(canonical);
            }
            coreMaterials = Set.copyOf(resolved);
            AddonMaterials.publishPrefixes();
            AddonMaterials.publishForms();
            phase = RegistrationPhase.READY;
        } catch (RuntimeException | Error failure) {
            definitionsOpen = false;
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
