package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.addon.GregTechMaterialContributor;
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
            case "id" -> idCollision();
            case "addons" -> addonCollision();
            case "range" -> outsideRange();
            case "form" -> ungeneratedForm();
            case "invalid" -> invalidForm();
            case "late" -> lateSubmitAndEmptyBand();
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
        check(added.isValid() && added.getId() == 4291, "queued material is installed at its declared id");
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
        for (int id = AddonMaterials.ADDON_ID_MIN + 1; id <= AddonMaterials.ADDON_ID_MAX; id++) {
            GTMaterial occupant = GTMaterialRegistry.get(id);
            check(!occupant.isValid(), "addon band stays free aside from the sample, found " + id + " " + occupant.getName());
        }
        System.out.println("ammonium nitrate items=" + nitrate + " iron automatic items=" + iron);
    }

    private static void nameCollision() {
        prepare();
        AddonMaterials.submit("aa_name", List.of(MaterialDefinition.builder(4291, "Iron").dust().build()), List.of(), null);
        expectThrows("same name", "core material", "Iron", "aa_name");
    }

    private static void idCollision() {
        prepare();
        AddonMaterials.submit("aa_id", List.of(MaterialDefinition.builder(260, "AddonIron").dust().build()), List.of(), null);
        expectThrows("same id", "core material", "Iron", "aa_id");
    }

    private static void addonCollision() {
        prepare();
        AddonMaterials.submit("aa_pair", List.of(MaterialDefinition.builder(4291, "SharedName").dust().build()), List.of(), null);
        AddonMaterials.submit("zz_pair", List.of(MaterialDefinition.builder(4292, "SharedName").dust().build()), List.of(), null);
        expectThrows("same name", "addon 'aa_pair'", "SharedName", "zz_pair");
    }

    private static void outsideRange() {
        prepare();
        AddonMaterials.submit("aa_range", List.of(MaterialDefinition.builder(9998, "RangeProbe").dust().build()), List.of(), null);
        expectThrows(IllegalArgumentException.class, GTMaterialRegistry::init,
                "outside the addon range 4291 7899 aa_range");
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
        for (int id = AddonMaterials.ADDON_ID_MIN; id <= AddonMaterials.ADDON_ID_MAX; id++) {
            GTMaterial occupant = GTMaterialRegistry.get(id);
            check(!occupant.isValid(), "empty startup leaves the addon band free, found " + id + " " + occupant.getName());
        }
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
}
