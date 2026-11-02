/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.machine;

import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiblockMachineTest {
    private static StructureLayout layout() {
        return StructureLayout.pattern("shared_shell").layer(0, "SA")
                .where('A', CellMatch.block("minecraft:stone"), StructureRole.CASING).build();
    }

    private static RecipeMachine recipe(String name, boolean multi) {
        var builder = RecipeMachine.builder(MachineCasing.builder(name, "iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build()).recipeMap("test_map");
        if (multi) builder.largeRecipe();
        return builder.build();
    }

    @Test void bindsTheSameNamedLayoutToDifferentControllerTypes() {
        var layout = layout();
        var first = recipe("first_controller", true).withStructure(layout);
        var second = recipe("second_controller", true).withStructure(layout);
        assertSame(layout, first.structure());
        assertSame(layout, second.structure());
        assertEquals("first_controller", first.casing().machineType());
        assertEquals("second_controller", second.casing().machineType());
    }

    @Test void rejectsMissingStructuresAndIncorrectControllerKinds() {
        assertThrows(NullPointerException.class, () -> recipe("large", true).withStructure(null));
        assertThrows(IllegalArgumentException.class, () -> recipe("small", false).withStructure(layout()));
        assertThrows(IllegalArgumentException.class, () -> recipe("large", true).withStructure(layout()).withStructure(layout()));
    }
}
