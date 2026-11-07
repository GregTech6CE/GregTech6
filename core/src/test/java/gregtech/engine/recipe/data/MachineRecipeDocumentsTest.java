package gregtech.engine.recipe.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Numeric fields are narrowed only after their full range is validated. */
final class MachineRecipeDocumentsTest {
    @Test
    void negativeValuesOutsideTheIntRangeAreRejectedInsteadOfWrapping() {
        assertThrows(IllegalArgumentException.class, () -> MachineRecipeDocuments.removal("""
                        {"map":"gt.recipe.crusher",
                         "item_inputs":[{"item":"minecraft:stone","count":-4294967295}]}
                        """));
        assertThrows(IllegalArgumentException.class, () -> MachineRecipeDocuments.removal("""
                        {"map":"gt.recipe.crusher",
                         "item_outputs":[{"item":"minecraft:stone","count":2147483648}]}
                        """));
    }

    @Test
    void boundaryCountsAreAccepted() {
        var spec = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone","count":2147483647}]}
                """);
        assertEquals(2147483647, spec.itemInputs().get(0).count());
    }

    @Test
    void nonIntegralValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MachineRecipeDocuments.removal("""
                        {"map":"gt.recipe.crusher",
                         "item_inputs":[{"item":"minecraft:stone","count":1.5}]}
                        """));
    }

    @Test
    void removalConditionsDoNotTakePriority() {
        // Removal matches inputs/outputs; a priority filter would silently change an old datapack's
        // meaning, so the field stays an addition-only annotation and is rejected on removal.
        assertThrows(IllegalArgumentException.class, () -> MachineRecipeDocuments.removal("""
                        {"map":"gt.recipe.crusher","priority":5,
                         "item_inputs":[{"item":"minecraft:stone"}]}
                        """));
    }

    @Test
    void priorityIsOptionalAndRoundTripsOnlyWhenSet() {
        var ordinary = MachineRecipeDocuments.addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"item":"minecraft:gravel"}],
                 "eu_per_tick":16,"duration":20}
                """);
        assertEquals(0, ordinary.priority());
        assertEquals("""
                {"map":"gt.recipe.crusher","item_inputs":[{"item":"minecraft:stone","count":1}],\
                "fluid_inputs":[],"item_outputs":[{"item":"minecraft:gravel","count":1,"chance":10000}],\
                "fluid_outputs":[],"eu_per_tick":16,"duration":20,"special":0}""", MachineRecipeDocuments.canonical(ordinary));

        var prioritized = MachineRecipeDocuments.addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"item":"minecraft:gravel"}],
                 "eu_per_tick":16,"duration":20,"priority":7}
                """);
        assertEquals(7, prioritized.priority());
        assertEquals(prioritized, MachineRecipeDocuments.addition(MachineRecipeDocuments.canonical(prioritized)));
    }
}
