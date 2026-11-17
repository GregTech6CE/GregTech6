package gregtech.api.addon.material;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SpecialProcessValueTest {
    @Test
    void methodDerivesStableMaterialTags() {
        var method = new SpecialProcessMethod(
                MaterialProcessKey.multiple(
                        new DataKey("example", "special_process/wash"), SpecialProcessValue.class),
                "gt.recipe.bath",
                "example.special_process.wash");

        assertEquals(new DataKey("example", "special_process/wash/target"), method.targetTag());
        assertEquals(new DataKey("example", "special_process/wash/reagent"), method.reagentTag());
    }

    @Test
    void targetRejectsIncompleteRecipePlans() {
        var output = new SpecialProcessValue.ItemOutput(
                new MaterialKey("Iron"),
                List.of(new SpecialProcessValue.FormCount(new FormKey("dust"), 1)),
                10_000);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SpecialProcessValue.Target(
                        Map.of(), List.of(output), List.of(), 1, 1, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpecialProcessValue.Target(
                        Map.of(new FormKey("dust"), 1), List.of(), List.of(), 1, 1, 0));
    }

    @Test
    void fluidOutputUsesOneAmountAndPreservesLegacyForms() {
        var water = new MaterialKey("Water");
        var liquid = new FormKey("liquid");
        var gas = new FormKey("gas");
        var legacyForms = List.of(
                new SpecialProcessValue.FormCount(liquid, 5), new SpecialProcessValue.FormCount(gas, 2));
        var legacy = new SpecialProcessValue.FluidOutput(water, legacyForms, 250);
        var phaseOnly = new SpecialProcessValue.FluidOutput(water, 250, List.of(liquid, gas));

        assertEquals(legacyForms, legacy.forms());
        assertEquals(legacy, new SpecialProcessValue.FluidOutput(water, legacyForms, 250));
        assertEquals(legacy.phases(), phaseOnly.phases());
        assertEquals(legacy.millibuckets(), phaseOnly.millibuckets());
    }

    @Test
    void unsupportedCatalogFailsExplicitly() {
        MaterialCatalogView catalog = new MaterialCatalogView() {
            @Override
            public long revision() {
                return 0;
            }

            @Override
            public Optional<MaterialKey> resolve(String name) {
                return Optional.empty();
            }

            @Override
            public MaterialTagsView tags() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<MaterialView> find(MaterialKey key) {
                return Optional.empty();
            }

            @Override
            public List<MaterialView> materials() {
                return List.of();
            }
        };

        assertThrows(UnsupportedOperationException.class, catalog::specialProcesses);
    }
}
