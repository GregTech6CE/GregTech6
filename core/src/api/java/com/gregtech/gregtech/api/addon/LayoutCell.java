package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** One structure cell. The origin is rejected. Coordinates are not limited to ±16. */
public final class LayoutCell {
    private final int right;
    private final int up;
    private final int back;
    private final CellMatch match;
    private final StructureRole role;

    private LayoutCell(int right, int up, int back, CellMatch match, StructureRole role) {
        this.right = right;
        this.up = up;
        this.back = back;
        this.match = match;
        this.role = role;
    }

    public int right() { return right; }
    public int up() { return up; }
    public int back() { return back; }
    public CellMatch match() { return match; }
    public StructureRole role() { return role; }

    public static LayoutCell at(int right, int up, int back, CellMatch match, StructureRole role) {
        if (match == null) throw new IllegalArgumentException("Missing structure part");
        Objects.requireNonNull(role, "role");
        if (right == 0 && up == 0 && back == 0)
            throw new IllegalArgumentException("Structure cell cannot be the origin");
        if ((match instanceof CellMatch.Block || match instanceof CellMatch.Tag) && role != StructureRole.CASING)
            throw new IllegalArgumentException("Plain block '" + match + "' requires the casing role");
        return new LayoutCell(right, up, back, match, role);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LayoutCell that)) return false;
        return right == that.right && up == that.up && back == that.back
                && match.equals(that.match) && role == that.role;
    }

    @Override public int hashCode() {
        return Objects.hash(right, up, back, match, role);
    }
}
