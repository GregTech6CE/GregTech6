package gregtech.engine.addon;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.addon.machine.AddonMachine;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.multiblock.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class AddonMachineDeclarationsTest {
    private static RecipeMachine machine(String id) {
        return RecipeMachine.builder(MachineCasing.builder(id, "iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .recipeMap("map")
                .largeRecipe()
                .build();
    }

    private static StructureLayout layout(String name, String block) {
        return StructureLayout.pattern(name)
                .layer(0, "SA")
                .where('A', CellMatch.block(block), StructureRole.CASING)
                .build();
    }

    @Test
    void explicitBindingRegistersTheShapeUnderTheControllerType() {
        var controller = machine("controller");
        var shape = layout("reusable", "minecraft:stone");
        var resolved = AddonMachineDeclarations.resolve(List.of(controller.withStructure(shape)));
        assertEquals(List.of(controller), resolved.machines());
        assertEquals(List.of(layout("controller", "minecraft:stone")), resolved.layouts());
        assertEquals("reusable", shape.name());
    }

    @Test
    void rejectsMissingOrConflictingBindings() {
        var controller = machine("controller");
        assertThrows(
                IllegalArgumentException.class, () -> AddonMachineDeclarations.resolve(List.of(controller)));
        assertThrows(
                IllegalArgumentException.class,
                () -> AddonMachineDeclarations.resolve(List.of(
                        controller.withStructure(layout("stone_shape", "minecraft:stone")),
                        controller.withStructure(layout("dirt_shape", "minecraft:dirt")))));
    }

    @Test
    void oneShapeCanBindToSeveralControllerTypes() {
        var shape = layout("shared", "minecraft:stone");
        var resolved = AddonMachineDeclarations.resolve(
                List.<AddonMachine>of(
                        machine("first").withStructure(shape), machine("second").withStructure(shape)));
        assertEquals(
                List.of("first", "second"),
                resolved.layouts().stream().map(StructureLayout::name).toList());
        assertEquals(
                resolved.layouts().get(0).cells(), resolved.layouts().get(1).cells());
    }
}
