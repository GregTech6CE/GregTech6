package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** Programmed circuit numbers. 0 through 24 are {@code integrated_circuit_0} through {@code integrated_circuit_24}. */
public final class SelectorTags {
    public static final int MIN = 0;
    public static final int MAX = 24;

    private SelectorTags() {}

    public static int require(int n) {
        if (n < MIN || n > MAX)
            throw new IllegalArgumentException("Selector tag " + n + " is outside " + MIN + ".." + MAX);
        return n;
    }
}
