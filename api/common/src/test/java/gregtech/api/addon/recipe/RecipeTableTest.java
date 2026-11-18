/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTableTest {
    @Test
    void toStringIncludesEveryDeclarationField() {
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
        RecipeTable direct = new RecipeTable(false, "test_map",
                new RecipeTable.Presentation("Test", "gt6ce:textures/gui/test.png"),
                new RecipeTable.SlotCounts(3, 4, 1),
                new RecipeTable.SlotCounts(5, 6, 2),
                new RecipeTable.Rules(2, true, true, true, true));
        assertEquals(direct, table);
        assertEquals(3, table.inputItems());
        assertEquals(4, table.outputItems());
        assertEquals(1, table.minimalInputItems());
        assertEquals(5, table.inputFluids());
        assertEquals(6, table.outputFluids());
        assertEquals(2, table.minimalInputFluids());

        String text = table.toString();
        assertTrue(text.contains("fuel=false"));
        assertTrue(text.contains("name=test_map"));
        assertTrue(text.contains("title=Test"));
        assertTrue(text.contains("gui=gt6ce:textures/gui/test.png"));
        assertTrue(text.contains("itemSlots=SlotCounts[inputs=3, outputs=4, minimumInputs=1]"));
        assertTrue(text.contains("fluidTanks=SlotCounts[inputs=5, outputs=6, minimumInputs=2]"));
        assertTrue(text.contains("minimalInputs=2"));
        assertTrue(text.contains("needsOutputs=true"));
        assertTrue(text.contains("combinePower=true"));
        assertTrue(text.contains("bucketIn=true"));
        assertTrue(text.contains("bucketOut=true"));
    }

    @Test
    void machineAndFuelTablesAreNotEqual() {
        RecipeTable machine = RecipeTable.machine("test_map").title("Test")
                .gui("gt6ce:textures/gui/test.png").build();
        RecipeTable fuel = RecipeTable.fuel("test_map").title("Test")
                .gui("gt6ce:textures/gui/test.png").build();
        assertNotEquals(machine, fuel);
    }
}
