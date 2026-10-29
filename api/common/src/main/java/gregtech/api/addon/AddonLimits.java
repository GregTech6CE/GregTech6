/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

/** Declaration limits shared by the addon types. */
public final class AddonLimits {
    /** Largest accepted energy per tick, in energy units. */
    public static final long MAX_PER_TICK = live(1_000_000_000_000L);
    /** Largest tank or fluid-buffer capacity, in millibuckets. */
    public static final int MAX_CAPACITY = live(1_024_000_000);
    /** Largest processing efficiency, in parts per 10,000. */
    public static final int MAX_EFFICIENCY = live(100_000);
    /** Default processing efficiency, in parts per 10,000 (10,000 is 100%). */
    public static final int DEFAULT_EFFICIENCY = live(10_000);

    /** Keeps a public value out of the compile-time constant pool so javac cannot inline it. */
    public static int live(int value) { return value; }

    /** {@code long} overload of {@link #live(int)}. */
    public static long live(long value) { return value; }

    private AddonLimits() {}
}
