/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A machine-relative face. The declaration order is the GT6 side index
 * (bottom 0, top 1, right 2, front 3, left 4, back 5), exposed through {@link #index()}.
 * Use {@link BlockFace} for an absolute direction.
 */
public enum MachineFace {
    BOTTOM(0), TOP(1), RIGHT(2), FRONT(3), LEFT(4), BACK(5);

    private static final List<MachineFace> ALL = List.of(values());
    /** Behavior-machine default input order: every face except the front. */
    private static final List<MachineFace> EXCEPT_FRONT = List.of(BOTTOM, TOP, LEFT, RIGHT, BACK);

    private final int index;

    MachineFace(int index) { this.index = index; }

    public int index() { return index; }

    /** All six faces, in {@link #index()} order. */
    public static List<MachineFace> all() { return ALL; }

    /** Bottom, top, left, right, back. This is the behavior-machine input default. */
    public static List<MachineFace> exceptFront() { return EXCEPT_FRONT; }

    /** Copies {@code faces}, rejecting a null entry or a repeated face. */
    public static List<MachineFace> distinct(@Nullable List<MachineFace> faces, String label) {
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

    /**
     * Copies {@code faces} in {@link #index()} order, rejecting a null entry or a repeated face.
     * Order-independent equality can then use the stored list directly.
     */
    public static List<MachineFace> canonical(@Nullable List<MachineFace> faces, String label) {
        var copy = new ArrayList<>(distinct(faces, label));
        copy.sort(Comparator.comparingInt(MachineFace::index));
        return List.copyOf(copy);
    }

    /** Null entries stay; this does not reject repeats. {@link #distinct} does. */
    public static List<MachineFace> listed(MachineFace... faces) {
        var list = new ArrayList<MachineFace>();
        if (faces != null) for (MachineFace face : faces) list.add(face);
        return list;
    }
}
