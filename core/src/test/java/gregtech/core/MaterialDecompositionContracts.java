package gregtech.core;

import gregtech.api.addon.AddonMaterialContributor;
import gregtech.api.addon.AddonMaterials;
import gregtech.api.addon.MaterialComposition;
import gregtech.api.addon.MaterialDeclaration;
import gregtech.api.addon.MaterialFormRequest;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.api.mod.ModData;
import gregtech.api.prefix.PrefixRegistry;
import gregtech.data.ModReferences;
import gregtech.data.generated.MaterialDecompositionData;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/** Original permissions and the slim opt-in API, in a separate JVM from catalog fingerprints. */
public final class MaterialDecompositionContracts {
    private static int assertions;
    private MaterialDecompositionContracts() {}

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        gregtech.data.MaterialGroups.Glowstone.getClass();
        var root = Files.createTempDirectory("gt-decomposition-api");
        var service = root.resolve("META-INF/services/gregtech.api.addon.AddonMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, Contributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, MaterialDecompositionContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        GTMaterialRegistry.init();
        policy("Alumina", true, false, false);
        policy("Salt", true, false, false);
        policy("Bronze", true, true, false); // uumAloy -> alloyCentrifuge
        policy("Lazurite", true, false, true); // elec, despite compound components
        policy("SilverIodide", true, false, true);
        policy("Porcelain", true, true, false); // supplemental built-in shares original identity
        policy("ChromiumDioxide", true, false, true); // explicit ELECTROLYSER factory argument
        policy("FlamascusSteel", true, true, false); // explicit CENTRIFUGE factory argument
        policy("Meteorite", true, false, false); // explicit DECOMPOSABLE, no route
        policy("PlainComposition", false, false, false);
        policy("PlainCrucible", false, false, false);
        policy("CentrifugeOnly", true, true, false);
        policy("ElectrolyzerOnly", true, false, true);
        policy("BothRoutes", true, true, true);
        policy("Vinteum", false, false, false); // addon name matches an absent original policy row
        for (String name : List.of("PlainComposition", "PlainCrucible", "CentrifugeOnly", "ElectrolyzerOnly", "BothRoutes")) {
            GTMaterial material = GTMaterialRegistry.get(name);
            check(material.hasComposition() && material.getCompositionDivider() == 1, "real linked composition: " + name);
            check(AddonMaterials.owns(material) && !GTMaterialRegistry.isCoreMaterial(material), "addon provenance: " + name);
        }
        check(AddonMaterials.owns(GTMaterialRegistry.get("Vinteum")) && !GTMaterialRegistry.get(8316).isValid()
                        && GTMaterialRegistry.get("Vinteum").getId() >= GTMaterialRegistry.ADDON_ID_START,
                "old numeric ID is ignored, and must not acquire the absent original material's permissions");
        var legacy = Contributor.legacyDeclaration();
        check(!legacy.centrifuge() && !legacy.electrolyzer(), "old constructor preserves no-permission defaults");
        check(MaterialDeclaration.class.getConstructor(int.class, String.class, String.class, int.class,
                int.class, int.class, float.class, String.class, boolean.class, boolean.class, boolean.class,
                boolean.class, boolean.class, List.class, boolean.class, boolean.class) != null,
                "old sixteen-argument binary constructor descriptor remains available");
        check(MaterialDeclaration.builder("Same").dust().build().equals(MaterialDeclaration.builder("Same").dust().build()),
                "default declarations retain value equality");
        check(!MaterialDeclaration.builder("Same").dust().build().equals(MaterialDeclaration.builder("Same").dust().centrifuge().build()),
                "explicit processing policy participates in declaration equality");
        // Deliberate alias identity isolation, not a supported addon registration path.
        // This JVM is separate from all catalog fingerprint contracts.
        GTMaterial alumina = GTMaterialRegistry.get("Alumina");
        GTMaterial foreign = GTMaterialRegistry.get("PlainComposition");
        try {
            alumina.setRegistration(foreign);
            MaterialDecompositionData.apply();
            policy("PlainComposition", false, false, false);
        } finally {
            alumina.setRegistration(null);
        }
        check(GTMaterialRegistry.get(8008) == alumina, "original registration restored after foreign identity probe");
        System.out.println("Material decomposition contracts passed: " + assertions);
    }

    private static void policy(String name, boolean decomposable, boolean centrifuge, boolean electrolyzer) {
        GTMaterial material = GTMaterialRegistry.get(name);
        check(material.isValid(), "registered material: " + name);
        check(material.has(MaterialProperty.DECOMPOSABLE) == decomposable
                        && material.has(MaterialProperty.CENTRIFUGE) == centrifuge
                        && material.has(MaterialProperty.ELECTROLYSER) == electrolyzer,
                "independent original processing permissions: " + name + " " + material.getProperties());
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    /** Loaded only by the temporary service fixture, never globally by the sample addon. */
    public static final class Contributor implements AddonMaterialContributor {
        @Override public String addonId() { return "decomposition_contract"; }
        static MaterialDeclaration legacyDeclaration() {
            return new MaterialDeclaration(8316, "Vinteum", "Vinteum", 0x778899, 300, 1000, 1,
                    "", true, false, false, false, false, List.of(), false, false);
        }
        @Override public List<MaterialDeclaration> materials() {
            return List.of(MaterialDeclaration.builder("PlainComposition").dust().build(),
                    MaterialDeclaration.builder("PlainCrucible").dust().build(),
                    MaterialDeclaration.builder("CentrifugeOnly").dust().centrifuge().build(),
                    MaterialDeclaration.builder("ElectrolyzerOnly").dust().electrolyzer().build(),
                    MaterialDeclaration.builder("BothRoutes").dust().centrifuge().electrolyzer().build(), legacyDeclaration());
        }
        @Override public List<MaterialFormRequest> forms() { return List.of(); }
        @Override public List<MaterialComposition> compositions() {
            // Existing built-in composition import already name-binds Vinteum to Ma.
            // Do not declare a conflicting Carbon composition merely to test policy ownership.
            return materials().stream().filter(declaration -> !declaration.name().equals("Vinteum"))
                    .map(declaration -> new MaterialComposition(declaration.name(), 1,
                    List.of(new MaterialComposition.Part("Carbon", GTValues.U)), declaration.name().equals("PlainCrucible"))).toList();
        }
    }
}
