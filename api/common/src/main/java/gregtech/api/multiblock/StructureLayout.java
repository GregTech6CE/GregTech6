/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A multiblock layout bound by name. The name equals the controller {@code machineType}.
 * Layouts are not inlined into a machine declaration.
 */
public final class StructureLayout {
    private final String name;
    private final List<LayoutCell> cells;
    private final List<LayoutRepeat> repeats;

    private StructureLayout(String name, List<LayoutCell> cells, List<LayoutRepeat> repeats) {
        this.name = name;
        this.cells = cells;
        this.repeats = repeats;
    }

    public String name() { return name; }

    /** Fixed cells. Repeat groups live in {@link #repeats()}. */
    public List<LayoutCell> cells() { return List.copyOf(cells); }

    /** Repeatable layer groups. Empty when the layout does not repeat. */
    public List<LayoutRepeat> repeats() { return List.copyOf(repeats); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StructureLayout that)) return false;
        return name.equals(that.name) && cells.equals(that.cells) && repeats.equals(that.repeats);
    }

    @Override public String toString() {
        return "StructureLayout[name=" + name + ", cells=" + cells.size()
                + ", repeats=" + repeats.size() + "]";
    }

    @Override public int hashCode() { return Objects.hash(name, cells, repeats); }

    public static Builder builder(String name) { return new Builder(name); }

    public static final class Builder {
        private final String name;
        private final List<LayoutCell> cells = new ArrayList<>();
        private final List<LayoutRepeat> repeats = new ArrayList<>();

        private Builder(String name) { this.name = name; }

        public Builder cell(LayoutCell cell) {
            if (cell == null) throw new IllegalArgumentException("Missing structure cell");
            cells.add(cell);
            return this;
        }

        public Builder cell(int right, int up, int back, CellMatch match, StructureRole role) {
            return cell(LayoutCell.at(right, up, back, match, role));
        }

        public Builder cell(int right, int up, int back, CellMatch match, StructureRole role, int design) {
            return cell(LayoutCell.at(right, up, back, match, role).withDesign(design));
        }

        /** Adds a group of layers that repeats as a whole. */
        public Builder repeat(LayoutRepeat repeat) {
            if (repeat == null) throw new IllegalArgumentException("Missing structure repeat");
            repeats.add(repeat);
            return this;
        }

        public StructureLayout build() {
            if (!AddonNames.matches(AddonNames.MACHINE_TYPE, name))
                throw new IllegalArgumentException("Invalid layout name: " + name);
            if (cells.isEmpty() && repeats.isEmpty())
                throw new IllegalArgumentException("Layout '" + name + "' is empty");
            var seen = new HashSet<LayoutPosition>();
            for (LayoutCell cell : cells) {
                if (!seen.add(new LayoutPosition(cell.right(), cell.up(), cell.back())))
                    throw new IllegalArgumentException("Duplicate structure cell at "
                            + cell.right() + "/" + cell.up() + "/" + cell.back());
            }
            var keys = new HashSet<String>();
            for (LayoutRepeat repeat : repeats) {
                if (!keys.add(repeat.key()))
                    throw new IllegalArgumentException("Duplicate structure repeat key: " + repeat.key());
                // Every copy a formed structure can contain. Checking the first copy alone misses a
                // later required layer, and misses two groups whose first copies do not touch.
                int copies = repeat.unbounded() ? LayoutRepeat.MAX_COPIES : repeat.max();
                List<LayoutCell> group = repeat.cells();
                for (int copy = 0; copy < copies; copy++) {
                    for (LayoutCell cell : group) {
                        int absoluteUp = absoluteLayer(repeat, cell, copy);
                        if (cell.right() == 0 && absoluteUp == 0 && cell.back() == 0)
                            throw new IllegalArgumentException("Structure repeat '" + repeat.key()
                                    + "' copy " + copy + " cell cannot be the origin");
                        if (!seen.add(new LayoutPosition(cell.right(), absoluteUp, cell.back())))
                            throw new IllegalArgumentException("Structure repeat '" + repeat.key()
                                    + "' copy " + copy + " overlaps another cell");
                    }
                }
            }
            return new StructureLayout(name, List.copyOf(cells), List.copyOf(repeats));
        }
    }

    /** Absolute layer of {@code cell} in {@code copy}, rejecting a coordinate that does not fit in an int. */
    private static int absoluteLayer(LayoutRepeat repeat, LayoutCell cell, int copy) {
        try {
            return repeat.absoluteUp(cell, copy);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Structure repeat '" + repeat.key()
                    + "' copy " + copy + " is outside the coordinate range", overflow);
        }
    }
}
