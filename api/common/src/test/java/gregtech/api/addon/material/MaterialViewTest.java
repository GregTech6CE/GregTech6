/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.material;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MaterialViewTest {
    @Test
    void typedMetadataRejectsTypeConflictsAndMutableValues() {
        var id = new DataKey("test_addon", "hardness");
        var hardness = new PropertyKey<>(id, Integer.class);
        var builder = MaterialMetadata.builder().property(hardness, 3).flag(new DataKey("test_addon", "tough"));
        assertThrows(IllegalArgumentException.class, () -> builder.property(new PropertyKey<>(id, String.class), "3"));
        assertThrows(IllegalArgumentException.class, () -> MaterialMetadata.builder()
                .property(new PropertyKey<>(new DataKey("test_addon", "mutable"), ArrayList.class), new ArrayList<>()));
        var snapshot = builder.build();
        builder.flag(new DataKey("test_addon", "later"));
        assertEquals(1, snapshot.flags().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.flags().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.keys().clear());
        assertThrows(IllegalArgumentException.class, () -> snapshot.property(new PropertyKey<>(id, String.class)));
        assertEquals(3, snapshot.property(hardness).orElseThrow());
        assertTrue(snapshot.property(new PropertyKey<>(new DataKey("test_addon", "unknown"), Long.class)).isEmpty());
    }
    @Test
    void keysValidateWithoutSilentlyChangingIdentity() {
        assertEquals("Iron", new MaterialKey("Iron").name());
        assertThrows(IllegalArgumentException.class, () -> new MaterialKey("Iron Alloy"));
        assertThrows(IllegalArgumentException.class, () -> new FormKey(""));
        assertThrows(IllegalArgumentException.class, () -> new DataKey("UpperCase", "value"));
        assertThrows(IllegalArgumentException.class, () -> new MaterialInfo(-1, "group", "group", 0, "gt6ce"));
    }
    @Test
    void oldCatalogImplementersExplicitlyRejectUnsupportedViewService() {
        GregTechMaterials.Catalog legacy = new GregTechMaterials.Catalog() {
            public GregTechMaterials.Material find(String name) { return null; }
            public GregTechMaterials.Material find(int id) { return null; }
            public GregTechMaterials.Material invalid() { return null; }
            public Iterable<GregTechMaterials.Material> all() { return java.util.List.of(); }
        };
        assertThrows(UnsupportedOperationException.class, legacy::catalog);
    }

    @Test
    void tagAndGroupDeclarationsCopyMutableInputs() {
        var leaf = new DataKey("test_addon", "leaf");
        var parent = new DataKey("test_addon", "parent");
        var includes = new java.util.HashSet<DataKey>();
        includes.add(leaf);
        var tag = new MaterialTagDeclaration(parent, includes);
        includes.clear();
        assertEquals(Set.of(leaf), tag.includes());
        assertThrows(UnsupportedOperationException.class, () -> tag.includes().clear());

        var groupKey = new MaterialGroupKey(new DataKey("test_addon", "family"));
        var materials = new java.util.HashSet<MaterialKey>();
        materials.add(new MaterialKey("Iron"));
        var group = new MaterialGroupDeclaration(groupKey, "Family", materials, Set.of());
        materials.clear();
        assertEquals(Set.of(new MaterialKey("Iron")), group.materials());
        assertThrows(UnsupportedOperationException.class, () -> group.materials().clear());
    }
}
