package com.gregtech.gregtech.api.addon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * A multiblock layout bound by name. The name equals the controller {@code machineType}.
 * Layouts are not inlined into a machine declaration.
 */
public final class StructureLayout {
    private final String name;
    private final List<LayoutCell> cells;

    private StructureLayout(String name, List<LayoutCell> cells) {
        this.name = name;
        this.cells = cells;
    }

    public String name() { return name; }

    public List<LayoutCell> cells() { return List.copyOf(cells); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StructureLayout that)) return false;
        return name.equals(that.name) && cells.equals(that.cells);
    }

    @Override public int hashCode() { return Objects.hash(name, cells); }

    public static Builder builder(String name) { return new Builder(name); }

    public static final class Builder {
        private final String name;
        private final List<LayoutCell> cells = new ArrayList<>();

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

        public StructureLayout build() {
            if (name == null || !name.matches("[a-z0-9_]+"))
                throw new IllegalArgumentException("Invalid layout name: " + name);
            if (cells.isEmpty()) throw new IllegalArgumentException("Layout '" + name + "' is empty");
            var seen = new HashSet<Position>();
            for (LayoutCell cell : cells) {
                if (!seen.add(new Position(cell.right(), cell.up(), cell.back())))
                    throw new IllegalArgumentException("Duplicate structure cell");
            }
            return new StructureLayout(name, List.copyOf(cells));
        }
    }

    private record Position(int right, int up, int back) {}
}
