package com.gregtech.gregtech.api.energy;

/** Arithmetic for signed rotation packets and finite energy buffers. */
public final class EnergyPackets {
    private EnergyPackets() {}
    public static long magnitude(long size, boolean signed) {
        if (size == Long.MIN_VALUE) return 0;
        return signed ? Math.abs(size) : Math.max(0, size);
    }
    public static long fitting(long requested, long space, long magnitude) {
        return requested <= 0 || space <= 0 || magnitude <= 0 ? 0 : Math.min(requested, space / magnitude);
    }
}
