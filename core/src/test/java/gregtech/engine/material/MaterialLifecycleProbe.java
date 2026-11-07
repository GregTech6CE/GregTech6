package gregtech.engine.material;

import gregtech.api.addon.material.*;
import gregtech.api.material.*;
import gregtech.api.mod.ModData;
import gregtech.content.material.MaterialDefinitions;
import gregtech.content.material.Materials;
import gregtech.engine.addon.AddonCoreServices;
import gregtech.engine.addon.AddonMaterials;
import java.util.List;

/** Independent lifecycle fixture; intentionally has no dependency on the JUnit runtime. */
public final class MaterialLifecycleProbe {
    private MaterialLifecycleProbe() {}

    private static void check(boolean value) {
        if (!value) throw new AssertionError("Lifecycle assertion failed");
    }

    private static RuntimeException rejects(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            return failure;
        }
        throw new AssertionError("Expected operation to fail");
    }

    public static void main(String[] args) throws Exception {
        ModData.bindPresence(id -> false);
        AddonCoreServices.bind();
        String scenario = args[0];
        check(!MaterialDefinitions.all().isEmpty());
        check(GTMaterialRegistry.allMaterials().size() == 2);
        rejects(MaterialViews::catalog);
        if (scenario.equals("early")) {
            rejects(() -> GTMaterialRegistry.requireInstalled("Iron"));
            rejects(() -> GTMaterialRegistry.createMaterial(260, "Iron", "Iron", 0));
            check(GTMaterialRegistry.allMaterials().size() == 2);
            GTMaterialRegistry.init();
            check(GTMaterialRegistry.get("Iron").getId() == 260);
            return;
        }
        if (scenario.equals("facade")) {
            try {
                Class.forName(Materials.class.getName());
                throw new AssertionError("Facade must require bootstrap");
            } catch (ExceptionInInitializerError expected) {
                check(expected.getCause() instanceof IllegalStateException);
            }
            check(GTMaterialRegistry.allMaterials().size() == 2);
            check(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.DEFINITIONS);
            return;
        }
        if (scenario.startsWith("metadata")
                || scenario.startsWith("relation")
                || scenario.startsWith("process")
                || scenario.startsWith("info")) {
            System.setProperty("material.metadata.scenario", scenario);
            var spi = java.nio.file.Files.createTempDirectory("gt6ce-material-metadata-");
            var service = spi.resolve("META-INF/services/" + AddonMaterialContributor.class.getName());
            java.nio.file.Files.createDirectories(service.getParent());
            java.nio.file.Files.writeString(service, MaterialMetadataProbeContributor.class.getName());
            try (var loader = new java.net.URLClassLoader(
                    new java.net.URL[] {spi.toUri().toURL()}, MaterialLifecycleProbe.class.getClassLoader())) {
                try {
                    AddonMaterials.collectContributors(loader);
                } catch (IllegalStateException failure) {
                    if (!scenario.equals("infoNamespaceFailure")) throw failure;
                    check(failure.getCause() instanceof IllegalArgumentException);
                    check(failure.getCause().getMessage().contains("cannot declare tag"));
                    return;
                }
            }
        }
        var shared = MaterialDefinition.builder(1, "SharedMaterial")
                .density(2)
                .dust()
                .build();
        if (scenario.equals("merge") || scenario.equals("conflict")) {
            AddonMaterials.submit("alpha_addon", List.of(shared), List.of(), () -> {});
            AddonMaterials.submit(
                    "beta_addon",
                    List.of(
                            scenario.equals("merge")
                                    ? shared
                                    : MaterialDefinition.builder(1, "SharedMaterial")
                                            .density(3)
                                            .dust()
                                            .build()),
                    List.of(),
                    () -> {});
        }
        if (scenario.equals("definitionFailure"))
            AddonMaterials.submit(
                    "bad_addon", List.of(MaterialDefinition.builder(1, "Iron").build()), List.of(), () -> {});
        if (scenario.equals("linkFailure"))
            AddonMaterials.submit(
                    "bad_addon",
                    List.of(),
                    List.of(),
                    () -> GTMaterialRegistry.registerAlias("Typo", "NoSuchMaterial"));
        if (scenario.equals("normal"))
            AddonMaterials.submit("legal_link", List.of(), List.of(), () -> {
                check(Materials.Iron == GTMaterialRegistry.get(260));
                check(Materials.Coal.getFurnaceBurnTime() > 0);
                check(Materials.Iron.has(MaterialProperty.NEVER_FURNACE));
                GTMaterialRegistry.registerAlias("LinkedIron", "Iron");
                rejects(MaterialViews::catalog);
                rejects(() -> GTMaterialRegistry.createMaterial(9000, "Late", "Late", 0));
            });
        if (scenario.endsWith("Failure") || scenario.equals("conflict")) {
            var failure = rejects(GTMaterialRegistry::init);
            check(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.FAILED);
            if (scenario.equals("conflict"))
                check(failure.getMessage().contains("density")
                        && failure.getMessage().contains("alpha_addon")
                        && failure.getMessage().contains("beta_addon"));
            if (scenario.equals("infoLegacyToolConflictFailure"))
                check(failure.getMessage().contains("tool profile")
                        && failure.getMessage().contains("MetadataMaterial")
                        && failure.getMessage().contains("metadata_test"));
            rejects(GTMaterialRegistry::init);
            rejects(() -> GTMaterialRegistry.get("Iron"));
            rejects(GTMaterialRegistry::allMaterials);
            rejects(MaterialViews::catalog);
            return;
        }
        GTMaterialRegistry.init();
        var before = List.copyOf(GTMaterialRegistry.allMaterials());
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        check(before.equals(List.copyOf(GTMaterialRegistry.allMaterials())));
        check(Materials.Trinium == GTMaterialRegistry.get(1260));
        check(GTMaterialRegistry.get("Ke") == Materials.Trinium);
        check(GTMaterialRegistry.get("Tn") == GTMaterialRegistry.get("TritaniumElemental"));
        check(GTMaterialRegistry.get("Gold") == GTMaterialRegistry.get(790));
        check(GTMaterialRegistry.get("Black") == GTMaterialRegistry.get(8250));
        check(GTMaterialRegistry.get("AntiElectron") == GTMaterialRegistry.get("Positron"));
        check(GTMaterialRegistry.get("Iron") != GTMaterialRegistry.get("AntiIron"));
        check(MaterialViews.catalog().find(new MaterialKey("AnyIron")).isEmpty());
        check(MaterialViews.catalog()
                        .find(new MaterialKey("Iron"))
                        .orElseThrow()
                        .property(MaterialProperties.MELTING_POINT)
                        .orElseThrow()
                == Materials.Iron.getMeltingPoint());
        check(MaterialViews.catalog().find(new MaterialKey("UnknownMaterial")).isEmpty());
        rejects(() -> MaterialViews.catalog().materials().clear());
        rejects(() -> MaterialViews.catalog().forms().forms().clear());
        if (scenario.equals("normal") || scenario.equals("infoLegacy")) checkEffectiveQueries();
        if (scenario.equals("merge")) {
            var material = GTMaterialRegistry.get("SharedMaterial");
            check(material.getId() >= GTMaterialRegistry.ADDON_ID_START);
            check(AddonMaterials.owners(material).equals(List.of("alpha_addon", "beta_addon")));
            check(!GTMaterialRegistry.isCoreMaterial(material));
        }
        check(MaterialViews.catalog().resolve("Fe").orElseThrow().equals(new MaterialKey("Iron")));
        var woodFamily = new DataKey("gt6ce", "wood_family");
        check(MaterialViews.catalog().tags().members(woodFamily).size() == 32);
        check(!MaterialViews.catalog().tags().contains(new MaterialKey("PetrifiedWood"), woodFamily));
        var group = MaterialViews.catalog()
                .tags()
                .group(new MaterialGroupKey(new DataKey("gt6ce", "anyiron")))
                .orElseThrow();
        check(group.members().contains(new MaterialKey("Iron")));
        check(!MaterialViews.catalog().resolve("AnyIron").isPresent());
        check(MaterialViews.catalog().forms().allows(new MaterialKey("Graphite"), new FormKey("dust")));
        var graphiteSmelting =
                MaterialViews.catalog().processes().find(new MaterialKey("Graphite"), MaterialProcessKeys.SMELTING);
        check(graphiteSmelting.size() == 1);
        check(graphiteSmelting.get(0).value().equals(new MaterialTarget(new MaterialKey("Carbon"), 648648000L)));
        rejects(() -> graphiteSmelting.clear());
        if (scenario.equals("metadata")) {
            check(MaterialViews.catalog()
                            .find(new MaterialKey("MetadataMaterial"))
                            .orElseThrow()
                            .property(new PropertyKey<>(new DataKey("metadata_test", "hardness"), Integer.class))
                            .orElseThrow()
                    == 3);
            check(MaterialViews.catalog()
                    .tags()
                    .contains(new MaterialKey("Iron"), new DataKey("metadata_test", "descriptive")));
            check(GTMaterialRegistry.get("MetadataMaterial")
                    .getProperties()
                    .equals(java.util.Set.of(
                            MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)));
        }
        if (scenario.equals("relations")) {
            var iron = new MaterialKey("Iron");
            var leaf = new DataKey("metadata_test", "leaf");
            var parent = new DataKey("metadata_test", "parent");
            check(MaterialViews.catalog().tags().contains(iron, leaf));
            check(MaterialViews.catalog().tags().contains(iron, parent));
            check(MaterialViews.catalog().tags().members(parent).contains(iron));
            var family = new MaterialGroupKey(new DataKey("metadata_test", "family"));
            check(MaterialViews.catalog()
                    .tags()
                    .group(family)
                    .orElseThrow()
                    .members()
                    .contains(iron));
            check(MaterialViews.catalog()
                    .tags()
                    .group(family)
                    .orElseThrow()
                    .members()
                    .contains(new MaterialKey("Steel")));
        }
        if (scenario.equals("processes")) {
            var key = new MaterialProcessKey<>(new DataKey("metadata_test", "throw_into_water"), String.class);
            var rules = MaterialViews.catalog().processes().find(new MaterialKey("Iron"), new FormKey("dust"), key);
            check(rules.size() == 1 && rules.get(0).value().equals("wet"));
            check(MaterialViews.catalog()
                    .processes()
                    .find(new MaterialKey("Iron"), new FormKey("ingot"), key)
                    .isEmpty());
        }
        if (scenario.equals("info")) {
            var material = new MaterialKey("MetadataMaterial");
            check(GTMaterialRegistry.get(material.name()).tool().equals(new MaterialToolProfile(1, 3F, 64, 1)));
            check(MaterialInfoTool.tool(material).equals(new MaterialToolProfile(1, 3F, 64, 1)));
            check(MaterialInfoTool.composition(material).orElseThrow().parts().size() == 1);
            check(SmeltingMaterialHelper.target(new MaterialKey("Graphite"))
                    .orElseThrow()
                    .equals(new MaterialTarget(new MaterialKey("Carbon"), 648648000L)));
            check(MaterialViews.catalog().tags().contains(material, new DataKey("metadata_test", "typed_tag")));
            var process = new MaterialProcessKey<>(new DataKey("metadata_test", "typed_process"), String.class);
            check(MaterialViews.catalog()
                    .processes()
                    .find(material, process)
                    .get(0)
                    .value()
                    .equals("typed"));
            rejects(() ->
                    MaterialInfoRegistry.addonRegistrar("metadata_test").registerForm(material, new FormKey("dust")));
        }
        if (scenario.equals("infoLegacy")) {
            var material = new MaterialKey("MetadataMaterial");
            var composition = MaterialInfoTool.composition(material).orElseThrow();
            check(composition.divider() == 2 && composition.parts().size() == 2 && composition.crucible());
            check(composition.parts().get(0).equals(new MaterialComposition.Part("Iron", GTValues.U)));
            check(composition.parts().get(1).equals(new MaterialComposition.Part("Copper", GTValues.U)));
            check(MaterialInfoTool.tool(material).equals(new MaterialToolProfile(3, 7F, 600L, 4)));
            check(GregTechMaterials.find(material.name()).toolQuality() == 4);
            check(SmeltingMaterialHelper.target(material)
                    .orElseThrow()
                    .equals(new MaterialTarget(new MaterialKey("Copper"), GTValues.U2)));
            var second = new MaterialKey("MetadataSecond");
            check(!MaterialInfoTool.composition(second).orElseThrow().crucible());
            check(SmeltingMaterialHelper.target(second).isEmpty());
            check(PulverizationMaterialHelper.target(second).isEmpty());
            check(CrushingMaterialHelper.target(second).isEmpty());
            check(MaterialViews.catalog()
                    .processes()
                    .find(second, MaterialProcessKeys.SMELTING)
                    .isEmpty());
        }
        rejects(() -> GTMaterialRegistry.registerDefinition(shared.material()));
        rejects(() -> GTMaterialRegistry.installAddonMaterial(shared, 10001));
        rejects(() -> GTMaterialRegistry.registerAlias("Fe", "Iron"));
    }

    private static void checkEffectiveQueries() {
        for (var view : MaterialViews.catalog().materials()) {
            var key = view.key();
            var material = GTMaterialRegistry.get(key.name());
            check(SmeltingMaterialHelper.target(material).equals(SmeltingMaterialHelper.target(key)));
            check(CrushingMaterialHelper.target(material).equals(CrushingMaterialHelper.target(key)));
            check(PulverizationMaterialHelper.target(material).equals(PulverizationMaterialHelper.target(key)));
            check(FuelMaterialHelper.rule(material).equals(FuelMaterialHelper.rule(key)));
            check(OreProcessingMaterialHelper.rule(material).equals(OreProcessingMaterialHelper.rule(key)));
            check(material.tool()
                    .equals(new MaterialToolProfile(
                            material.getToolTypes(),
                            material.getToolSpeed(),
                            material.getToolDurability(),
                            material.getToolQuality())));
            var indexed = MaterialInfoTool.composition(key);
            check(indexed.isPresent() == material.hasComposition());
            if (indexed.isPresent()) {
                check(indexed.get().divider() == material.getCompositionDivider());
                check(indexed.get()
                        .parts()
                        .equals(material.getCompositionComponents().stream()
                                .map(part -> new MaterialComposition.Part(
                                        part.material().getName(), part.amount()))
                                .toList()));
            }
        }
        var iron = MaterialViews.catalog().resolve("Fe").orElseThrow();
        check(SmeltingMaterialHelper.target(iron).orElseThrow().equals(new MaterialTarget(iron, GTValues.U)));
        check(SmeltingMaterialHelper.isMaterialSmeltable(iron));
        check(PulverizationMaterialHelper.target(iron).orElseThrow().equals(new MaterialTarget(iron, GTValues.U)));
    }
}
