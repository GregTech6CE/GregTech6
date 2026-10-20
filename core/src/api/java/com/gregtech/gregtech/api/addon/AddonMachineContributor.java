package com.gregtech.gregtech.api.addon;

import java.util.List;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * Machine entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor}.
 * GregTech loads it after materials are initialized and before pipes or machine blocks are registered.
 * Return pure data. Do not register blocks.
 */
public interface AddonMachineContributor {
    /** The addon's own loader mod id, not {@code gt6ce}, {@code gregtech6} or {@code minecraft}. */
    String addonId();

    default List<RecipeMapDeclaration> recipeMaps() { return List.of(); }

    default List<MachineDeclaration> machines() { return List.of(); }

    default List<MachineLayout> layouts() { return List.of(); }

    /**
     * Structure casings and energy parts. Names are global and may be used from {@link MachineLayout}.
     * A part with no energy role is a casing. {@link StructurePartEnergy} makes it an energy part.
     */
    default List<StructurePartDeclaration> parts() { return List.of(); }

    /**
     * Fluid-pipe materials. Each row expands into the seven built-in sizes.
     * {@code idSuffix} must not match a built-in fluid pipe or another addon's fluid pipe.
     */
    default List<FluidPipeDeclaration> fluidPipes() { return List.of(); }

    /**
     * Item-pipe materials. Each row expands into the six built-in sizes.
     * {@code idSuffix} must not match a built-in item pipe or another addon's item pipe.
     */
    default List<ItemPipeDeclaration> itemPipes() { return List.of(); }

    /** Behavior machines. Pair each machine type with a platform {@code BehaviorMachineContributor}. */
    default List<BehaviorMachineDeclaration> behaviorMachines() { return List.of(); }

    /** Fuel maps. These are not machine recipe maps. */
    default List<FuelMapDeclaration> fuelMaps() { return List.of(); }

    /** Fuel generators. A multiblock names its layout through {@code machineType}. */
    default List<FuelGeneratorDeclaration> fuelGenerators() { return List.of(); }

    /**
     * Compressed gas drums. Ordinary fluid insertion is closed.
     * A behavior fills one through {@code MachineBehaviorContext#pushCompressedGas}.
     * Fuel machines and pipes drain it.
     */
    default List<CompressedGasTankDeclaration> compressedGasTanks() { return List.of(); }
}
