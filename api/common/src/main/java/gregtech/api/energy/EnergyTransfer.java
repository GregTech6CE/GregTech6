/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

/**
 * Exact accounting for one packet-shaped energy request. The sign of {@code packetSize}
 * carries direction for energy types that support it; all unit amounts are nonnegative.
 * A part buffer can accept less than a full packet, so {@code transferredUnits} is authoritative.
 * This value does not itself move energy or specify whether the sender was debited.
 *
 * @param packetSize signed units per requested packet
 * @param requestedPackets requested packet count, never negative
 * @param transferredUnits total units actually moved, including a possible partial packet
 */
public record EnergyTransfer(long packetSize, long requestedPackets, long transferredUnits) {
    public EnergyTransfer {
        long requestedUnits = requestUnits(packetSize, requestedPackets);
        if (transferredUnits < 0 || transferredUnits > requestedUnits)
            throw new IllegalArgumentException("Transferred energy must be between zero and the request");
    }

    /**
     * Validates a request and returns its total unit count before any transfer takes place.
     * Zero packets is a valid no-op, including a zero packet size. A nonempty request needs
     * nonzero size. {@link Long#MIN_VALUE} cannot represent a magnitude and is always rejected.
     * A product outside the positive {@code long} range throws {@link ArithmeticException}.
     */
    public static long requestUnits(long packetSize, long packetCount) {
        if (packetCount < 0)
            throw new IllegalArgumentException("Packet count must not be negative");
        if (packetSize == Long.MIN_VALUE || (packetSize == 0 && packetCount > 0))
            throw new IllegalArgumentException("A nonempty packet request needs a representable nonzero size");
        return Math.multiplyExact(Math.abs(packetSize), packetCount);
    }

    /** Converts an accepted whole-packet count to exact energy units. */
    public static EnergyTransfer fromPackets(long packetSize, long requestedPackets, long transferredPackets) {
        requestUnits(packetSize, requestedPackets);
        if (transferredPackets < 0 || transferredPackets > requestedPackets)
            throw new IllegalArgumentException("Transferred packets must be between zero and the request");
        return new EnergyTransfer(packetSize, requestedPackets,
                requestUnits(packetSize, transferredPackets));
    }

    /** Absolute units per packet, ignoring rotational direction. */
    public long packetMagnitude() { return Math.abs(packetSize); }

    /** Total units requested; checked for overflow during construction. */
    public long requestedUnits() { return requestUnits(packetSize, requestedPackets); }

    /** Whole packets represented by {@link #transferredUnits()}, excluding its partial remainder. */
    public long completePackets() {
        return packetSize == 0 ? 0 : transferredUnits / packetMagnitude();
    }

    /** Units accepted beyond {@link #completePackets()}, not rounded up to another packet. */
    public long partialPacketUnits() {
        return packetSize == 0 ? 0 : transferredUnits % packetMagnitude();
    }

    /** Requested units that were not transferred. */
    public long remainingUnits() { return requestedUnits() - transferredUnits; }
}
