package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.recipe.RecipeMapSpec;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link AddonMachineContributor}. Removal is 0.8.
 */
@Deprecated(since = "0.4", forRemoval = true)
@AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.GregTechMachineContributor", removeAt = "0.8")
public interface GregTechMachineContributor {
    /** The addon's own loader mod id, not {@code gregtech} or {@code gregtech6}. */
    String addonId();

    default List<RecipeMapSpec> recipeMaps() { return List.of(); }

    default List<AddonMachineDefinition> machines() { return List.of(); }

    default List<AddonMachineLayout> layouts() { return List.of(); }
}
