/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

/**
 * How a basic machine declares the energy per tick it accepts. The three states are distinct:
 * an explicit range, no declaration (the bounds follow the nominal input), and an explicit
 * "record as undeclared" used by large recipe machines.
 */
public sealed interface EnergyRange {
    /** Explicit GT6 {@code NBT_INPUT_MIN} / {@code NBT_INPUT_MAX} bounds. */
    record Declared(long min, long max) implements EnergyRange {
        public Declared {
            if (min < 1 || max < min)
                throw new IllegalArgumentException("Invalid energy range: " + min + ".." + max);
        }
    }

    /** No explicit declaration: half to double the nominal input, or 1..16 for time-based energy. */
    enum Defaulted implements EnergyRange { INSTANCE }

    /** Recorded as {@code 0..0} so a large recipe machine stays undeclared. */
    enum Omitted implements EnergyRange { INSTANCE }

    static EnergyRange declared(long min, long max) { return new Declared(min, max); }

    static EnergyRange defaulted() { return Defaulted.INSTANCE; }

    static EnergyRange omitted() { return Omitted.INSTANCE; }
}
