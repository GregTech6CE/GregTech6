package gregtech.engine.addon;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AddonMachineSubmissionTest {
    private static final String ADDON = "structure_submission_test";

    @AfterEach
    void removeOwnSubmission() throws ReflectiveOperationException {
        // Leave the shared startup queue intact; remove only this test's isolated contributor.
        var field = AddonMachines.class.getDeclaredField("QUEUE");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).remove(ADDON);
    }

    @Test
    void directSubmissionAcceptsAnExplicitStructure() {
        var machine = BehaviorMachine.builder(
                        MachineCasing.builder("controller", "iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui("gt6ce:textures/gui/test.png")
                .build();
        var layout = StructureLayout.pattern("shared_shape")
                .layer(0, "SA")
                .where('A', CellMatch.block("minecraft:stone"), StructureRole.CASING)
                .build();

        assertDoesNotThrow(() -> AddonMachines.acceptContributor(
                ADDON, List.of(), List.of(machine.withStructure(layout)), List.of(), List.of()));
        assertTrue(AddonMachines.queuedPipes().stream()
                .anyMatch(row -> row.addonId().equals(ADDON)));
    }

    @Test
    void directSubmissionRejectsMissingStructuresBeforeMaterialLookup() {
        var machine = RecipeMachine.builder(
                        MachineCasing.builder("controller", "iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .recipeMap("test_map")
                .largeRecipe()
                .build();

        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> AddonMachines.acceptContributor(ADDON, List.of(), List.of(machine), List.of(), List.of()));
        assertTrue(failure.getMessage().contains("Multiblock has no structure: controller"));
        assertTrue(AddonMachines.queuedPipes().stream()
                .noneMatch(row -> row.addonId().equals(ADDON)));
    }
}
