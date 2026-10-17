package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.recipe.RecipeMapSpec;

import java.util.List;

/**
 * Machine entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/com.gregtech.gregtech.api.addon.GregTechMachineContributor}.
 * GregTech loads it after materials are initialized and before machine blocks are registered.
 * Return pure data. Do not register blocks, and do not touch {@code Materials} from the constructor.
 */
public interface GregTechMachineContributor {
    /** The addon's own loader mod id, not {@code gregtech} or {@code gregtech6}. */
    String addonId();

    default List<RecipeMapSpec> recipeMaps() { return List.of(); }

    default List<AddonMachineDefinition> machines() { return List.of(); }

    default List<AddonMachineLayout> layouts() { return List.of(); }
}
