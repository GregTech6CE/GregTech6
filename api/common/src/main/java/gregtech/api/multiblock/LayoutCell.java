/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

/**
 * One structure cell. {@link #at} rejects the origin; {@link #relative} allows it for repeat
 * anchors, and {@link StructureLayout} rejects an absolute origin. Coordinates are not limited to ±16.
 *
 * @param right offset to the right
 * @param up offset upward
 * @param back offset backward
 * @param match what the cell matches
 * @param role the structure role
 * @param design the design index, or -1 when this cell does not write one
 */
public record LayoutCell(int right, int up, int back, CellMatch match, StructureRole role, int design) {
    public static final int MAX_DESIGN = 255;

    public LayoutCell {
        if (match == null) throw new IllegalArgumentException("Missing structure part");
        if (role == null) throw new IllegalArgumentException("Missing structure role");
        if (isPlainBlock(match) && role != StructureRole.CASING)
            throw new IllegalArgumentException("Plain block '" + match + "' requires the casing role");
    }

    /**
     * Writes {@code gt.design} when the structure forms and restores the previous value when it breaks.
     * Only a part whose original design count includes this index accepts it. That check runs when the layout is installed.
     */
    public LayoutCell withDesign(int design) {
        if (design < 0 || design > MAX_DESIGN)
            throw new IllegalArgumentException("Structure design must be 0 to " + MAX_DESIGN + ": " + design);
        return new LayoutCell(right, up, back, match, role, design);
    }

    public static LayoutCell at(int right, int up, int back, CellMatch match, StructureRole role) {
        if (match == null) throw new IllegalArgumentException("Missing structure part");
        if (role == null) throw new IllegalArgumentException("Missing structure role");
        if (right == 0 && up == 0 && back == 0)
            throw new IllegalArgumentException("Structure cell cannot be the origin");
        return new LayoutCell(right, up, back, match, role, -1);
    }

    /** A cell whose coordinates are relative to a repeat anchor. The origin check runs on the absolute position. */
    public static LayoutCell relative(int right, int up, int back, CellMatch match, StructureRole role) {
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
}
