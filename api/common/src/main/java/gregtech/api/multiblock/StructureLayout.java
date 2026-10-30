/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A multiblock layout bound by name. The name equals the controller {@code machineType}.
 * Layouts are not inlined into a machine declaration.
 *
 * @param name the layout name
 * @param cells fixed cells
 * @param repeats repeatable layer groups
 */
public record StructureLayout(String name, List<LayoutCell> cells, List<LayoutRepeat> repeats) {
    public StructureLayout {
        if (!AddonNames.matches(AddonNames.MACHINE_TYPE, name))
            throw new IllegalArgumentException("Invalid layout name: " + name);
        if (cells == null || repeats == null)
            throw new IllegalArgumentException("Missing structure layout lists");
        cells = List.copyOf(cells);
        repeats = List.copyOf(repeats);
        if (cells.isEmpty() && repeats.isEmpty())
            throw new IllegalArgumentException("Layout '" + name + "' is empty");
        var seen = new HashSet<LayoutPosition>();
        for (LayoutCell cell : cells) {
            if (cell.right() == 0 && cell.up() == 0 && cell.back() == 0)
                throw new IllegalArgumentException("Structure cell cannot be the origin");
            if (!seen.add(new LayoutPosition(cell.right(), cell.up(), cell.back())))
                throw new IllegalArgumentException("Duplicate structure cell at "
                        + cell.right() + "/" + cell.up() + "/" + cell.back());
        }
        var keys = new HashSet<String>();
        for (LayoutRepeat repeat : repeats) {
            if (!keys.add(repeat.key()))
                throw new IllegalArgumentException("Duplicate structure repeat key: " + repeat.key());
            // A bounded group may contain every copy up to max, so check them all: the first copy
            // alone misses a later required layer and two groups whose first copies do not touch.
            // An unbounded group only guarantees its minimum; the runtime stops at the first copy
            // that fails to match, so a fixed cap above a repeating tower is valid.
            int copies = repeat.unbounded() ? repeat.min() : repeat.max();
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
    }

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
            return new StructureLayout(name, cells, repeats);
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
