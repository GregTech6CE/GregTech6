/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A group of layers that repeats as a whole. The key names the group for
 * {@code MachineBehaviorContext.structureRepeats(key)}. {@code up} is the anchor layer;
 * a cell's {@code up} is its distance from the anchor along {@code step}. {@code step} is
 * {@code 1} upward or {@code -1} downward. {@code max} {@code -1} repeats until a layer
 * fails to match, bounded by {@link #MAX_COPIES}.
 */
public final class LayoutRepeat {
    /** Largest finite {@code max}. An unbounded group is overlap-checked for this many copies. */
    public static final int MAX_COPIES = 1024;

    private final String key;
    private final int up;
    private final int min;
    private final int max;
    private final int step;
    private final List<LayoutCell> cells;
    private final int height;

    private LayoutRepeat(String key, int up, int min, int max, int step, List<LayoutCell> cells) {
        this.key = key;
        this.up = up;
        this.min = min;
        this.max = max;
        this.step = step;
        this.cells = cells;
        this.height = cells.stream().mapToInt(LayoutCell::up).max().orElse(-1) + 1;
    }

    public String key() { return key; }

    /** Absolute layer of the first copy. */
    public int up() { return up; }

    public int min() { return min; }

    /** {@code -1} when the group repeats until a layer fails. */
    public int max() { return max; }

    public boolean unbounded() { return max < 0; }

    /** {@code 1} upward, {@code -1} downward. */
    public int step() { return step; }

    /** Highest relative {@code up} plus one. One copy advances this many layers. */
    public int height() { return height; }

    public List<LayoutCell> cells() { return List.copyOf(cells); }

    /** Absolute {@code up} of {@code cell} in copy {@code copy}, rejecting a coordinate that does not fit in an int. */
    public int absoluteUp(LayoutCell cell, int copy) {
        return Math.addExact(up, Math.multiplyExact(step,
                Math.addExact(cell.up(), Math.multiplyExact(copy, height))));
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LayoutRepeat that)) return false;
        return up == that.up && min == that.min && max == that.max && step == that.step
                && key.equals(that.key) && cells.equals(that.cells);
    }

    @Override public String toString() {
        return "LayoutRepeat[key=" + key + ", up=" + up + ", min=" + min + ", max=" + max
                + ", step=" + step + ", height=" + height + ", cells=" + cells.size() + "]";
    }

    @Override public int hashCode() { return Objects.hash(key, up, min, max, step, cells); }

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
            if (!AddonNames.matches(AddonNames.LOWER_PATH, key))
                throw new IllegalArgumentException("Structure repeat key is invalid: " + key);
            if (min < 1)
                throw new IllegalArgumentException("Structure repeat '" + key + "' needs min >= 1");
            if (max != -1 && max < min)
                throw new IllegalArgumentException("Structure repeat '" + key + "' needs max >= min or -1");
            if (max > MAX_COPIES)
                throw new IllegalArgumentException("Structure repeat '" + key + "' max is too large: " + max);
            if (cells.isEmpty())
                throw new IllegalArgumentException("Structure repeat '" + key + "' is empty");
            var seen = new HashSet<Position>();
            for (LayoutCell cell : cells) {
                if (!seen.add(new Position(cell.right(), cell.up(), cell.back())))
                    throw new IllegalArgumentException("Duplicate cell in structure repeat '" + key + "'");
            }
            return new LayoutRepeat(key, up, min, max, step, List.copyOf(cells));
        }
    }

    private record Position(int right, int up, int back) {}
}
