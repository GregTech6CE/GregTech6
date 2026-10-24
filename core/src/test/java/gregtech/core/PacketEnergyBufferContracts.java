package gregtech.core;

import gregtech.api.energy.PacketEnergyBuffer;
import java.math.BigInteger;

/** Finite packet accounting, including requests whose unchecked product wraps. */
public final class PacketEnergyBufferContracts {
    private static int assertions;

    public static void main(String[] args) {
        long[] values = {1, 2, 3, 32, 1024, Integer.MAX_VALUE, Long.MAX_VALUE / 2,
                Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE - 1, Long.MAX_VALUE};
        for (long capacity : values) for (long size : values) for (long requested : values) {
            var buffer = new PacketEnergyBuffer(capacity);
            buffer.setCanExtract(true);
            long expected = BigInteger.valueOf(capacity).divide(BigInteger.valueOf(size))
                    .min(BigInteger.valueOf(requested)).longValueExact();
            require(buffer.receiveEnergy(size, requested, false) == expected, "simulated receive");
            require(buffer.stored() == 0 && buffer.lastSize() == 32 && buffer.lastAmount() == 1,
                    "receive simulation leaves storage and telemetry untouched");
            require(buffer.receiveEnergy(size, requested, true) == expected, "actual receive");
            require(buffer.stored() == expected * size, "received energy conservation");
            long stored = buffer.stored();
            require(buffer.extractEnergy(size, requested, false) == expected, "simulated extraction");
            require(buffer.stored() == stored, "extraction simulation leaves storage untouched");
            require(buffer.extractEnergy(size, requested, true) == expected, "actual extraction");
            require(buffer.stored() == 0, "extraction conserves energy");
        }
        var buffer = new PacketEnergyBuffer(100);
        buffer.setCanExtract(true);
        for (long invalid : new long[]{Long.MIN_VALUE, -1, 0}) {
            require(buffer.receiveEnergy(invalid, 1, true) == 0, "reject nonpositive packet");
            require(buffer.receiveEnergy(1, invalid, true) == 0, "reject nonpositive amount");
            buffer.setStored(100);
            require(buffer.extractEnergy(invalid, 1, true) == 0, "reject invalid extraction packet");
            require(buffer.extractEnergy(1, invalid, true) == 0, "reject invalid extraction amount");
            buffer.setStored(invalid);
            require(buffer.stored() == 0, "restored storage is nonnegative");
        }
        buffer.setStored(Long.MAX_VALUE);
        require(buffer.stored() == 100, "restore clamps upper bound");
        buffer.setCapacity(33);
        require(buffer.stored() == 33, "capacity shrink clamps energy");
        require(buffer.extractEnergy(32, Long.MAX_VALUE, true) == 1 && buffer.stored() == 1,
                "partial packet remainder is retained");
        buffer.setCanReceive(false);
        require(buffer.receiveEnergy(1, 1, true) == 0 && buffer.stored() == 1, "receive gate");
        buffer.setCanExtract(false);
        require(buffer.extractEnergy(1, 1, true) == 0 && buffer.stored() == 1, "extract gate");
        System.out.println("Packet energy buffer contracts passed: " + assertions + " assertions");
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
