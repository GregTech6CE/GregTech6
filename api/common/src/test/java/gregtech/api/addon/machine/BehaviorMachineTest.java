/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineIo;
import java.util.ArrayList;
import java.util.List;
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
    void groupedConstructorMatchesBuilder() {
        var casing = MachineCasing.builder("compat_machine", "iron").build();
        var main = EnergyInput.builder(EnergyType.EU, 32).build();
        var auxiliary = EnergyInput.builder(EnergyType.HU, 16)
                .range(1, 32).capacity(100).faces(MachineFace.TOP).build();
        var output = EnergyOutput.builder(EnergyType.KU, 64, 2).faces(MachineFace.BACK).build();
        var io = MachineIo.builder().itemInputs(MachineFace.LEFT).build();
        var item = new BehaviorMachine.ItemSlot(30, 17, BehaviorMachine.SlotRole.INPUT);
        var fluid = new BehaviorMachine.FluidSlot(1000, 110, 12, BehaviorMachine.SlotRole.OUTPUT);
        var toggle = new BehaviorMachine.Toggle("machine.active", 140, 15, true);
        var progress = new BehaviorMachine.Progress(75, 32);
        var items = new ArrayList<>(List.of(item));

        var ports = new BehaviorMachine.EnergyPorts(main, auxiliary, output);
        var layout = new BehaviorMachine.GuiLayout(192, 180, GUI, 7, 10, items, List.of(fluid), List.of(toggle), progress);
        var automation = new BehaviorMachine.Automation(io, MachineFace.RIGHT, true);
        var options = new BehaviorMachine.RuntimeOptions(6, List.of("gt.recipe.compat"), List.of("active"));
        var direct = new BehaviorMachine(casing, ports, "compat_be", layout, automation, options);
        var built = BehaviorMachine.builder(casing)
                .energy(main).auxiliaryEnergy(auxiliary).energyOutput(output)
                .blockEntityId("compat_be")
                .guiSize(192, 180).gui(GUI).energyGauge(7, 10)
                .item(item.x(), item.y(), item.role())
                .fluid(fluid.capacity(), fluid.x(), fluid.y(), fluid.role())
                .toggle(toggle.translationKey(), toggle.x(), toggle.y(), toggle.initiallyOn())
                .progress(progress.x(), progress.y())
                .maxRange(6).recipeMaps("gt.recipe.compat")
                .io(io).gasValve(MachineFace.RIGHT).signalOutput(true).clientState("active")
                .build();

        items.clear();
        assertEquals(built, direct);
        assertEquals(List.of(item), direct.items());
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
                .faces(MachineFace.BOTTOM)
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
