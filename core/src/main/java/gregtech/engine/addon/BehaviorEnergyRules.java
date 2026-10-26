/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.engine.addon;

import gregtech.engine.machine.BasicMachineEnergy;

/**
 * Internal: not part of the addon contract; may change without notice.
 * Tick-end storage and declared output limits for a behavior machine.
 */
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
     * How many packets the main input of a behavior machine takes.
     * {@code anyPacket} skips the window. {@code ranged} refuses a packet below {@code min}.
     * The default window still stores an undersized packet while {@code overload} is true.
     * A packet above {@code max} overloads when {@code overload} is true, and is refused when it is false.
     */
    public static PartEnergyRules.Admission admitInput(long stored, long capacity, long size, long amount,
                                                        long min, long max, boolean anyPacket, boolean overload,
                                                        boolean ranged) {
        if (anyPacket)
            return PartEnergyRules.admit(stored, capacity, size, amount, 1, Long.MAX_VALUE, true);
        if (size == 0 || size == Long.MIN_VALUE || amount <= 0 || capacity < 1)
            return new PartEnergyRules.Admission(0, false);
        long sizeAbs = Math.abs(size);
        if (sizeAbs > max)
            return overload ? new PartEnergyRules.Admission(amount, true) : new PartEnergyRules.Admission(0, false);
        if ((ranged || !overload) && sizeAbs < min)
            return new PartEnergyRules.Admission(0, false);
        return new PartEnergyRules.Admission(BasicMachineEnergy.accepted(stored, capacity, size, amount), false);
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

    /** Rejects an emit that the declaration did not allow. The minimum is 1 and the sign is rejected. */
    public static void requireEmit(long size, long amount, long maxSize, long maxAmount) {
        requireEmit(size, amount, 1, maxSize, maxAmount, false);
    }

    /**
     * Rejects an emit outside {@code minSize}..{@code maxSize}. Does not clamp.
     * When {@code allowNegative} is set, the size is checked by its absolute value.
     * {@link Long#MIN_VALUE} is rejected because it has no absolute value.
     */
    public static void requireEmit(long size, long amount, long minSize, long maxSize, long maxAmount,
                                   boolean allowNegative) {
        if (maxSize < 1 || maxAmount < 1)
            throw new IllegalStateException("This machine declares no energy output");
        if (minSize < 1 || minSize > maxSize)
            throw new IllegalStateException("Energy output minimum is outside its maximum");
        long magnitude = size;
        if (allowNegative) {
            if (size == Long.MIN_VALUE)
                throw new IllegalArgumentException("Energy output " + size + " x " + amount
                        + " exceeds " + minSize + ".." + maxSize + " x " + maxAmount);
            magnitude = Math.abs(size);
        }
        if (magnitude < minSize || magnitude > maxSize || amount < 1 || amount > maxAmount)
            throw new IllegalArgumentException("Energy output " + size + " x " + amount
                    + " exceeds " + minSize + ".." + maxSize + " x " + maxAmount);
    }
}
