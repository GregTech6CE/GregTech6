/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MachineCasingTest {
    @Test
    void builderValidatesTheIdAndMaterial() {
        assertThrows(IllegalArgumentException.class, () -> MachineCasing.builder("Bad ID", "iron").build());
        assertThrows(IllegalArgumentException.class, () -> MachineCasing.builder("test_machine", " ").build());
        assertThrows(IllegalArgumentException.class,
                () -> MachineCasing.builder("test_machine", "iron").machineType("Bad Type!").build());
    }

    @Test
    void resolveRevalidatesTheMachineType() {
        MachineCasing casing = MachineCasing.builder("test_machine", "iron").build();
        assertThrows(IllegalArgumentException.class, () -> casing.resolve("Bad Type!", true, -5));
        assertEquals("addon_machine", casing.resolve("addon_machine", true, 1).machineType());
    }

    @Test
    void slashIdsAreValidUntilTheMachineTypeIsDerived() {
        MachineCasing casing = MachineCasing.builder("addon/casing", "iron").build();
        assertEquals("addon/casing", casing.id());
    }
}
