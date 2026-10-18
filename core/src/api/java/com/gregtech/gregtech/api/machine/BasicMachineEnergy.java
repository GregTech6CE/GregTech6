package com.gregtech.gregtech.api.machine;

/** Shared bounded per-tick machine energy math; platform adapters retain saved state and sides. */
public final class BasicMachineEnergy {
    private BasicMachineEnergy() {}

    /** GT6 rounds demand up to whole packets; the inherited machine buffer clips excess units. */
    public static long demanded(long stored, long capacity, long size) {
        if (capacity <= 0 || size == 0 || size == Long.MIN_VALUE) return 0;
        long space = capacity - Math.min(capacity, Math.max(0, stored));
        long magnitude = Math.abs(size);
        return space / magnitude + (space % magnitude == 0 ? 0 : 1);
    }

    public static long accepted(long stored, long capacity, long size, long requested) {
        return requested <= 0 ? 0 : Math.min(requested, demanded(stored, capacity, size));
    }

    /** Add accepted packets without multiplying until the product is known to fit. */
    public static long add(long stored, long capacity, long size, long packets) {
        long current = Math.min(Math.max(0, capacity), Math.max(0, stored));
        if (capacity <= 0 || size == 0 || size == Long.MIN_VALUE || packets <= 0) return current;
        long demand = demanded(current, capacity, size);
        if (packets >= demand) return capacity;
        return current + Math.abs(size) * packets;
    }

    /** The original machine drains its maximum rated input each tick, including unused power. */
    public static long drain(long stored, long ratedMaximum) {
        long current = Math.max(0, stored);
        return current - Math.min(current, Math.max(0, ratedMaximum));
    }
}
