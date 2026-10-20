package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.machine.BasicMachineEnergy;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Tick-end storage and declared output limits for a behavior machine. */
public final class BehaviorEnergyRules {
    private BehaviorEnergyRules() {}

    /** Capacity 0 keeps the rated-input drain. A positive capacity keeps energy up to that cap. */
    public static long finishTick(long stored, long capacity, long ratedInput) {
        if (capacity <= 0) return BasicMachineEnergy.drain(stored, ratedInput);
        return Math.min(Math.max(0, stored), capacity);
    }

    /** The buffer that accepts energy. Capacity 0 still uses the rated maximum. */
    public static long buffer(long capacity, long ratedMaximum) {
        return capacity > 0 ? capacity : Math.max(0, ratedMaximum);
    }

    /**
     * Reads {@code gt.capacity}. A missing key uses the declaration.
     * A negative saved value is rejected.
     */
    public static long readCapacity(boolean present, long saved, long declared) {
        if (!present) return declared;
        if (saved < 0) throw new IllegalArgumentException("gt.capacity " + saved);
        return saved;
    }

    /** Rejects an emit that the declaration did not allow. Does not clamp. */
    public static void requireEmit(long size, long amount, long maxSize, long maxAmount) {
        if (maxSize < 1 || maxAmount < 1)
            throw new IllegalStateException("This machine declares no energy output");
        if (size < 1 || amount < 1 || size > maxSize || amount > maxAmount)
            throw new IllegalArgumentException("Energy output " + size + " x " + amount
                    + " exceeds " + maxSize + " x " + maxAmount);
    }
}
