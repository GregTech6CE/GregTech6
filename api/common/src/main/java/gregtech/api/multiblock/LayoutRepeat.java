/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A group of layers that repeats as a whole. The key names the group for
 * {@code MachineBehaviorContext.structureRepeats(key)}. {@code up} is the anchor layer;
 * a cell's {@code up} is its distance from the anchor along {@code step}. {@code step} is
 * {@code 1} upward or {@code -1} downward. {@code max} {@code -1} repeats until a layer
 * fails to match, bounded by {@link #MAX_COPIES}.
 *
 * @param key the repeat group key
 * @param up absolute layer of the first copy
 * @param min minimum copies
 * @param max maximum copies, or -1 when unbounded
 * @param step 1 upward or -1 downward
 * @param cells the layers in one copy
 */
public record LayoutRepeat(String key, int up, int min, int max, int step, List<LayoutCell> cells) {
    /**
     * Largest finite {@code max} and the runtime cap for an unbounded group. Declaration-time
     * overlap checks only cover the guaranteed {@link #min()} copies of an unbounded group, because
     * the runtime stops at the first copy that fails to match.
     */
    public static final int MAX_COPIES = 1024;

    public LayoutRepeat {
        if (!AddonNames.matches(AddonNames.LOWER_PATH, key))
            throw new IllegalArgumentException("Structure repeat key is invalid: " + key);
        if (min < 1)
            throw new IllegalArgumentException("Structure repeat '" + key + "' needs min >= 1");
        if (max != -1 && max < min)
            throw new IllegalArgumentException("Structure repeat '" + key + "' needs max >= min or -1");
        if (max > MAX_COPIES)
            throw new IllegalArgumentException("Structure repeat '" + key + "' max is too large: " + max);
        if (cells == null) throw new IllegalArgumentException("Missing structure repeat cells");
        cells = List.copyOf(cells);
        if (cells.isEmpty())
            throw new IllegalArgumentException("Structure repeat '" + key + "' is empty");
        var seen = new HashSet<LayoutPosition>();
        for (LayoutCell cell : cells) {
            if (!seen.add(new LayoutPosition(cell.right(), cell.up(), cell.back())))
                throw new IllegalArgumentException("Duplicate cell in structure repeat '" + key + "'");
        }
    }

    public boolean unbounded() { return max < 0; }

    /** Highest relative {@code up} plus one. One copy advances this many layers. */
    public int height() {
        return cells.stream().mapToInt(LayoutCell::up).max().orElse(-1) + 1;
    }

    /** Absolute {@code up} of {@code cell} in copy {@code copy}, rejecting a coordinate that does not fit in an int. */
    public int absoluteUp(LayoutCell cell, int copy) {
        return Math.addExact(up, Math.multiplyExact(step,
                Math.addExact(cell.up(), Math.multiplyExact(copy, height()))));
    }

    public static Builder builder(String key, int up, int min, int max) {
        return new Builder(key, up, min, max);
    }

    public static final class Builder {
        private final String key;
        private final int up;
        private final int min;
        private final int max;
        private int step = 1;
        private final List<LayoutCell> cells = new ArrayList<>();

        private Builder(String key, int up, int min, int max) {
            this.key = key;
            this.up = up;
            this.min = min;
            this.max = max;
        }

        /** {@code 1} upward (default) or {@code -1} downward. */
        public Builder step(int value) {
            if (value != 1 && value != -1)
                throw new IllegalArgumentException("Structure repeat step must be 1 or -1: " + value);
            step = value;
            return this;
        }

        /** Coordinates are relative to the anchor and the repeat direction. */
        public Builder cell(int right, int up, int back, CellMatch match, StructureRole role) {
            return cell(LayoutCell.relative(right, up, back, match, role));
        }

        public Builder cell(int right, int up, int back, CellMatch match, StructureRole role, int design) {
            return cell(LayoutCell.relative(right, up, back, match, role).withDesign(design));
        }

        public Builder cell(LayoutCell cell) {
            if (cell == null) throw new IllegalArgumentException("Missing repeat cell");
            if (cell.up() < 0)
                throw new IllegalArgumentException("Repeat cell up must be zero or positive");
            cells.add(cell);
            return this;
        }

        public LayoutRepeat build() {
            return new LayoutRepeat(key, up, min, max, step, cells);
        }
    }
}
