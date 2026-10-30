/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import gregtech.api.energy.EnergyType;
import gregtech.api.material.GTMaterial;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicMachineParametersTest {
    private static final GTMaterial MATERIAL = new GTMaterial(1, "Test", "Test", 0xFFFFFF);

    @Test
    void defaultEnergyRangeDependsOnTheEnergyType() {
        assertEquals(16, BasicMachineParameters.defaultEnergyInMin(EnergyType.EU, 32));
        assertEquals(64, BasicMachineParameters.defaultEnergyInMax(EnergyType.EU, 32));
        assertEquals(1, BasicMachineParameters.defaultEnergyInMin(EnergyType.HU, 1));
        assertEquals(1, BasicMachineParameters.defaultEnergyInMin(EnergyType.TU, 32));
        assertEquals(16, BasicMachineParameters.defaultEnergyInMax(EnergyType.TU, 32));
    }

    @Test
    void defaultEnergyRangeRejectsUnknownTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> BasicMachineParameters.defaultEnergyInMin(EnergyType.of("Custom"), 32));
        assertThrows(IllegalArgumentException.class,
                () -> BasicMachineParameters.defaultEnergyInMax(EnergyType.ANY, 32));
        assertThrows(IllegalArgumentException.class,
                () -> BasicMachineParameters.defaultEnergyInMin(null, 32));
    }

    @Test
    void constructorValidatesItsParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> parameters(EnergyType.of("Custom"), 32, 16, 64, 0));
        assertThrows(IllegalArgumentException.class,
                () -> parameters(EnergyType.EU, 32, 0, 64, 0));
        assertThrows(IllegalArgumentException.class,
                () -> parameters(EnergyType.EU, 32, 16, 64, 16));
    }

    @Test
    void defaultRangeRejectsAnImpossibleNominalInput() {
        assertThrows(IllegalArgumentException.class,
                () -> profile(EnergyType.EU, 0, EnergyRange.defaulted()));
        assertThrows(IllegalArgumentException.class,
                () -> profile(EnergyType.EU, Long.MAX_VALUE, EnergyRange.defaulted()));

        BasicMachineParameters.EnergyProfile tu = profile(EnergyType.TU, 0, EnergyRange.defaulted());
        assertEquals(1, tu.energyInMin());
        assertEquals(16, tu.energyInMax());
    }

    @Test
    void energyRangeKeepsDeclaredDefaultedAndOmittedDistinct() {
        BasicMachineParameters.EnergyProfile declared = profile(EnergyType.EU, 32, EnergyRange.declared(3, 9));
        assertEquals(3, declared.energyInMin());
        assertEquals(9, declared.energyInMax());
        assertEquals(3, declared.acceptedEnergyInMin(true));
        assertEquals(9, declared.acceptedEnergyInMax(true));

        BasicMachineParameters.EnergyProfile defaulted = profile(EnergyType.EU, 32, EnergyRange.defaulted());
        assertEquals(16, defaulted.energyInMin());
        assertEquals(64, defaulted.energyInMax());
        assertEquals(512, defaulted.acceptedEnergyInMin(true));

        BasicMachineParameters.EnergyProfile omitted = profile(EnergyType.HU, 32, EnergyRange.omitted());
        assertEquals(0, omitted.energyInMin());
        assertEquals(0, omitted.energyInMax());
        assertEquals(1, omitted.acceptedEnergyInMin(true));
        assertEquals(4096, omitted.acceptedEnergyInMax(true));
    }

    private static BasicMachineParameters.EnergyProfile profile(
            EnergyType type, long energyIn, EnergyRange range) {
        return new BasicMachineParameters.EnergyProfile(type, 1, energyIn, 32, range);
    }

    private static BasicMachineParameters parameters(
            EnergyType energyType, long energyIn, long energyInMin, long energyInMax, int minimumLightAbove) {
        EnergyRange range = energyInMin == 0 && energyInMax == 0
                ? EnergyRange.defaulted() : EnergyRange.declared(energyInMin, energyInMax);
        return new BasicMachineParameters(
                "test_machine",
                MATERIAL,
                "test_machine",
                new BasicMachineParameters.EnergyProfile(energyType, 1, energyIn, 32, range),
                1F,
                1F,
                MachineFaceMasks.ALL_SIDES,
                List.of(),
                new BasicMachineParameters.Options(1,
                        BasicMachineParameters.Options.CheapOverclocking.UNSPECIFIED, minimumLightAbove,
                        false, false),
                BasicMachineParameters.IoPorts.NONE);
    }
}
