/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTableTest {
    @Test
    void toStringIncludesEveryDeclarationField() {
        RecipeTable table = RecipeTable.machine("test_map")
                .title("Test")
                .gui("gt6ce:textures/gui/test.png")
                .inputItems(3)
                .minimalInputs(2)
                .needsOutputs(true)
                .combinePower(true)
                .bucketIn(true)
                .bucketOut(true)
                .build();
        String text = table.toString();
        assertTrue(text.contains("title=Test"));
        assertTrue(text.contains("gui=gt6ce:textures/gui/test.png"));
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
