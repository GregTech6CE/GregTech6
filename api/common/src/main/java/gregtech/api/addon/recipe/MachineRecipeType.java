/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

/** Datapack recipe type IDs for machine recipe documents. */
public final class MachineRecipeType {
    public static final String NAMESPACE = "gt6ce";
    /** Adds or replaces rows in an existing machine recipe map. */
    public static final String ADDITION = NAMESPACE + ":machine";
    /** Removes matching rows from an existing machine recipe map. */
    public static final String REMOVAL = NAMESPACE + ":machine_removal";

    private MachineRecipeType() {}
}
