/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTableTest {
    @Test
    void builderPreservesGroupedDeclarationFields() {
        RecipeTable table = RecipeTable.machine("test_map")
                .title("Test")
                .gui("gt6ce:textures/gui/test.png")
                .inputItems(3)
                .outputItems(4)
                .minimalInputItems(1)
                .inputFluids(5)
                .outputFluids(6)
                .minimalInputFluids(2)
                .minimalInputs(2)
                .needsOutputs(true)
                .combinePower(true)
                .bucketIn(true)
                .bucketOut(true)
                .build();

        assertEquals("test_map", table.name());
        assertEquals(new RecipeTable.Presentation("Test", "gt6ce:textures/gui/test.png"), table.presentation());
        assertEquals(new RecipeTable.SlotCounts(3, 4, 1), table.itemSlots());
        assertEquals(new RecipeTable.SlotCounts(5, 6, 2), table.fluidTanks());
        assertEquals(new RecipeTable.Rules(2, true, true, true, true), table.rules());
    }

    @Test
    void machineAndFuelTablesAreNotEqual() {
        RecipeTable machine = RecipeTable.machine("test_map").title("Test")
                .gui("gt6ce:textures/gui/test.png").build();
        RecipeTable fuel = RecipeTable.fuel("test_map").title("Test")
                .gui("gt6ce:textures/gui/test.png").build();
        assertFalse(machine.fuel());
        assertTrue(fuel.fuel());
        assertNotEquals(machine, fuel);
    }
}
