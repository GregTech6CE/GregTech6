/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyTransferTest {
    @Test
    void zeroPacketsIsANoop() {
        assertEquals(0, EnergyTransfer.requestUnits(0, 0));
    }

    @Test
    void requestRejectsInvalidCountsAndSizes() {
        assertThrows(IllegalArgumentException.class, () -> EnergyTransfer.requestUnits(32, -1));
        assertThrows(IllegalArgumentException.class, () -> EnergyTransfer.requestUnits(0, 1));
        assertThrows(IllegalArgumentException.class, () -> EnergyTransfer.requestUnits(Long.MIN_VALUE, 1));
    }

    @Test
    void requestRejectsOverflow() {
        assertThrows(ArithmeticException.class, () -> EnergyTransfer.requestUnits(Long.MAX_VALUE, 2));
    }

    @Test
    void fromPacketsTracksPartialPackets() {
        EnergyTransfer transfer = EnergyTransfer.fromPackets(32, 4, 3);
        assertEquals(96, transfer.transferredUnits());
        assertEquals(3, transfer.completePackets());
        assertEquals(0, transfer.partialPacketUnits());
        assertEquals(32, transfer.remainingUnits());
    }

    @Test
    void constructorAcceptsAPartialPacketButNotAnOverdraw() {
        EnergyTransfer partial = new EnergyTransfer(32, 4, 96 + 5);
        assertEquals(3, partial.completePackets());
        assertEquals(5, partial.partialPacketUnits());
        assertThrows(IllegalArgumentException.class, () -> new EnergyTransfer(32, 1, 33));
    }

    @Test
    void directionSignDoesNotChangeTheMagnitude() {
        EnergyTransfer transfer = EnergyTransfer.fromPackets(-32, 2, 1);
        assertEquals(32, transfer.packetMagnitude());
        assertEquals(32, transfer.transferredUnits());
        assertEquals(64, transfer.requestedUnits());
    }
}
