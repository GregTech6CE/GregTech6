/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.List;

/**
 * Formed-layout bindings for one behavior machine. Valid only during the same behavior callback
 * as its context; a broken structure drops these views.
 */
public interface StructureAccess {
    /**
     * True when this machine has no layout, or the layout matched this tick.
     * A machine with a layout is false until that match succeeds.
     */
    boolean formed();

    /** Energy parts bound by the formed layout. Empty when {@link #formed()} is false. */
    List<EnergyPart> energyParts();

    /**
     * How many copies of the repeat group {@code key} matched this tick.
     * Zero when the structure is not formed, the key is unknown, or the layout has no such group.
     */
    int repeats(String key);

    /**
     * Addon parts bound by the formed layout, energy parts and functional parts together.
     * A casing is not included. Empty when {@link #formed()} is false.
     * {@link #energyParts()} is the energy view of this same binding.
     */
    List<BoundPart> parts();

    /**
     * Complete matched layout in declaration order (fixed cells, then repeat copies).
     * Empty for unformed or single-block machines. Unlike {@link #parts()}, includes ordinary
     * casings and plain world blocks. The host reuses this snapshot until it evaluates new cells.
     */
    default List<StructureCell> cells() { return List.of(); }

    /**
     * Explicit read-only scan using the host's compiled matching rules, including counts and ownership.
     * Call when requested by a player or diagnostic tool, not every tick. Empty means no problem was
     * found; it does not force formation, claim parts, or promise the world stays unchanged.
     * Older hosts throw instead of presenting an unsupported scan as success.
     */
    default List<StructureProblem> diagnose() {
        throw new UnsupportedOperationException("This host does not expose structure diagnosis");
    }

    /**
     * True when no bound part is a condition, or every bound condition part is satisfied.
     * An unformed structure has no bound parts, so this is true. The behavior tick still runs.
     */
    boolean conditionsMet();
}
