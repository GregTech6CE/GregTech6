package gregtech.engine.material;

import gregtech.api.addon.material.*;
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
        var key = new MaterialProcessKey<>(new DataKey(addonId(), "throw_into_water"), String.class);
        return switch (scenario) {
            case "processes" ->
                List.of(new MaterialProcessDeclaration<>("Iron", key, java.util.Set.of(new FormKey("dust")), "wet"));
            case "processNamespaceFailure" ->
                List.of(new MaterialProcessDeclaration<>(
                        "Iron",
                        new MaterialProcessKey<>(new DataKey("another_addon", "throw_into_water"), String.class),
                        java.util.Set.of(),
                        "wet"));
            case "processUnknownMaterialFailure" ->
                List.of(new MaterialProcessDeclaration<>("MissingMaterial", key, java.util.Set.of(), "wet"));
            default -> List.of();
        };
    }
}
