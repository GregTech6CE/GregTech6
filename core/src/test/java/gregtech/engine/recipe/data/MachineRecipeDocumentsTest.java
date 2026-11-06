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
}
