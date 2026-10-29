/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

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

    /** Mask of {@code faces}; a null list is empty. */
    public static int mask(@Nullable List<MachineFace> faces) {
        int mask = 0;
        if (faces != null) for (MachineFace face : faces) mask |= face.bit();
        return mask;
    }

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

    /** Null entries stay; this does not reject repeats. {@link #distinct} does. */
    public static List<MachineFace> listed(MachineFace... faces) {
        var list = new ArrayList<MachineFace>();
        if (faces != null) for (MachineFace face : faces) list.add(face);
        return list;
    }

    /** Same faces, ignoring order. */
    public static boolean sameSet(@Nullable List<MachineFace> left, @Nullable List<MachineFace> right) {
        if (left == right) return true;
        if (left == null || right == null || left.size() != right.size()) return false;
        return new HashSet<>(left).equals(new HashSet<>(right));
    }

    /**
     * Order-independent hash for {@link #sameSet}. Repeated faces count once, matching the set
     * comparison; length still takes part in {@code sameSet}.
     */
    public static int hashSet(@Nullable List<MachineFace> faces) {
        return faces == null ? 0 : new HashSet<>(faces).hashCode();
    }
}
