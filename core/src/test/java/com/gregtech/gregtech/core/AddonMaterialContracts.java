package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMaterialContributor;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.addon.GregTechMaterialContributor;
import com.gregtech.gregtech.api.addon.MaterialDeclaration;
import com.gregtech.gregtech.api.addon.MaterialFormRequest;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialDefinition;
import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.ModReferences;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Addon material queue, in its own JVM so a submission cannot change the core fingerprint process. */
public final class AddonMaterialContracts {
    private static final String ITEM_ID_SHA256 = "c3360a6cdc2ac4aa396b7a9bb778f11663171afb4e949709c65f3903f25a5fef";
    private static int assertions;
    private static boolean linked;

    private AddonMaterialContracts() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "service" -> service();
            case "name" -> nameCollision();
            case "id" -> ignoredId();
            case "addons" -> addonMerge();
            case "range" -> ignoredHighId();
            case "form" -> ungeneratedForm();
            case "invalid" -> invalidForm();
            case "late" -> lateSubmitAndEmptyBand();
            case "mismatch" -> fieldMismatch();
            case "casefold" -> caseConflict();
            case "corecase" -> coreCaseConflict();
            case "order" -> nameOrder();
            case "named" -> namedDeclaration();
            case "scale" -> scale();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void service() throws Exception {
        prepare();
        Path root = Files.createTempDirectory("gt-addon-materials");
        Path service = root.resolve("META-INF/services/com.gregtech.gregtech.api.addon.GregTechMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, SampleContributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                AddonMaterialContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        GTMaterialRegistry.init();
        GTMaterial added = GTMaterialRegistry.get("AmmoniumNitrate");
        check(added.isValid() && added.getId() == GTMaterialRegistry.ADDON_ID_START,
                "queued material is assigned the first addon id");
        check(!GTMaterialRegistry.isCoreMaterial(added), "queued material stays out of the core snapshot");
        check(GTMaterialRegistry.isCoreMaterial(GTMaterialRegistry.get("Iron")), "Iron remains core");
        check("aa_sample".equals(AddonMaterials.owner(added)), "the addon id is recorded on the material");
        check(added.getSourceMod() != null && "aa_sample".equals(added.getSourceMod().id), "source mod is the addon");
        check(linked, "addon link runs after the built-in directory");
        check(added.getTargetPulverMaterial() == GTMaterialRegistry.get("Iron"), "link can point at a core material");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "extra forms do not change built-in automatic item ids");
        int nitrate = 0;
        int iron = 0;
        int ironDust = 0;
        Set<MaterialPrefix> automatic = Set.copyOf(MaterialItemDefinitions.candidatePrefixes());
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            if ("AmmoniumNitrate".equals(definition.material().getName())) {
                nitrate++;
                check(!definition.hasIdSuffix(), "new material keeps its base item id: " + definition.itemId());
                check(automatic.contains(definition.prefix()), "new material items stay on generated prefixes: " + definition.itemId());
            }
            if ("Iron".equals(definition.material().getName()) && automatic.contains(definition.prefix())) iron++;
            if ("dust_iron".equals(definition.itemId())) ironDust++;
        }
        check(nitrate > 0, "ammonium nitrate generates dust forms");
        check(ironDust == 1, "an already generated form is not emitted again");
        check(oldBandEmpty(), "addon ids stay above the old band");
        System.out.println("ammonium nitrate items=" + nitrate + " iron automatic items=" + iron);
    }

    private static void nameCollision() {
        prepare();
        AddonMaterials.submit("aa_name", List.of(MaterialDefinition.builder(4291, "Iron").dust().build()), List.of(), null);
        expectThrows("same name", "core material", "Iron", "aa_name");
    }

    private static void ignoredId() {
        prepare();
        var warnings = new ArrayList<String>();
        GTMaterialRegistry.setLogSink((warning, message) -> { if (warning) warnings.add(message); });
        AddonMaterials.submit("aa_id", List.of(MaterialDefinition.builder(260, "AddonIron").dust().build()), List.of(), null);
        GTMaterialRegistry.init();
        GTMaterial added = GTMaterialRegistry.get("AddonIron");
        check(added.isValid() && added.getId() == GTMaterialRegistry.ADDON_ID_START, "a built-in id is not claimed");
        check(GTMaterialRegistry.get(260).getName().equals("Iron"), "Iron keeps id 260");
        check(!GTMaterialRegistry.get(260).getName().equals("AddonIron"), "the declared number is not the addon id");
        check(warnings.stream().anyMatch(message -> message.contains("260") && message.contains("ignored")),
                "the ignored id is logged");
    }

    private static void addonMerge() {
        prepare();
        AddonMaterials.submit("aa_pair", List.of(MaterialDefinition.builder(4291, "SharedName").dust().build()),
                List.of(new MaterialFormRequest("Iron", "dust")), null);
        AddonMaterials.submit("zz_pair", List.of(MaterialDefinition.builder(4292, "SharedName").color(0xFFFFFF).dust().build()),
                List.of(new MaterialFormRequest("Iron", "dustSmall")), null);
        GTMaterialRegistry.init();
        GTMaterial shared = GTMaterialRegistry.get("SharedName");
        check(shared.isValid() && shared.getId() == GTMaterialRegistry.ADDON_ID_START, "matching declarations merge");
        check(AddonMaterials.owners(shared).equals(List.of("aa_pair", "zz_pair")), "both addons are recorded, in addon-id order");
        check(AddonMaterials.acceptedForms().stream().anyMatch(form -> form.prefixName().equals("dust"))
                        && AddonMaterials.acceptedForms().stream().anyMatch(form -> form.prefixName().equals("dustSmall")),
                "forms() is the union");
    }

    private static void ignoredHighId() {
        prepare();
        AddonMaterials.submit("aa_range", List.of(MaterialDefinition.builder(9998, "RangeProbe").dust().build()), List.of(), null);
        GTMaterialRegistry.init();
        GTMaterial added = GTMaterialRegistry.get("RangeProbe");
        check(added.isValid() && added.getId() == GTMaterialRegistry.ADDON_ID_START, "an old out-of-band id is not stored");
        check(!GTMaterialRegistry.get(9998).isValid(), "9998 stays empty");
    }

    private static void fieldMismatch() {
        prepare();
        AddonMaterials.submit("aa_pair", List.of(MaterialDefinition.builder(4291, "SharedName").color(0x111111).dust().build()), List.of(), null);
        AddonMaterials.submit("zz_pair", List.of(MaterialDefinition.builder(4292, "SharedName").color(0x222222).dust().build()), List.of(), null);
        expectThrows("field color", "aa_pair", "zz_pair", "SharedName");
    }

    private static void caseConflict() {
        prepare();
        AddonMaterials.submit("aa_case", List.of(MaterialDefinition.builder(1, "AddonCase").dust().build()), List.of(), null);
        AddonMaterials.submit("zz_case", List.of(MaterialDefinition.builder(1, "addoncase").dust().build()), List.of(), null);
        expectThrows("differs only by case", "AddonCase", "addoncase", "aa_case", "zz_case");
    }

    private static void nameOrder() {
        prepare();
        AddonMaterials.submit("aa_order", List.of(MaterialDefinition.builder(1, "OrderZeta").dust().build()), List.of(), null);
        AddonMaterials.submit("zz_order", List.of(MaterialDefinition.builder(9, "OrderAlpha").dust().build()), List.of(), null);
        GTMaterialRegistry.init();
        check(GTMaterialRegistry.get("OrderAlpha").getId() == GTMaterialRegistry.ADDON_ID_START, "names sort before addon-id order");
        check(GTMaterialRegistry.get("OrderZeta").getId() == GTMaterialRegistry.ADDON_ID_START + 1, "the later name gets the next id");
    }

    private static void coreCaseConflict() {
        prepare();
        AddonMaterials.submit("aa_iron", List.of(MaterialDefinition.builder(1, "iron").dust().build()), List.of(), null);
        expectThrows("differs only by case", "core material", "Iron", "aa_iron");
    }

    private static void namedDeclaration() throws Exception {
        prepare();
        var warnings = new ArrayList<String>();
        GTMaterialRegistry.setLogSink((warning, message) -> { if (warning) warnings.add(message); });
        Path root = Files.createTempDirectory("gt-addon-named");
        Path service = root.resolve("META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, NamedContributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                AddonMaterialContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        GTMaterialRegistry.init();
        GTMaterial named = GTMaterialRegistry.get("NamedDust");
        GTMaterial numbered = GTMaterialRegistry.get("FromNumber");
        check(named.isValid() && named.getId() == GTMaterialRegistry.ADDON_ID_START + 1, "name order places FromNumber first");
        check(numbered.isValid() && numbered.getId() == GTMaterialRegistry.ADDON_ID_START, "the deprecated builder does not keep 7777");
        check(!GTMaterialRegistry.get(7777).isValid(), "7777 is not registered");
        check(warnings.stream().anyMatch(message -> message.contains("7777") && message.contains("FromNumber")),
                "the deprecated builder logs the ignored id");
        check(warnings.stream().noneMatch(message -> message.contains("NamedDust")), "the name builder does not warn");
    }

    private static void scale() {
        prepare();
        int dusts = 1000;
        int metals = 1000;
        var materials = new ArrayList<MaterialDefinition>();
        for (int i = 0; i < dusts; i++) {
            materials.add(MaterialDefinition.builder(1, String.format("ScaleDust%04d", i)).dust().build());
        }
        var metal = AddonMaterials.metalFlags();
        for (int i = 0; i < metals; i++) {
            materials.add(MaterialDefinition.builder(1, String.format("ScaleMetal%04d", i)).properties(metal).build());
        }
        AddonMaterials.submit("aa_scale", materials, List.of(), null);
        long started = System.nanoTime();
        GTMaterialRegistry.init();
        long initMs = (System.nanoTime() - started) / 1_000_000L;
        long itemsStarted = System.nanoTime();
        var definitions = MaterialItemDefinitions.all();
        long itemsMs = (System.nanoTime() - itemsStarted) / 1_000_000L;
        int dustItems = 0;
        int metalItems = 0;
        int coreItems = 0;
        for (var definition : definitions) {
            String name = definition.material().getName();
            if (name.startsWith("ScaleDust")) dustItems++;
            else if (name.startsWith("ScaleMetal")) metalItems++;
            else coreItems++;
        }
        System.gc();
        long heapMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
        System.out.println("scale dustMaterials=" + dusts + " metalMaterials=" + metals
                + " dustItems=" + dustItems + " metalItems=" + metalItems + " coreItems=" + coreItems
                + " initMs=" + initMs + " itemListMs=" + itemsMs + " heapMb=" + heapMb);
    }

    private static void ungeneratedForm() {
        prepare();
        AddonMaterials.submit("aa_form", List.of(), List.of(new MaterialFormRequest("Diamond", "crystal")), null);
        expectThrows("not a generated form", "crystal", "Diamond", "aa_form");
    }

    private static void invalidForm() {
        prepare();
        AddonMaterials.submit("aa_invalid", List.of(), List.of(new MaterialFormRequest("Diamond", "plate")), null);
        expectThrows("not valid", "plate", "Diamond", "aa_invalid");
    }

    private static void lateSubmitAndEmptyBand() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        expectThrows(IllegalStateException.class,
                () -> AddonMaterials.submit("aa_late", List.of(), List.of(), null),
                "Addon material queue is closed");
        expectThrows(IllegalStateException.class,
                () -> GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(4291, "LateDefinition").dust().build()),
                "Material definitions are closed");
        check(oldBandEmpty(), "empty startup leaves the old addon band free");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "an empty addon queue keeps the core item id sample");
    }

    private static void prepare() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
    }

    private static void expectThrows(String... parts) {
        expectThrows(IllegalStateException.class, GTMaterialRegistry::init, String.join(" ", parts));
        // init failure message is the assertion text only when the thrown message contains every part
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(message, failure);
            if (message.equals(failure.getMessage())) return;
            String text = failure.getMessage() == null ? "" : failure.getMessage();
            for (String part : message.split(" ")) {
                if (!text.contains(part)) throw new AssertionError(message + " but was: " + text, failure);
            }
            return;
        }
        throw new AssertionError(message);
    }

    private static String coreAutomaticFingerprint() throws Exception {
        Set<MaterialPrefix> automatic = Set.copyOf(MaterialItemDefinitions.candidatePrefixes());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            if (!GTMaterialRegistry.isCoreMaterial(definition.material()) || !automatic.contains(definition.prefix())) continue;
            String line = definition.itemId() + "\t" + definition.baseItemId() + "\t"
                    + definition.prefix().getName() + "\t" + definition.material().getName() + "\t"
                    + definition.material().getId() + "\n";
            digest.update(line.getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder text = new StringBuilder();
        for (byte value : digest.digest()) text.append(String.format("%02x", value));
        return text.toString();
    }

    private static boolean oldBandEmpty() {
        for (int id = AddonMaterials.ADDON_ID_MIN; id <= AddonMaterials.ADDON_ID_MAX; id++) {
            GTMaterial occupant = GTMaterialRegistry.get(id);
            if (occupant.isValid()) return false;
        }
        return true;
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }

    /** Loaded only through a temporary service file, never from the shared test classpath. */
    public static final class SampleContributor implements GregTechMaterialContributor {
        @Override public String addonId() { return "aa_sample"; }

        @Override public List<MaterialDefinition> materials() {
            return List.of(MaterialDefinition.builder(AddonMaterials.ADDON_ID_MIN, "AmmoniumNitrate")
                    .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                    .meltingPointKelvin(443).boilingPointKelvin(483).density(1.72F).dust().build());
        }

        @Override public List<MaterialFormRequest> forms() {
            return List.of(new MaterialFormRequest("Iron", "dust"));
        }

        @Override public void link() {
            GTMaterialRegistry.get("AmmoniumNitrate").setPulver(GTMaterialRegistry.get("Iron"), 1);
            linked = true;
        }
    }

    /** Loaded only through a temporary service file, never from the shared test classpath. */
    public static final class NamedContributor implements AddonMaterialContributor {
        @Override public String addonId() { return "aa_named"; }

        @Override public List<MaterialDeclaration> materials() {
            return List.of(
                    MaterialDeclaration.builder("NamedDust").dust().build(),
                    MaterialDeclaration.builder(7777, "FromNumber").dust().build());
        }

        @Override public List<MaterialFormRequest> forms() { return List.of(); }
    }
}
