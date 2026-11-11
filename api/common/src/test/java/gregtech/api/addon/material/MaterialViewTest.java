/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.material;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialViewTest {
    @Test
    void typedMetadataRejectsTypeConflictsAndMutableValues() {
        var id = new DataKey("test_addon", "hardness");
        var hardness = new PropertyKey<>(id, Integer.class);
        var builder = MaterialMetadata.builder().property(hardness, 3).flag(new DataKey("test_addon", "tough"));
        assertThrows(IllegalArgumentException.class, () -> builder.property(new PropertyKey<>(id, String.class), "3"));
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialMetadata.builder()
                        .property(
                                new PropertyKey<>(new DataKey("test_addon", "mutable"), ArrayList.class),
                                new ArrayList<>()));
        var snapshot = builder.build();
        builder.flag(new DataKey("test_addon", "later"));
        assertEquals(1, snapshot.flags().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.flags().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.keys().clear());
        assertThrows(IllegalArgumentException.class, () -> snapshot.property(new PropertyKey<>(id, String.class)));
        assertEquals(3, snapshot.property(hardness).orElseThrow());
        assertTrue(snapshot.property(new PropertyKey<>(new DataKey("test_addon", "unknown"), Long.class))
                .isEmpty());
    }

    @Test
    void keysValidateWithoutSilentlyChangingIdentity() {
        assertEquals("Iron", new MaterialKey("Iron").name());
        assertThrows(IllegalArgumentException.class, () -> new MaterialKey("Iron Alloy"));
        assertThrows(IllegalArgumentException.class, () -> new FormKey(""));
        assertThrows(IllegalArgumentException.class, () -> new DataKey("UpperCase", "value"));
        assertThrows(IllegalArgumentException.class, () -> new MaterialInfo(-1, "group", "group", 0, "gt6ce"));
        assertTrue(MaterialProcessKeys.legacyTarget(null).isEmpty());
        assertTrue(MaterialProcessKeys.legacyTargetName(null).isEmpty());
    }

    @Test
    void oldCatalogImplementersExplicitlyRejectUnsupportedViewService() {
        GregTechMaterials.Catalog legacy = new GregTechMaterials.Catalog() {
            public GregTechMaterials.Material find(String name) {
                return null;
            }

            public GregTechMaterials.Material find(int id) {
                return null;
            }

            public GregTechMaterials.Material invalid() {
                return null;
            }

            public Iterable<GregTechMaterials.Material> all() {
                return java.util.List.of();
            }
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
        assertThrows(
                UnsupportedOperationException.class, () -> group.materials().clear());
    }

    @Test
    void processDeclarationsAreTypedAndImmutable() {
        var process = MaterialProcessKey.multiple(new DataKey("test_addon", "throw_into_water"), String.class);
        var forms = new java.util.HashSet<FormKey>();
        forms.add(new FormKey("dust"));
        var declaration = new MaterialProcessDeclaration<>("Iron", process, forms, "swim");
        forms.clear();
        assertEquals(Set.of(new FormKey("dust")), declaration.forms());
        assertThrows(
                UnsupportedOperationException.class, () -> declaration.forms().clear());
        @SuppressWarnings("rawtypes")
        MaterialProcessKey integerProcess =
                MaterialProcessKey.multiple(new DataKey("test_addon", "throw_into_water"), Integer.class);
        @SuppressWarnings("unchecked")
        var invalid = assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialProcessDeclaration("Iron", integerProcess, Set.of(), "wrong"));
        assertTrue(invalid.getMessage().contains("type mismatch"));
    }

    @Test
    void decompositionRulesRetainNativeQuantitiesAndRejectDuplicatePhases() {
        var fluid = new MaterialDecompositionRule.FluidBinding(
                MaterialDecompositionRule.Phase.LIQUID, "molten.iron", 648648000L, 144);
        var routes = new java.util.HashSet<MaterialDecompositionRule.Route>();
        routes.add(MaterialDecompositionRule.Route.CENTRIFUGE);
        var rule = new MaterialDecompositionRule(routes, java.util.List.of(fluid), true);
        routes.clear();
        assertEquals(Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), rule.routes());
        assertThrows(UnsupportedOperationException.class, () -> rule.routes().clear());
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialDecompositionRule(
                        Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), java.util.List.of(fluid, fluid), false));
        assertEquals(MaterialDecompositionRule.class, MaterialProcessKeys.DECOMPOSITION.type());
    }

    @Test
    void processViewSupportsFormSpecificQueries() {
        var iron = new MaterialKey("Iron");
        var dust = new FormKey("dust");
        var process = MaterialProcessKey.multiple(new DataKey("test_addon", "throw_into_water"), String.class);
        var rule = new MaterialProcessRule<>(Set.of(dust), "wet");
        MaterialProcessesView view = new MaterialProcessesView() {
            public <T> java.util.List<MaterialProcessRule<T>> find(MaterialKey material, MaterialProcessKey<T> key) {
                if (!material.equals(iron) || !key.equals(process)) return java.util.List.of();
                @SuppressWarnings("unchecked")
                var typed = (MaterialProcessRule<T>) rule;
                return java.util.List.of(typed);
            }

            public Set<MaterialProcessKey<?>> keys(MaterialKey material) {
                return material.equals(iron) ? Set.of(process) : Set.of();
            }
        };
        assertEquals("wet", view.find(iron, dust, process).get(0).value());
        assertTrue(view.find(iron, new FormKey("ingot"), process).isEmpty());
        assertTrue(view.supports(iron, process));
    }

    @Test
    void processViewDistinguishesSingleAndMultipleRules() {
        var iron = new MaterialKey("Iron");
        var single = MaterialProcessKey.single(new DataKey("test_addon", "single"), String.class);
        var multiple = MaterialProcessKey.multiple(new DataKey("test_addon", "multiple"), String.class);
        var singleRule = new MaterialProcessRule<>(Set.of(), "one");
        var first = new MaterialProcessRule<>(Set.of(), "first");
        var second = new MaterialProcessRule<>(Set.of(), "second");
        MaterialProcessesView view = new MaterialProcessesView() {
            public <T> java.util.List<MaterialProcessRule<T>> find(MaterialKey material, MaterialProcessKey<T> key) {
                @SuppressWarnings("unchecked")
                var typed = key.equals(single)
                        ? java.util.List.of((MaterialProcessRule<T>) singleRule)
                        : key.equals(multiple)
                                ? java.util.List.of((MaterialProcessRule<T>) first, (MaterialProcessRule<T>) second)
                                : java.util.List.<MaterialProcessRule<T>>of();
                return typed;
            }

            public Set<MaterialProcessKey<?>> keys(MaterialKey material) {
                return Set.of(single, multiple);
            }
        };
        assertEquals("one", view.findSingle(iron, single).orElseThrow().value());
        assertTrue(view.findSingle(iron, multiple).isEmpty());
        assertEquals(2, view.find(iron, multiple).size());
    }

    @Test
    void oldCatalogImplementersExplicitlyRejectProcessViews() {
        MaterialCatalogView legacy = new MaterialCatalogView() {
            public long revision() {
                return 1;
            }

            public java.util.Optional<MaterialKey> resolve(String name) {
                return java.util.Optional.empty();
            }

            public MaterialTagsView tags() {
                return null;
            }

            public java.util.Optional<MaterialView> find(MaterialKey key) {
                return java.util.Optional.empty();
            }

            public java.util.List<MaterialView> materials() {
                return java.util.List.of();
            }
        };
        assertThrows(UnsupportedOperationException.class, legacy::processes);
        assertThrows(UnsupportedOperationException.class, legacy::forms);
    }

    @Test
    void formRulesViewIsQueryableAndImmutable() {
        var iron = new MaterialKey("Iron");
        var dust = new FormKey("dust");
        MaterialFormRulesView view = new MaterialFormRulesView() {
            public Set<FormKey> forms() {
                return Set.of(dust);
            }

            public Set<FormKey> forms(MaterialKey material) {
                return material.equals(iron) ? Set.of(dust) : Set.of();
            }

            public boolean allows(MaterialKey material, FormKey form) {
                return forms(material).contains(form);
            }
        };
        assertTrue(view.allows(iron, dust));
        assertThrows(UnsupportedOperationException.class, () -> view.forms().clear());
    }
}
