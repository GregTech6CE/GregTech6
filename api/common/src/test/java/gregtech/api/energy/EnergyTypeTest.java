/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import gregtech.api.addon.machine.FuelGenerator;
import gregtech.api.addon.machine.FuelOutput;
import gregtech.api.machine.MachineFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyTypeTest {
    @Test
    void knownNamesAreCanonicalized() {
        assertSame(EnergyType.EU, EnergyType.of("eu"));
        assertSame(EnergyType.STEAM, EnergyType.of("steam"));
        assertSame(EnergyType.ORDO, EnergyType.of("ordo"));
        assertSame(EnergyType.AER, EnergyType.of("AER"));
        assertSame(EnergyType.ANY, EnergyType.of("*"));
    }

    @Test
    void customNamesAreKeptAsWrittenAndCompareExactly() {
        EnergyType custom = EnergyType.of("Custom");
        assertEquals("Custom", custom.name());
        assertEquals(custom, EnergyType.of("Custom"));
        assertNotEquals(custom, EnergyType.of("custom"));
    }

    @Test
    void ofRejectsBlankAndInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> EnergyType.of(null));
        assertThrows(IllegalArgumentException.class, () -> EnergyType.of(""));
        assertThrows(IllegalArgumentException.class, () -> EnergyType.of(" EU"));
        assertThrows(IllegalArgumentException.class, () -> EnergyType.of("1EU"));
    }

    @Test
    void knownRejectsCustomNamesAndWildcards() {
        assertThrows(IllegalArgumentException.class, () -> EnergyType.known("Custom"));
        assertThrows(IllegalArgumentException.class, () -> EnergyType.known(EnergyType.ANY));
        assertThrows(IllegalArgumentException.class, () -> EnergyType.known((String) null));
        assertThrows(IllegalArgumentException.class, () -> EnergyType.known((EnergyType) null));
        assertSame(EnergyType.EU, EnergyType.known("eu"));
        assertThrows(IllegalArgumentException.class,
                () -> EnergyType.known(EnergyType.of("Custom")));
    }

    @Test
    void energyAndFuelOutputsCompareCustomTypesTheSameWay() {
        EnergyOutput output = EnergyOutput.builder(EnergyType.of("Custom"), 32, 1)
                .faces(MachineFace.FRONT).build();
        EnergyOutput sameSpelling = EnergyOutput.builder(EnergyType.of("Custom"), 32, 1)
                .faces(MachineFace.FRONT).build();
        EnergyOutput differentCase = EnergyOutput.builder(EnergyType.of("custom"), 32, 1)
                .faces(MachineFace.FRONT).build();
        assertEquals(output, sameSpelling);
        assertNotEquals(output, differentCase);

        FuelOutput fuel = FuelOutput.builder(EnergyType.of("Custom"), 32).build();
        FuelOutput fuelSameSpelling = FuelOutput.builder(EnergyType.of("Custom"), 32).build();
        FuelOutput fuelDifferentCase = FuelOutput.builder(EnergyType.of("custom"), 32).build();
        assertEquals(fuel, fuelSameSpelling);
        assertNotEquals(fuel, fuelDifferentCase);
    }

    @Test
    void fuelOutputStoresThePlacement() {
        FuelOutput front = FuelOutput.builder(EnergyType.EU, 32).build();
        assertEquals(FuelOutput.Placement.FRONT, front.placement());
        assertTrue(front.front());
        assertEquals(0, front.behind());

        FuelOutput back = FuelOutput.builder(EnergyType.EU, 32).back().build();
        assertEquals(FuelOutput.Placement.BACK, back.placement());
        assertTrue(back.back());

        FuelOutput behind = FuelOutput.builder(EnergyType.EU, 32).behind(3).build();
        assertEquals(FuelOutput.Placement.BEHIND, behind.placement());
        assertEquals(3, behind.behind());
        assertFalse(behind.front());
        assertEquals(behind, FuelOutput.builder(EnergyType.EU, 32).behind(3).build());
        assertNotEquals(behind, FuelOutput.builder(EnergyType.EU, 32).behind(2).build());

        assertThrows(IllegalArgumentException.class, () -> FuelOutput.builder(EnergyType.EU, 32).behind(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> FuelOutput.builder(EnergyType.EU, 32).behind(FuelGenerator.MAX_BEHIND + 1).build());
    }

    @Test
    void signTimeAndWildcardFlagsFollowTheType() {
        assertTrue(EnergyType.RU.signedDirection());
        assertTrue(EnergyType.of("ku").signedDirection());
        assertFalse(EnergyType.EU.signedDirection());
        assertTrue(EnergyType.TU.time());
        assertFalse(EnergyType.HU.time());
        assertTrue(EnergyType.ANY.wildcard());
        assertFalse(EnergyType.EU.wildcard());
    }
}
