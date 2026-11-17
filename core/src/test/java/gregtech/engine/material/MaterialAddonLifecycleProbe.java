package gregtech.engine.material;

import gregtech.api.addon.material.*;
import gregtech.api.mod.ModData;
import gregtech.engine.addon.AddonCoreServices;
import gregtech.engine.addon.AddonMaterials;
import java.nio.file.Files;
import java.util.List;

/** Isolated JVM fixture exercising actual addon discovery and publication. */
public final class MaterialAddonLifecycleProbe {
    private static final MaterialProcessKey<String> PROCESS =
            MaterialProcessKey.single(new DataKey("alpha_addon", "probe"), String.class);
    private static final MaterialProcessKey<SpecialProcessValue> SPECIAL =
            MaterialProcessKey.multiple(new DataKey("alpha_addon", "special"), SpecialProcessValue.class);
    private static final DataKey FLAG = new DataKey("alpha_addon", "probe_flag");
    private static final PropertyKey<String> PROPERTY =
            new PropertyKey<>(new DataKey("alpha_addon", "probe_value"), String.class);

    public static final class Alpha implements AddonMaterialContributor {
        public String addonId() {
            return "alpha_addon";
        }

        public List<MaterialDeclaration> materials() {
            return List.of(MaterialDeclaration.builder("SharedMaterial")
                    .density(2)
                    .dust()
                    .build());
        }

        public void materialInfo(MaterialInfoTool.Registrar registrar) {
            registrar.registerProcess(
                    new MaterialKey("SharedMaterial"), PROCESS, MaterialProcessRule.anyForm("published"));
            if ("foreign_host".equals(System.getProperty("probe.scenario"))) {
                registrar.registerMetadata(new MaterialMetadataDeclaration(
                        "Iron",
                        MaterialMetadata.builder()
                                .flag(FLAG)
                                .property(PROPERTY, "published")
                                .build()));
                registrar.registerForm("Iron", "dust");
                registrar.registerComposition(
                        new MaterialKey("Iron"),
                        new MaterialComposition(
                                "Iron",
                                1,
                                List.of(new MaterialComposition.Part("Copper", gregtech.api.material.GTValues.U)),
                                false));
                registrar.registerProcess(
                        new MaterialKey("Iron"),
                        MaterialProcessKeys.DECOMPOSITION,
                        MaterialProcessRule.anyForm(new MaterialDecompositionRule(
                                java.util.Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), List.of(), false)));
            }
            if (List.of("unknown", "foreign_host", "foreign_output").contains(System.getProperty("probe.scenario"))) {
                registrar.registerSpecialProcess(new SpecialProcessMethod(SPECIAL, "gt.recipe.bath", "probe.special"));
                var dust = new FormKey("dust");
                var output = new SpecialProcessValue.ItemOutput(
                        new MaterialKey(
                                "unknown".equals(System.getProperty("probe.scenario"))
                                        ? "UnknownOutput"
                                        : "foreign_host".equals(System.getProperty("probe.scenario"))
                                                ? "SharedMaterial"
                                                : "Iron"),
                        List.of(new SpecialProcessValue.FormCount(dust, 1)),
                        10000);
                var target =
                        new SpecialProcessValue.Target(java.util.Map.of(dust, 1), List.of(output), List.of(), 1, 20, 0);
                registrar.registerProcess(
                        new MaterialKey(
                                "foreign_host".equals(System.getProperty("probe.scenario"))
                                        ? "Iron"
                                        : "SharedMaterial"),
                        SPECIAL,
                        new MaterialProcessRule<>(java.util.Set.of(dust), target));
            }
        }
    }

    public static final class Beta implements AddonMaterialContributor {
        public String addonId() {
            return "beta_addon";
        }

        public List<MaterialDeclaration> materials() {
            return List.of(MaterialDeclaration.builder("SharedMaterial")
                    .density(Boolean.getBoolean("probe.conflict") ? 3 : 2)
                    .dust()
                    .build());
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("probe.scenario", args[0]);
        System.setProperty("probe.conflict", args[0]);
        ModData.bindPresence(id -> false);
        AddonCoreServices.bind();
        var directory = Files.createTempDirectory("gt6ce-addon-probe");
        var service = directory.resolve("META-INF/services/" + AddonMaterialContributor.class.getName());
        Files.createDirectories(service.getParent());
        Files.writeString(service, Alpha.class.getName() + "\n" + Beta.class.getName() + "\n");
        try (var loader = new java.net.URLClassLoader(
                new java.net.URL[] {directory.toUri().toURL()}, MaterialAddonLifecycleProbe.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        if (Boolean.parseBoolean(args[0]) || args[0].equals("unknown")) {
            try {
                GTMaterialRegistry.init();
                throw new AssertionError("invalid addon declarations were accepted");
            } catch (RuntimeException expected) {
                String wanted = args[0].equals("unknown") ? "Unknown special process output" : "density";
                if (!expected.getMessage().contains(wanted)) throw expected;
            }
            if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.FAILED)
                throw new AssertionError("failed material initialization became visible");
            try {
                MaterialViews.catalog();
                throw new AssertionError("failed catalog was exposed");
            } catch (IllegalStateException expected) {
            }
            return;
        }
        GTMaterialRegistry.init();
        var material = GTMaterialRegistry.get("SharedMaterial");
        if (!AddonMaterials.owners(material).equals(List.of("alpha_addon", "beta_addon")))
            throw new AssertionError("shared material owners were not merged");
        var rules = MaterialViews.catalog().processes().find(new MaterialKey("SharedMaterial"), PROCESS);
        if (rules.size() != 1 || !rules.get(0).value().equals("published"))
            throw new AssertionError("addon process was not published");
        if (args[0].equals("foreign_host") || args[0].equals("foreign_output")) {
            var host = new MaterialKey(args[0].equals("foreign_host") ? "Iron" : "SharedMaterial");
            if (MaterialViews.catalog().processes().find(host, SPECIAL).size() != 1)
                throw new AssertionError("special process was not published for " + host);
        }
        if (args[0].equals("foreign_host")) {
            var iron = MaterialViews.catalog().find(new MaterialKey("Iron")).orElseThrow();
            if (!iron.flags().contains(FLAG)
                    || !iron.property(PROPERTY).orElseThrow().equals("published"))
                throw new AssertionError("metadata on existing material was not published");
            if (MaterialViews.catalog()
                            .processes()
                            .find(new MaterialKey("Iron"), MaterialProcessKeys.DECOMPOSITION)
                            .size()
                    != 1) throw new AssertionError("decomposition on existing material was not published");
            if (!AddonMaterials.accepts("Iron", "dust"))
                throw new AssertionError("form on existing material was not accepted");
            if (MaterialInfoTool.composition(new MaterialKey("Iron")).isEmpty())
                throw new AssertionError("composition on existing material was not published");
        }
    }
}
