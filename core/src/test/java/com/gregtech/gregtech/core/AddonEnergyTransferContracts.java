package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.EnergyTransfer;
import java.math.BigInteger;

/** Public addon energy accounting: signed packets, partial part charges and overflow rejection. */
public final class AddonEnergyTransferContracts {
    private static int assertions;

    public static void main(String[] args) {
        EnergyTransfer adjacent = EnergyTransfer.fromPackets(32, 4, 3);
        require(adjacent.requestedUnits() == 128 && adjacent.transferredUnits() == 96
                && adjacent.completePackets() == 3 && adjacent.partialPacketUnits() == 0
                && adjacent.remainingUnits() == 32, "packet receiver reports accepted, not remaining, packets");

        EnergyTransfer part = new EnergyTransfer(-32, 4, 70);
        require(part.packetSize() == -32 && part.packetMagnitude() == 32
                && part.completePackets() == 2 && part.partialPacketUnits() == 6
                && part.remainingUnits() == 58, "partial part charge preserves every debited unit and direction");
        require(new EnergyTransfer(0, 0, 0).completePackets() == 0
                && new EnergyTransfer(0, 0, 0).partialPacketUnits() == 0,
                "empty request does not divide by zero");
        require(new EnergyTransfer(Long.MAX_VALUE, 0, 0).requestedUnits() == 0,
                "nonzero size with zero packets is empty");
        require(EnergyTransfer.fromPackets(-Long.MAX_VALUE, 1, 1).transferredUnits() == Long.MAX_VALUE,
                "maximum representable signed packet");

        long[] sizes = {1, -1, 3, -3, 32, -32, Integer.MAX_VALUE, Long.MAX_VALUE / 2,
                -Long.MAX_VALUE / 2, Long.MAX_VALUE, -Long.MAX_VALUE};
        long[] counts = {0, 1, 2, 3, 64, Long.MAX_VALUE};
        for (long size : sizes) for (long count : counts) {
            BigInteger requested = BigInteger.valueOf(size).abs().multiply(BigInteger.valueOf(count));
            if (requested.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                expect(ArithmeticException.class, () -> EnergyTransfer.requestUnits(size, count));
                expect(ArithmeticException.class, () -> new EnergyTransfer(size, count, 0));
                continue;
            }
            long units = requested.longValueExact();
            for (long transferred : new long[] {0, units / 2, units}) {
                EnergyTransfer result = new EnergyTransfer(size, count, transferred);
                BigInteger[] parts = BigInteger.valueOf(transferred)
                        .divideAndRemainder(BigInteger.valueOf(size).abs());
                require(result.requestedUnits() == units, "request total agrees with unbounded arithmetic");
                require(result.completePackets() == parts[0].longValueExact()
                        && result.partialPacketUnits() == parts[1].longValueExact(),
                        "partial packet decomposition agrees with unbounded arithmetic");
                require(BigInteger.valueOf(result.transferredUnits()).add(BigInteger.valueOf(result.remainingUnits()))
                        .equals(requested), "transferred plus remaining conserves requested energy");
            }
        }

        expect(IllegalArgumentException.class, () -> new EnergyTransfer(32, -1, 0));
        expect(IllegalArgumentException.class, () -> new EnergyTransfer(0, 1, 0));
        expect(IllegalArgumentException.class, () -> new EnergyTransfer(Long.MIN_VALUE, 0, 0));
        expect(IllegalArgumentException.class, () -> new EnergyTransfer(Long.MIN_VALUE, 1, 0));
        expect(IllegalArgumentException.class, () -> new EnergyTransfer(32, 2, -1));
        expect(IllegalArgumentException.class, () -> new EnergyTransfer(32, 2, 65));
        expect(IllegalArgumentException.class, () -> EnergyTransfer.fromPackets(32, 2, -1));
        expect(IllegalArgumentException.class, () -> EnergyTransfer.fromPackets(32, 2, 3));
        expect(ArithmeticException.class, () -> EnergyTransfer.fromPackets(Long.MAX_VALUE, 2, 0));
        System.out.println("Addon energy transfer contracts passed: " + assertions + " assertions");
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable operation) {
        assertions++;
        try {
            operation.run();
        } catch (RuntimeException failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
