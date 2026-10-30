/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonLimits;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeMachineTest {
    @Test
    void buildsWithDefaults() {
        RecipeMachine machine = builder("addon_recipe").energy(eu(32)).recipeMap("test_map").build();
        assertEquals("addon_recipe", machine.casing().machineType());
        assertEquals(1, machine.parallel());
        assertEquals(AddonLimits.DEFAULT_EFFICIENCY, machine.efficiency());
        assertTrue(machine.constantEnergy());
    }

    @Test
    void slashIdsNeedAnExplicitMachineType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RecipeMachine.builder(MachineCasing.builder("addon/slash", "iron").build())
                        .energy(eu(32))
                        .recipeMap("test_map")
                        .build());
        assertTrue(error.getMessage().contains("machineType("));
    }

    @Test
    void validatesTierAndEnergyBounds() {
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine", 0)
                .energy(eu(32)).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine", 256)
                .energy(eu(32)).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(0)).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(AddonLimits.MAX_PER_TICK + 1)).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).range(64, 128).build())
                .recipeMap("test_map").build());
    }

    @Test
    void zeroCapacityCountsAsUnset() {
        RecipeMachine machine = builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).capacity(0).build())
                .recipeMap("test_map").build();
        assertEquals(1, machine.parallel());
    }

    @Test
    void rejectsEnergyFeaturesOnlyBehaviorMachinesSupport() {
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).anyType().build()).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).capacity(64).build()).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).anyPacket().build()).recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).overload(false).build()).recipeMap("test_map").build());
    }

    @Test
    void auxiliaryEnergyNeedsItsOwnFacesAndBuffer() {
        EnergyInput valid = EnergyInput.builder(EnergyType.HU, 32)
                .range(1, 64)
                .capacity(100)
                .faces(MachineFace.BOTTOM)
                .build();
        RecipeMachine machine = builder("test_machine")
                .energy(eu(32))
                .auxiliaryEnergy(valid)
                .recipeMap("test_map")
                .build();
        assertEquals(1, machine.auxiliary().faces().size());

        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32))
                .auxiliaryEnergy(EnergyInput.builder(EnergyType.HU, 32).capacity(100).faces(MachineFace.BOTTOM).build())
                .recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32))
                .auxiliaryEnergy(EnergyInput.builder(EnergyType.EU, 32).range(1, 64).capacity(100)
                        .faces(MachineFace.BOTTOM).build())
                .recipeMap("test_map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32))
                .startupEnergy(true)
                .recipeMap("test_map").build());
    }

    @Test
    void validatesRemainingDeclarationFields() {
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32)).recipeMap("Bad Map").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32)).recipeMap("test_map").parallel(0).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32)).recipeMap("test_map").parallel(AddonLimits.MAX_PARALLEL + 1).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32)).recipeMap("test_map").efficiency(0).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32)).recipeMap("test_map").structureKeys(List.of("Bad Key")).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(eu(32)).recipeMap("test_map").structureKeys(List.of("key", "key")).build());
    }

    @Test
    void toStringIncludesEveryDeclarationField() {
        RecipeMachine machine = builder("test_machine")
                .energy(eu(32))
                .recipeMap("test_map")
                .structureKeys(List.of("key"))
                .build();
        String text = machine.toString();
        assertTrue(text.contains("io="));
        assertTrue(text.contains("structureKeys=[key]"));
        assertTrue(text.contains("constantEnergy="));
        assertTrue(text.contains("minimumLightAbove="));
    }

    private static RecipeMachine.Builder builder(String id) {
        return builder(id, 1);
    }

    private static RecipeMachine.Builder builder(String id, int tier) {
        return RecipeMachine.builder(MachineCasing.builder(id, "iron").tier(tier).build());
    }

    private static EnergyInput eu(long perTick) {
        return EnergyInput.builder(EnergyType.EU,perTick).build();
    }
}
