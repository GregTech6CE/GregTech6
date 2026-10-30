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
     * True when no bound part is a condition, or every bound condition part is satisfied.
     * An unformed structure has no bound parts, so this is true. The behavior tick still runs.
     */
    boolean conditionsMet();
}
