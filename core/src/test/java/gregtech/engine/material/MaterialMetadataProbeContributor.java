package gregtech.engine.material;

import gregtech.api.addon.material.*;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialToolProfile;
import java.util.List;

/** Test-only SPI fixture for side metadata ownership, namespace and catalog-wide type checks. */
public final class MaterialMetadataProbeContributor implements AddonMaterialContributor {
    public String addonId() {
        return "metadata_test";
    }

    public List<MaterialDeclaration> materials() {
        return List.of(
                MaterialDeclaration.builder("MetadataMaterial").dust().build(),
                MaterialDeclaration.builder("MetadataSecond").dust().build());
    }

    @Override
    public List<MaterialComposition> compositions() {
        if (!"infoLegacy".equals(System.getProperty("material.metadata.scenario"))) return List.of();
        return List.of(
                new MaterialComposition(
                        "MetadataMaterial",
                        2,
                        List.of(
                                new MaterialComposition.Part("Iron", GTValues.U),
                                new MaterialComposition.Part("Copper", GTValues.U)),
                        true),
                new MaterialComposition(
                        "MetadataSecond", 1, List.of(new MaterialComposition.Part("Iron", GTValues.U)), false));
    }

    @Override
    public List<SmeltingTargetDeclaration> smeltingTargets() {
        return "infoLegacy".equals(System.getProperty("material.metadata.scenario"))
                ? List.of(new SmeltingTargetDeclaration("MetadataMaterial", "Copper", GTValues.U2))
                : List.of();
    }

    @Override
    public List<SmeltingTargetRemoval> smeltingTargetRemovals() {
        return "infoLegacy".equals(System.getProperty("material.metadata.scenario"))
                ? List.of(new SmeltingTargetRemoval("MetadataSecond"))
                : List.of();
    }

    @Override
    public void link() {
        String scenario = System.getProperty("material.metadata.scenario");
        if ("infoLegacy".equals(scenario)) {
            var material = GTMaterialRegistry.get("MetadataMaterial");
            material.setToolStats(3, 7F, 600L, 4)
                    .setCrushing(GTMaterialRegistry.get("Iron"), GTValues.U2)
                    .setPulver(GTMaterialRegistry.get("Copper"), GTValues.U9)
                    .setFurnaceBurnTime(100)
                    .setBurning(GTMaterialRegistry.get("Ashes"), GTValues.U9)
                    .setOreMultiplier(2)
                    .setOreProcessingMultiplier(3);
            material.addOreByProducts(GTMaterialRegistry.get("Copper"));
            GTMaterialRegistry.get("MetadataSecond").setPulver(null, 0).setCrushing(null, 0);
        } else if ("infoLegacyToolConflictFailure".equals(scenario)) {
            GTMaterialRegistry.get("MetadataMaterial").setToolStats(3, 7F, 600L, 4);
        }
    }

    public List<MaterialMetadataDeclaration> metadata() {
        String scenario = System.getProperty("material.metadata.scenario");
        var value = new PropertyKey<>(new DataKey(addonId(), "hardness"), Integer.class);
        return switch (scenario) {
            case "metadataUnknownFailure" ->
                List.of(new MaterialMetadataDeclaration(
                        "TypoMaterial", MaterialMetadata.builder().build()));
            case "metadataOwnerFailure" ->
                List.of(new MaterialMetadataDeclaration(
                        "Iron", MaterialMetadata.builder().property(value, 3).build()));
            case "metadataNamespaceFailure" ->
                List.of(new MaterialMetadataDeclaration(
                        "Iron",
                        MaterialMetadata.builder()
                                .tag(new DataKey("another_addon", "label"))
                                .build()));
            case "metadataTypeFailure" ->
                List.of(
                        new MaterialMetadataDeclaration(
                                "MetadataMaterial",
                                MaterialMetadata.builder().property(value, 3).build()),
                        new MaterialMetadataDeclaration(
                                "MetadataSecond",
                                MaterialMetadata.builder()
                                        .property(new PropertyKey<>(value.id(), String.class), "hard")
                                        .build()));
            case "relations", "relationTagCycleFailure", "relationUnknownTagFailure" ->
                List.of(
                        new MaterialMetadataDeclaration(
                                "MetadataMaterial",
                                MaterialMetadata.builder().property(value, 3).build()),
                        new MaterialMetadataDeclaration(
                                "Iron",
                                MaterialMetadata.builder()
                                        .tag(new DataKey(addonId(), "leaf"))
                                        .build()));
            default ->
                List.of(
                        new MaterialMetadataDeclaration(
                                "MetadataMaterial",
                                MaterialMetadata.builder().property(value, 3).build()),
                        new MaterialMetadataDeclaration(
                                "Fe",
                                MaterialMetadata.builder()
                                        .tag(new DataKey(addonId(), "descriptive"))
                                        .build()));
        };
    }

    public List<MaterialTagDeclaration> materialTags() {
        String scenario = System.getProperty("material.metadata.scenario");
        var leaf = new DataKey(addonId(), "leaf");
        var parent = new DataKey(addonId(), "parent");
        return switch (scenario) {
            case "relations" ->
                List.of(
                        new MaterialTagDeclaration(leaf, java.util.Set.of()),
                        new MaterialTagDeclaration(parent, java.util.Set.of(leaf)));
            case "relationTagCycleFailure" ->
                List.of(
                        new MaterialTagDeclaration(leaf, java.util.Set.of(parent)),
                        new MaterialTagDeclaration(parent, java.util.Set.of(leaf)));
            case "relationUnknownTagFailure" ->
                List.of(new MaterialTagDeclaration(parent, java.util.Set.of(new DataKey(addonId(), "missing"))));
            case "relationNamespaceFailure" ->
                List.of(new MaterialTagDeclaration(new DataKey("another_addon", "leaf"), java.util.Set.of()));
            default -> List.of();
        };
    }

    public List<MaterialGroupDeclaration> materialGroups() {
        String scenario = System.getProperty("material.metadata.scenario");
        var family = new MaterialGroupKey(new DataKey(addonId(), "family"));
        var first = new MaterialGroupKey(new DataKey(addonId(), "first"));
        var second = new MaterialGroupKey(new DataKey(addonId(), "second"));
        return switch (scenario) {
            case "relations" ->
                List.of(new MaterialGroupDeclaration(
                        family,
                        "Family",
                        java.util.Set.of(new MaterialKey("Steel")),
                        java.util.Set.of(new MaterialGroupKey(new DataKey("gt6ce", "anyiron")))));
            case "relationUnknownGroupFailure" ->
                List.of(new MaterialGroupDeclaration(
                        family, "Family", java.util.Set.of(new MaterialKey("MissingMaterial")), java.util.Set.of()));
            case "relationGroupCycleFailure" ->
                List.of(
                        new MaterialGroupDeclaration(first, "First", java.util.Set.of(), java.util.Set.of(second)),
                        new MaterialGroupDeclaration(second, "Second", java.util.Set.of(), java.util.Set.of(first)));
            default -> List.of();
        };
    }

    public List<MaterialProcessDeclaration<?>> processes() {
        String scenario = System.getProperty("material.metadata.scenario");
        var key = MaterialProcessKey.multiple(new DataKey(addonId(), "throw_into_water"), String.class);
        return switch (scenario) {
            case "processes" ->
                List.of(new MaterialProcessDeclaration<>("Iron", key, java.util.Set.of(new FormKey("dust")), "wet"));
            case "processFormAlias" ->
                List.of(new MaterialProcessDeclaration<>(
                        "Iron", key, java.util.Set.of(new FormKey("casingSmall")), "wet"));
            case "processSingleConflictFailure" -> {
                var single = MaterialProcessKey.single(new DataKey(addonId(), "listed_single"), String.class);
                yield List.of(
                        new MaterialProcessDeclaration<>("Iron", single, java.util.Set.of(), "wet"),
                        new MaterialProcessDeclaration<>("Iron", single, java.util.Set.of(), "dry"));
            }
            case "processMultiple" -> {
                var multiple = MaterialProcessKey.multiple(new DataKey(addonId(), "listed_multi"), String.class);
                yield List.of(
                        new MaterialProcessDeclaration<>("Iron", multiple, java.util.Set.of(), "wet"),
                        new MaterialProcessDeclaration<>("Iron", multiple, java.util.Set.of(), "dry"));
            }
            case "processUnknownFormFailure" ->
                List.of(new MaterialProcessDeclaration<>(
                        "Iron", key, java.util.Set.of(new FormKey("definitelyNotAPrefix")), "wet"));
            case "processNamespaceFailure" ->
                List.of(new MaterialProcessDeclaration<>(
                        "Iron",
                        MaterialProcessKey.multiple(new DataKey("another_addon", "throw_into_water"), String.class),
                        java.util.Set.of(),
                        "wet"));
            case "processUnknownMaterialFailure" ->
                List.of(new MaterialProcessDeclaration<>("MissingMaterial", key, java.util.Set.of(), "wet"));
            default -> List.of();
        };
    }

    @Override
    public void materialInfo(MaterialInfoTool.Registrar registrar) {
        String scenario = System.getProperty("material.metadata.scenario");
        var material = new MaterialKey("MetadataMaterial");
        switch (scenario) {
            case "infoLegacyToolConflictFailure":
                registrar.registerTool(material, new MaterialToolProfile(1, 3F, 64, 1));
                return;
            case "infoOwnerFailure":
                registrar.registerTool(new MaterialKey("Iron"), new MaterialToolProfile(1, 2F, 8, 1));
                return;
            case "infoNamespaceFailure":
                registrar.registerTag(new MaterialKey("Iron"), new DataKey("another_addon", "typed_tag"));
                return;
            case "infoProcessTypeFailure":
                var key = new DataKey(addonId(), "typed_process");
                registrar.registerProcess(
                        material, MaterialProcessKey.multiple(key, Integer.class), MaterialProcessRule.anyForm(1));
                registrar.registerProcess(
                        new MaterialKey("MetadataSecond"),
                        MaterialProcessKey.multiple(key, String.class),
                        MaterialProcessRule.anyForm("typed"));
                return;
            case "infoProcessFormAlias":
                registrar.registerProcess(
                        new MaterialKey("Iron"),
                        MaterialProcessKey.multiple(new DataKey(addonId(), "typed_process"), String.class),
                        new MaterialProcessRule<>(java.util.Set.of(new FormKey("casingSmall")), "typed"));
                return;
            case "infoProcessSingleConflictFailure": {
                var single = MaterialProcessKey.single(new DataKey(addonId(), "typed_single"), String.class);
                registrar.registerProcess(material, single, MaterialProcessRule.anyForm("wet"));
                registrar.registerProcess(material, single, MaterialProcessRule.anyForm("dry"));
                return;
            }
            case "infoProcessMultiple": {
                var multiple = MaterialProcessKey.multiple(new DataKey(addonId(), "typed_multi"), String.class);
                registrar.registerProcess(material, multiple, MaterialProcessRule.anyForm("wet"));
                registrar.registerProcess(material, multiple, MaterialProcessRule.anyForm("dry"));
                return;
            }
            case "infoProcessUnknownFormFailure":
                registrar.registerProcess(
                        new MaterialKey("Iron"),
                        MaterialProcessKey.multiple(new DataKey(addonId(), "typed_process"), String.class),
                        new MaterialProcessRule<>(java.util.Set.of(new FormKey("definitelyNotAPrefix")), "typed"));
                return;
            case "infoProcessMissingFormFailure":
                registrar.registerProcess(
                        new MaterialKey("Iron"),
                        MaterialProcessKey.multiple(new DataKey(addonId(), "typed_process"), String.class),
                        new MaterialProcessRule<>(java.util.Set.of(new FormKey("plateGem")), "typed"));
                return;
            case "info":
                break;
            default:
                return;
        }
        registrar
                .registerTool(material, new MaterialToolProfile(1, 3F, 64, 1))
                .registerComposition(
                        material,
                        new MaterialComposition(
                                material.name(), 1, List.of(new MaterialComposition.Part("Iron", GTValues.U)), false))
                .registerForm(material, new FormKey("dust"))
                .registerTag(material, new DataKey(addonId(), "typed_tag"))
                .registerProcess(
                        material,
                        MaterialProcessKey.multiple(new DataKey(addonId(), "typed_process"), String.class),
                        MaterialProcessRule.anyForm("typed"));
    }
}
