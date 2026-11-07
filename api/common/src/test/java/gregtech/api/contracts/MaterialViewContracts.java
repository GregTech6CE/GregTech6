/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.contracts;

import gregtech.api.addon.material.*;
import java.util.List;

/** Only the delivered API JAR participates in this consumer. */
final class MaterialViewContracts {
    static void check() {
        var key = new MaterialKey("Iron");
        var metadata = MaterialMetadata.builder().property(MaterialProperties.DENSITY, 7.874F)
                .tag(new DataKey("consumer", "metal_description")).build();
        MaterialView view = new MaterialSnapshot(key, new MaterialInfo(260, "material.gt6ce.iron", "Iron", 0xFFFFFF, "gt6ce"), metadata);
        if (view.property(MaterialProperties.DENSITY).orElseThrow() != 7.874F) throw new AssertionError("typed property");
        var group = new MaterialGroupView(new MaterialGroupKey(new DataKey("consumer", "iron_group")), "Iron group", java.util.Set.of(key));
        if (!group.members().contains(key)) throw new AssertionError("group view");
        AddonMaterialContributor contributor = new AddonMaterialContributor() {
            public String addonId() { return "consumer"; }
            public List<MaterialDeclaration> materials() { return List.of(); }
            public List<MaterialMetadataDeclaration> metadata() { return List.of(new MaterialMetadataDeclaration("Iron", metadata)); }
            public List<MaterialTagDeclaration> materialTags() {
                return List.of(new MaterialTagDeclaration(new DataKey("consumer", "metal"), java.util.Set.of()));
            }
            public List<MaterialGroupDeclaration> materialGroups() {
                return List.of(new MaterialGroupDeclaration(
                        new MaterialGroupKey(new DataKey("consumer", "iron_group")),
                        "Iron group",
                        java.util.Set.of(key),
                        java.util.Set.of()));
            }
        };
        if (contributor.metadata().size() != 1) throw new AssertionError("metadata SPI");
        if (contributor.materialTags().size() != 1 || contributor.materialGroups().size() != 1)
            throw new AssertionError("tag and group SPI");
        try { GregTechMaterials.catalog(); throw new AssertionError("unbound host must fail"); }
        catch (IllegalStateException expected) { }
    }
}
