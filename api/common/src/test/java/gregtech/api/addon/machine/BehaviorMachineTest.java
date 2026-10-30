/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorMachineTest {
    private static final String GUI = "gt6ce:textures/gui/test.png";

    @Test
    void buildsWithDefaultsAndKeepsTheIdAsMachineType() {
        BehaviorMachine machine = builder("addon_gen")
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .build();
        assertEquals("addon_gen", machine.casing().machineType());
        assertEquals(5, machine.energy().faces().size());
    }

    @Test
    void slashIdsNeedAnExplicitMachineType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> builder("addon/gen")
                        .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                        .gui(GUI)
                        .build());
        assertTrue(error.getMessage().contains("machineType("));

        BehaviorMachine machine = BehaviorMachine.builder(
                        MachineCasing.builder("addon/gen", "iron").machineType("addon_gen").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .build();
        assertEquals("addon_gen", machine.casing().machineType());
    }

    @Test
    void rejectsOverlappingGuiWidgets() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .item(30, 30, BehaviorMachine.SlotRole.INPUT)
                .item(30, 30, BehaviorMachine.SlotRole.OUTPUT)
                .build());
        assertTrue(error.getMessage().contains("overlaps"));
    }

    @Test
    void rejectsWidgetsOutsideTheGui() {
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .item(170, 30, BehaviorMachine.SlotRole.INPUT)
                .build());
    }

    @Test
    void validatesGuiSizeAndTexture() {
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui("not a texture")
                .build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .guiSize(100, 100)
                .build());
    }

    @Test
    void rejectsInvalidTiersAndEmptyEnergyFaces() {
        assertThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(
                        MachineCasing.builder("test_machine", "iron").tier(0).build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .build());
        assertThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(
                        MachineCasing.builder("test_machine", "iron").tier(256).build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui(GUI)
                .build());
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(EnergyInput.builder(EnergyType.EU, 32).faces().build())
                .gui(GUI)
                .build());
    }

    @Test
    void rejectsAuxiliaryEnergyOnTheSameFaceAndType() {
        EnergyInput main = EnergyInput.builder(EnergyType.EU, 32).build();
        EnergyInput auxiliary = EnergyInput.builder(EnergyType.EU, 32)
                .range(1, 64)
                .capacity(100)
                .faces(gregtech.api.machine.MachineFace.BOTTOM)
                .build();
        assertThrows(IllegalArgumentException.class, () -> builder("test_machine")
                .energy(main)
                .auxiliaryEnergy(auxiliary)
                .gui(GUI)
                .build());
    }

    private static BehaviorMachine.Builder builder(String id) {
        return BehaviorMachine.builder(MachineCasing.builder(id, "iron").build());
    }
}
