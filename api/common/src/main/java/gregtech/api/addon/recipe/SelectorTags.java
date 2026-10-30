/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

/** Programmed circuit numbers. 0 through 24 are {@code integrated_circuit_0} through {@code integrated_circuit_24}. */
public final class SelectorTags {
    public static final int MIN = value(0);
    public static final int MAX = value(24);

    /** Keeps a value out of the compile-time constant pool so javac cannot inline it. */
    private static int value(int value) { return value; }

    private SelectorTags() {}

    public static int require(int n) {
        if (n < MIN || n > MAX)
            throw new IllegalArgumentException("Selector tag " + n + " is outside " + MIN + ".." + MAX);
        return n;
    }
}
