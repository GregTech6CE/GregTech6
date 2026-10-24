package gregtech.core;

import gregtech.api.addon.*;
import gregtech.internal.addon.*;
import gregtech.internal.addon.*;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.mod.ModData;
import gregtech.internal.prefix.PrefixRegistry;
import gregtech.content.data.ModReferences;
import gregtech.content.data.generated.MaterialWorkability;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/** Real registry/ServiceLoader addon fixture in an isolated JVM, not a name-only material mock. */
public final class MaterialWorkabilityContracts {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        gregtech.content.data.MaterialGroups.Glowstone.getClass();
        var root = Files.createTempDirectory("gt-workability-api");
        var service = root.resolve("META-INF/services/gregtech.api.addon.AddonMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, Contributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, MaterialWorkabilityContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        GTMaterialRegistry.init();
        var al = GTMaterialRegistry.get("Aluminium");
        var foreign = GTMaterialRegistry.get("WorkabilityForeign");
        check(al.getId() == 130 && al.getName().equals("Aluminium"), "actual canonical Al identity");
        check(GTMaterialRegistry.get("Al").resolve() == al.resolve(), "real Al alias");
        check(MaterialWorkability.isFurnace(al) && MaterialWorkability.isMortarGrindable(al)
                && MaterialWorkability.hasProvenSimpleExtrusion(al), "source-inherited Al positives");
        check(AddonMaterials.owns(foreign) && foreign.getId() >= GTMaterialRegistry.ADDON_ID_START,
                "real addon ownership; requested legacy id130 must not become builtin id");
        check(!MaterialWorkability.isFurnace(foreign) && !MaterialWorkability.hasProvenSimpleExtrusion(foreign),
                "foreign material receives no imported processing flags");
        // Public addon registration cannot claim a built-in ID/name. The overlay's owns guard
        // remains defense-in-depth; this fixture does not mutate private ownership state.
        try {
            al.setRegistration(foreign);
            check(AddonMaterials.owns(al), "canonical foreign alias retains actual addon ownership");
            check(!MaterialWorkability.isFurnace(al) && !MaterialWorkability.hasProvenSimpleExtrusion(al),
                    "original alias cannot transfer positive permissions into owned foreign target");
        } finally { al.setRegistration(null); }
        check(GTMaterialRegistry.get(130) == al && MaterialWorkability.isFurnace(al), "original identity restored");
        var wax = GTMaterialRegistry.get("WaxRefractory");
        check(wax.getMeltingPoint() > 1200 && MaterialWorkability.isFurnace(wax), "factory flag survives later heat2600");
        check(MaterialWorkability.isSoft(GTMaterialRegistry.get("GoldInductive")), "canonical source-field alias correction");
        check(!MaterialWorkability.isFurnace(GTMaterialRegistry.get("Titanium")), "source-proven hard control; not arbitrary unknown-negative");
        System.out.println("Material workability contracts passed: " + assertions);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    public static final class Contributor implements AddonMaterialContributor {
        @Override public String addonId() { return "workability_contract"; }
        @Override public List<MaterialDeclaration> materials() {
            return List.of(new MaterialDeclaration(130, "WorkabilityForeign", "Workability Foreign", 0x778899,
                    933, 2792, 2.698F, "", true, false, false, false, false, List.of(), false, false));
        }
        @Override public List<MaterialFormRequest> forms() { return List.of(); }
    }
}
