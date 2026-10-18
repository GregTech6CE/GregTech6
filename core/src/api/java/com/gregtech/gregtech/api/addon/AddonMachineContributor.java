package com.gregtech.gregtech.api.addon;

import java.util.List;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * Machine entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor}.
 * GregTech loads it after materials are initialized and before machine blocks are registered.
 * Return pure data. Do not register blocks.
 */
public interface AddonMachineContributor {
    /** The addon's own loader mod id, not {@code gregtech}, {@code gregtech6} or {@code minecraft}. */
    String addonId();

    default List<RecipeMapDeclaration> recipeMaps() { return List.of(); }

    default List<MachineDeclaration> machines() { return List.of(); }

    default List<MachineLayout> layouts() { return List.of(); }

    /** Behavior machines. Pair each machine type with a platform {@code BehaviorMachineContributor}. */
    default List<BehaviorMachineDeclaration> behaviorMachines() { return List.of(); }

    /** Fuel maps. These are not machine recipe maps. */
    default List<FuelMapDeclaration> fuelMaps() { return List.of(); }

    /** Fuel generators. A multiblock names its layout through {@code machineType}. */
    default List<FuelGeneratorDeclaration> fuelGenerators() { return List.of(); }
}
