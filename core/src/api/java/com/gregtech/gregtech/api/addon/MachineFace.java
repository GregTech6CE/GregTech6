package com.gregtech.gregtech.api.addon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Machine-relative face. {@link #index()} and {@link #bit()} are the mask positions.
 */
public enum MachineFace {
    BOTTOM(0), TOP(1), RIGHT(2), FRONT(3), LEFT(4), BACK(5);

    private static final List<MachineFace> ALL = List.of(values());
    /** Behavior-machine default input order: every face except the front. */
    private static final List<MachineFace> EXCEPT_FRONT = List.of(BOTTOM, TOP, LEFT, RIGHT, BACK);

    private final int index;

    MachineFace(int index) { this.index = index; }

    public int index() { return index; }

    public int bit() { return 1 << index; }

    /** All six faces, in {@link #index()} order. */
    public static List<MachineFace> all() { return ALL; }

    /** Bottom, top, left, right, back. This is the behavior-machine input default. */
    public static List<MachineFace> exceptFront() { return EXCEPT_FRONT; }

    /** Copies {@code faces}, rejecting a null entry or a repeated face. */
    public static List<MachineFace> distinct(List<MachineFace> faces, String label) {
        var copy = new ArrayList<MachineFace>();
        var seen = new HashSet<MachineFace>();
        if (faces != null) {
            for (MachineFace face : faces) {
                if (face == null) throw new IllegalArgumentException("Missing " + label);
                if (!seen.add(face)) throw new IllegalArgumentException("Repeated " + label);
                copy.add(face);
            }
        }
        return List.copyOf(copy);
    }
}
