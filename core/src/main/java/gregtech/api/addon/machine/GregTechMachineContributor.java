package gregtech.api.addon.machine;




import gregtech.api.addon.*;

import gregtech.api.recipe.RecipeMapSpec;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link AddonMachineContributor}. Removal is 0.10.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "gregtech.api.addon.machine.GregTechMachineContributor", removeAt = "0.10")
public interface GregTechMachineContributor {
    /** The addon's own loader mod id, not {@code gt6ce} or {@code gregtech6}. */
    String addonId();

    default List<RecipeMapSpec> recipeMaps() { return List.of(); }

    default List<AddonMachineDefinition> machines() { return List.of(); }

    default List<AddonMachineLayout> layouts() { return List.of(); }
}
