package gregtech.engine.material;

import gregtech.api.material.*;
import gregtech.api.mod.ModData;
import gregtech.content.data.ModReferences;
import gregtech.content.material.MaterialDefinitions;
import gregtech.engine.addon.AddonMaterials;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/** Validated identity installation, keyed linking and one-way publication of the material directory. */
public final class GTMaterialRegistry {
    private GTMaterialRegistry() {}

    private static BiConsumer<Boolean, String> logSink = (warning, message) -> System.getLogger("gt6ce.materials")
            .log(warning ? System.Logger.Level.WARNING : System.Logger.Level.INFO, message);
    public static final int MAX_MATERIALS = 10000;
    public static final int ADDON_ID_START = MAX_MATERIALS;

    private static final MaterialRegistryState STATE = new MaterialRegistryState();
    private static final Map<String, GTMaterial> BY_NAME = STATE.names;
    private static final Map<Integer, GTMaterial> BY_ID = STATE.ids;
    private static final Set<GTMaterial> PERIODIC_TABLE = new LinkedHashSet<>();
    private static int nextAddonId = ADDON_ID_START;
    private static boolean initializing;
    private static boolean definitionsOpen;
    private static boolean catalogInstalled;
    private static Set<GTMaterial> coreMaterials = Set.of();

    public enum RegistrationPhase {
        DEFINITIONS,
        LINKS,
        FINALIZING,
        READY,
        FAILED
    }

    private static volatile RegistrationPhase phase = RegistrationPhase.DEFINITIONS;

    static {
        STATE.install(MaterialSentinels.INVALID, MaterialRegistryState.Mode.SENTINEL, List.of());
        STATE.install(MaterialSentinels.EMPTY, MaterialRegistryState.Mode.SENTINEL, List.of());
    }

    public static RegistrationPhase registrationPhase() {
        return phase;
    }

    public static boolean isCoreMaterial(GTMaterial material) {
        return material != null && coreMaterials.contains(material.resolve());
    }

    public static void setLogSink(BiConsumer<Boolean, String> sink) {
        logSink = Objects.requireNonNull(sink, "sink");
    }

    public static void warn(String message) {
        logSink.accept(true, message);
    }

    public static int allocateAddonId() {
        requireDefinitions("addon ID");
        while (BY_ID.containsKey(nextAddonId) && nextAddonId > 0) nextAddonId++;
        if (nextAddonId <= 0) throw new IllegalStateException("No material ids left");
        return nextAddonId++;
    }

    private static void requireDefinitions(String name) {
        if (!definitionsOpen || phase != RegistrationPhase.DEFINITIONS)
            throw new IllegalStateException("Material definitions are closed (" + phase + "): " + name);
    }

    /** Internal compatibility entry; never opens registration or replaces an existing identity. */
    public static GTMaterial registerDefinition(MaterialDefinition definition) {
        requireDefinitions(definition.name());
        return install(
                new MaterialDefinition.Declaration(definition, MaterialInfoDefinition.empty()),
                definition.id(),
                MaterialRegistryState.Mode.BUILTIN);
    }

    /** @deprecated Declare built-ins in the canonical directory; addons use the public declaration SPI. */
    @Deprecated
    public static GTMaterial createMaterial(int id, String name, String localName, int color) {
        return registerDefinition(MaterialDefinition.builder(id, GTMaterial.sanitize(name))
                .displayName(localName)
                .color(color)
                .build()
                .material());
    }

    /** Host-assigned addon identity; shared declarations are merged by AddonMaterials before this call. */
    public static GTMaterial installAddonMaterial(MaterialDefinition.Declaration declaration, int id) {
        MaterialDefinition definition = declaration.material();
        requireDefinitions(definition.name());
        if (declaration.info().relations().group())
            throw new IllegalArgumentException("Addon definitions cannot install legacy groups");
        return install(declaration, id, MaterialRegistryState.Mode.ADDON);
    }

    private static GTMaterial install(
            MaterialDefinition.Declaration declaration, int id, MaterialRegistryState.Mode mode) {
        MaterialDefinition definition = declaration.material();
        // Construct and validate the complete scalar object before any registry index is changed.
        GTMaterial material = new GTMaterial(
                        id,
                        definition.name(),
                        definition.localName().isEmpty() ? definition.displayName() : definition.localName(),
                        definition.color())
                .setDisplayName(definition.displayName())
                .put(definition.properties().toArray(MaterialProperty[]::new))
                .setTextureSet(definition.texture())
                .setAtomicProperties(definition.atomicProperties())
                .setAlpha(definition.alpha())
                .setStats(definition.meltingPointKelvin(), definition.boilingPointKelvin(), definition.density())
                .setTooltipChemical(definition.chemicalFormula());
        var tooling = declaration.info().tooling();
        if (!tooling.profile().equals(new MaterialToolProfile(0, 1F, 0, 0)))
            material.setToolStats(
                    tooling.profile().types(),
                    tooling.profile().speed(),
                    tooling.profile().durability(),
                    tooling.profile().quality());
        if (!definition.sourceId().isEmpty())
            material.setSourceMod(ModData.of(definition.sourceId(), definition.sourceName()));
        MaterialInfoRegistry.validateInstall(declaration);
        GTMaterial installed = STATE.install(material, mode, definition.aliases());
        MaterialInfoRegistry.install(declaration);
        return installed;
    }

    public static String registeredNameIgnoringCase(String name) {
        String key = GTMaterial.sanitize(name);
        for (String registered : BY_NAME.keySet()) if (registered.equalsIgnoreCase(key)) return registered;
        return null;
    }

    public static boolean isPeriodicTableElement(GTMaterial material) {
        return material != null && PERIODIC_TABLE.contains(material.resolve());
    }

    private static void stampBuiltinIdentity() {
        for (GTMaterial material : coreMaterials) {
            if (material.has(MaterialProperty.ELEMENT)
                    && material.getId() > 0
                    && material.getId() < 8000
                    && !material.has(MaterialProperty.ANTIMATTER)) PERIODIC_TABLE.add(material);
        }
    }

    public static GTMaterial get(String name) {
        rejectFailed();
        GTMaterial material = BY_NAME.get(GTMaterial.sanitize(name));
        return material == null ? MaterialSentinels.INVALID : material.resolve();
    }

    public static GTMaterial get(int id) {
        rejectFailed();
        if (id <= 0) return MaterialSentinels.INVALID;
        GTMaterial material = BY_ID.get(id);
        return material == null ? MaterialSentinels.INVALID : material.resolve();
    }

    /** Symbol holders require explicit bootstrap; reading one cannot start material installation. */
    public static void requireCatalogInstalled() {
        rejectFailed();
        if (!catalogInstalled)
            throw new IllegalStateException("Material catalog has not been installed; run material bootstrap first");
    }

    public static GTMaterial requireInstalled(String name) {
        requireCatalogInstalled();
        GTMaterial material = BY_NAME.get(GTMaterial.sanitize(name));
        if (material == null) throw new IllegalArgumentException("Unknown canonical material: " + name);
        return material.resolve();
    }

    private static void rejectFailed() {
        if (phase == RegistrationPhase.FAILED)
            throw new IllegalStateException("Material initialization previously failed");
    }

    /** Link callbacks may add checked aliases; canonical names can never be overwritten. */
    public static void registerAlias(String alias, String registryName) {
        if (!definitionsOpen && phase != RegistrationPhase.LINKS)
            throw new IllegalStateException("Material aliases are closed (" + phase + "): " + alias);
        STATE.alias(alias, registryName);
    }

    public static Map<String, GTMaterial> registeredNames() {
        rejectFailed();
        return Map.copyOf(BY_NAME);
    }

    public static Collection<GTMaterial> allMaterials() {
        rejectFailed();
        return STATE.all();
    }

    public static List<GTMaterial> sortedMaterials() {
        rejectFailed();
        return STATE.materials.stream()
                .sorted(Comparator.comparing(GTMaterial::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GTMaterial::getName))
                .toList();
    }

    public static synchronized void init() {
        rejectFailed();
        if (phase == RegistrationPhase.READY) return;
        if (initializing) throw new IllegalStateException("Reentrant material initialization (" + phase + ")");
        initializing = true;
        try {
            AddonMaterials.collectContributors();
            AddonMaterials.closeQueue();
            // Host provenance metadata must exist before per-material references use ModData.of.
            ModReferences.GT.getClass();
            definitionsOpen = true;
            try {
                for (var declaration : MaterialDefinitions.declarations())
                    install(
                            declaration,
                            declaration.id(),
                            declaration.info().relations().group()
                                    ? MaterialRegistryState.Mode.GROUP
                                    : MaterialRegistryState.Mode.BUILTIN);
                coreMaterials = Set.copyOf(STATE.materials);
                catalogInstalled = true;
                AddonMaterials.installDefinitions();
            } finally {
                definitionsOpen = false;
            }
            MaterialInfoRegistry.resolveAddonRegistrations();
            MaterialInfoRegistry.closeRegistrations();
            phase = RegistrationPhase.LINKS;
            MaterialLinker.validate(MaterialDefinitions.declarations(), BY_NAME::get);
            for (var declaration : MaterialDefinitions.declarations())
                MaterialLinker.link(declaration, BY_NAME.get(declaration.name()), BY_NAME::get);
            MaterialDefinitions.captureDefaults();
            stampBuiltinIdentity();
            AddonMaterials.linkAddons();
            AddonMaterials.publishOreByproducts();
            AddonMaterials.publishOreVeins();
            AddonMaterials.publishPrefixes();
            AddonMaterials.publishForms(MaterialInfoRegistry.formRequests());
            phase = RegistrationPhase.FINALIZING;
            MaterialRoleFlags.applyDerived();
            MaterialInfoRegistry.freeze();
            MaterialViews.publish();
            phase = RegistrationPhase.READY;
        } catch (RuntimeException | Error failure) {
            definitionsOpen = false;
            phase = RegistrationPhase.FAILED;
            throw failure;
        } finally {
            initializing = false;
        }
        logSink.accept(false, "[gregtech] Registered " + STATE.materials.size() + " materials");
    }

    /** Compatibility bootstrap hook. All static fuel, acid and processing facts now link before READY. */
    public static void postInit() {
        rejectFailed();
        if (phase != RegistrationPhase.READY)
            throw new IllegalStateException("Materials are not ready (" + phase + ")");
    }
}
