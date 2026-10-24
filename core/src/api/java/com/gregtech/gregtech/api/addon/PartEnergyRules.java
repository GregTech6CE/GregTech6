/* SPDX-License-Identifier: LGPL-3.0-or-later */

package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.machine.BasicMachineEnergy;

/**
 * Packet admission and the per-tick condition drain for an energy part or an auxiliary buffer.
 * Overload effects stay with the block entity. This class only decides the numbers.
 */
public final class PartEnergyRules {
    private PartEnergyRules() {}

    /**
     * How many packets may enter {@code stored}.
     * {@code overload} means the packet is larger than {@code max}: the caller consumes
     * {@link Admission#packets()} and does not add them to the buffer.
     * A packet below {@code min} is refused. {@code anyPacket} skips both checks.
     */
    public static Admission admit(long stored, long capacity, long size, long amount,
                                  long min, long max, boolean anyPacket) {
        if (size == 0 || size == Long.MIN_VALUE || amount <= 0 || capacity < 1)
            return new Admission(0, false);
        long sizeAbs = Math.abs(size);
        if (!anyPacket) {
            if (sizeAbs < min) return new Admission(0, false);
            if (sizeAbs > max) return new Admission(amount, true);
        }
        return new Admission(BasicMachineEnergy.accepted(stored, capacity, size, amount), false);
    }

    /** Enough energy pays {@code perTick} and is satisfied. A short buffer is left as it is. */
    public static Drain drain(long stored, long perTick) {
        long held = Math.max(0, stored);
        if (perTick < 1 || held < perTick) return new Drain(held, false);
        return new Drain(held - perTick, true);
    }

    public record Admission(long packets, boolean overload) {}

    public record Drain(long stored, boolean satisfied) {}
}
