/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import java.util.Objects;

/** One structure cell. The origin is rejected. Coordinates are not limited to ±16. */
public final class LayoutCell {
    private final int right;
    private final int up;
    private final int back;
    private final CellMatch match;
    private final StructureRole role;
    /** {@code -1} leaves the part's design index untouched. */
    private final int design;

    private LayoutCell(int right, int up, int back, CellMatch match, StructureRole role, int design) {
        this.right = right;
        this.up = up;
        this.back = back;
        this.match = match;
        this.role = role;
        this.design = design;
    }

    public int right() { return right; }
    public int up() { return up; }
    public int back() { return back; }
    public CellMatch match() { return match; }
    public StructureRole role() { return role; }

    /** {@code -1} when this cell does not write a design index. */
    public int design() { return design; }

    /**
     * Writes {@code gt.design} when the structure forms and restores the previous value when it breaks.
     * Only a part whose original design count includes this index accepts it. That check runs when the layout is installed.
     */
    public LayoutCell withDesign(int design) {
        if (design < 0 || design > 255)
            throw new IllegalArgumentException("Structure design must be 0 to 255: " + design);
        return new LayoutCell(right, up, back, match, role, design);
    }

    public static LayoutCell at(int right, int up, int back, CellMatch match, StructureRole role) {
        if (match == null) throw new IllegalArgumentException("Missing structure part");
        if (role == null) throw new IllegalArgumentException("Missing structure role");
        if (right == 0 && up == 0 && back == 0)
            throw new IllegalArgumentException("Structure cell cannot be the origin");
        return relative(right, up, back, match, role);
    }

    /** A cell whose coordinates are relative to a repeat anchor. The origin check runs on the absolute position. */
    public static LayoutCell relative(int right, int up, int back, CellMatch match, StructureRole role) {
        if (match == null) throw new IllegalArgumentException("Missing structure part");
        if (role == null) throw new IllegalArgumentException("Missing structure role");
        if (isPlainBlock(match) && role != StructureRole.CASING)
            throw new IllegalArgumentException("Plain block '" + match + "' requires the casing role");
        return new LayoutCell(right, up, back, match, role, -1);
    }

    /** A block id, block tag, or one of the block predicates. A group is plain when any member is. */
    public static boolean isPlainBlock(CellMatch match) {
        if (match instanceof CellMatch.OneOf one) {
            for (CellMatch member : one.members()) if (isPlainBlock(member)) return true;
            return false;
        }
        return match instanceof CellMatch.Block || match instanceof CellMatch.Tag
                || match instanceof CellMatch.AnyFluid || match instanceof CellMatch.AnyOpaque;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LayoutCell that)) return false;
        return right == that.right && up == that.up && back == that.back && design == that.design
                && match.equals(that.match) && role == that.role;
    }

    @Override public String toString() {
        return "LayoutCell[at=" + right + "/" + up + "/" + back + ", match=" + match
                + ", role=" + role + ", design=" + design + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(right, up, back, match, role, design);
    }
}
