/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.multiblock;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A rule over the cells actually matched in the world, including complete repeat copies.
 * Unlike {@link StructurePattern#count}, these rules count the chosen alternatives, not symbols.
 * The host evaluates them from its match cache; they must not initiate another world scan.
 */
public sealed interface StructureConstraint permits StructureConstraint.Count, StructureConstraint.SameBlock {
    Selection selection();

    /**
     * Selects accepted declaration choices, optionally restricted to a role. A tag selects cells
     * matched by that declared tag, not unrelated cells whose blocks also happen to carry the tag.
     * Null match means any choice; null role means any role. The controller is never a layout cell.
     */
    record Selection(@Nullable CellMatch match, @Nullable StructureRole role) {
        public static Selection matching(CellMatch match) {
            return new Selection(Objects.requireNonNull(match, "match"), null);
        }

        public static Selection role(StructureRole role) {
            return new Selection(null, Objects.requireNonNull(role, "role"));
        }

        public static Selection all() { return new Selection(null, null); }

        public boolean selects(CellMatch choice, StructureRole cellRole) {
            if (role != null && role != cellRole) return false;
            return match == null || choices(match).contains(choice);
        }
    }

    /** Inclusive bounds; -1 means no upper bound. */
    record Count(Selection selection, int min, int max) implements StructureConstraint {
        public Count {
            Objects.requireNonNull(selection, "selection");
            if (min < 0 || max < -1 || max != -1 && max < min)
                throw new IllegalArgumentException("Invalid matched-cell count: " + min + ".." + max);
        }
    }

    /** All selected cells must use the same block identity; block-state properties may differ. */
    record SameBlock(Selection selection) implements StructureConstraint {
        public SameBlock { Objects.requireNonNull(selection, "selection"); }
    }

    static Count count(CellMatch match, int min, int max) {
        return new Count(Selection.matching(match), min, max);
    }

    static Count exactly(CellMatch match, int count) { return count(match, count, count); }

    static SameBlock sameBlock(CellMatch match) { return new SameBlock(Selection.matching(match)); }

    private static List<CellMatch> choices(CellMatch match) {
        return match instanceof CellMatch.OneOf one ? one.members() : List.of(match);
    }

    /** Rejects unused selectors and individually impossible count bounds before registration. */
    static void validate(List<LayoutCell> cells, List<LayoutRepeat> repeats, List<StructureConstraint> rules) {
        for (StructureConstraint rule : rules) {
            Selection selection = rule.selection();
            long minimum = 0, maximum = 0;
            for (LayoutCell cell : cells) {
                minimum += required(selection, cell) ? 1 : 0;
                maximum += possible(selection, cell) ? 1 : 0;
            }
            for (LayoutRepeat repeat : repeats) {
                int maxCopies = repeat.unbounded() ? LayoutRepeat.MAX_COPIES : repeat.max();
                for (LayoutCell cell : repeat.cells()) {
                    minimum += required(selection, cell) ? repeat.min() : 0;
                    maximum += possible(selection, cell) ? maxCopies : 0;
                }
            }
            if (maximum == 0)
                throw new IllegalArgumentException("Constraint selects no declared cells: " + selection);
            if (rule instanceof Count count
                    && (count.min() > maximum || count.max() >= 0 && count.max() < minimum))
                throw new IllegalArgumentException("Matched-cell count " + count.min() + ".." + count.max()
                        + " cannot fit declared choices " + minimum + ".." + maximum + " for " + selection);
        }
    }

    private static boolean required(Selection selection, LayoutCell cell) {
        return choices(cell.match()).stream().allMatch(choice -> selection.selects(choice, cell.role()));
    }

    private static boolean possible(Selection selection, LayoutCell cell) {
        return choices(cell.match()).stream().anyMatch(choice -> selection.selects(choice, cell.role()));
    }
}
