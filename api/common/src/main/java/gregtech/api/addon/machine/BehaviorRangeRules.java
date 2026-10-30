/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import org.jetbrains.annotations.ApiStatus;

/**
 * Internal: not part of the addon contract; may change without notice.
 * Range and per-tick budget for behavior-machine world actions.
 */
@ApiStatus.Internal
public final class BehaviorRangeRules {
    /** Largest {@code maxRange} a declaration may name; larger ranges are rejected. */
    public static final int LIMIT = 32;
    /** Successful block breaks the shell allows during one server tick. */
    public static final int BREAKS_PER_TICK = 1;
    /** Item entities {@code collectItems} may take during one server tick. */
    public static final int ITEMS_PER_TICK = 16;

    private BehaviorRangeRules() {}

    /** A range of 0 means the world actions are closed. */
    public static void requireUsable(int declared) {
        if (declared < 1) throw new IllegalStateException("range is 0 (declared " + declared + ")");
    }

    /**
     * Chebyshev distance from the machine. The machine block itself is refused.
     * A declared range of 0 is refused before the distance is read.
     */
    public static void requireInside(int declared, int dx, int dy, int dz) {
        requireUsable(declared);
        if (dx == 0 && dy == 0 && dz == 0)
            throw new IllegalArgumentException("machine block at offset " + dx + "," + dy + "," + dz
                    + " (declared range " + declared + ")");
        if (!inside(declared, dx, dy, dz))
            throw new IllegalArgumentException("outside range " + declared + " at offset "
                    + dx + "," + dy + "," + dz);
    }

    /** {@code radius} is the cube half-extent and may not exceed the declaration. */
    public static void requireRadius(int declared, int radius) {
        requireUsable(declared);
        if (radius < 0 || radius > declared)
            throw new IllegalArgumentException("outside range " + declared + " with radius " + radius);
    }

    public static boolean inside(int declared, int dx, int dy, int dz) {
        if (declared < 1 || (dx == 0 && dy == 0 && dz == 0)) return false;
        int reach = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        return reach <= declared;
    }

    public static boolean radiusAllowed(int declared, int radius) {
        return declared > 0 && radius >= 0 && radius <= declared;
    }
}
