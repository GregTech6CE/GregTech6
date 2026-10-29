/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.signal;

/**
 * A block entity that stores GT6 fractional redstone.
 * One vanilla redstone level equals {@link #UNIT}.
 */
public interface GtSignalCarrier {
    /** One vanilla redstone level. {@code 15 * UNIT} still fits in a signed long. */
    long UNIT = Integer.MAX_VALUE;

    /** Fractional strength currently on this carrier, in {@link #UNIT} units. */
    long signal();

    /**
     * Clamps an emitted strength into {@code [0, 15 * UNIT]}.
     * Negative values become 0. Values above fifteen redstone levels become {@code 15 * UNIT}.
     */
    static long clamp(long emitted) {
        if (emitted <= 0) return 0;
        long max = 15L * UNIT;
        return emitted > max ? max : emitted;
    }
}
