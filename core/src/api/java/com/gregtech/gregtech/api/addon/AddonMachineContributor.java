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

    /**
     * @deprecated Use {@link #recipeTables()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#recipeMaps", removeAt = "0.2")
    default List<RecipeMapDeclaration> recipeMaps() { return List.of(); }

    /** Machine recipe tables and fuel tables. A fuel table is not a machine table. */
    default List<RecipeTable> recipeTables() { return List.of(); }

    /**
     * @deprecated Use {@link #addonMachines()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#machines", removeAt = "0.2")
    default List<MachineDeclaration> machines() { return List.of(); }

    /**
     * Recipe machines, behavior machines and fuel generators.
     * Names, ids and signatures are checked together with {@link #machines()},
     * {@link #behaviorMachines()} and {@link #fuelGenerators()}.
     */
    default List<AddonMachine> addonMachines() { return List.of(); }

    /**
     * @deprecated Use {@link #structureLayouts()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#layouts", removeAt = "0.2")
    default List<MachineLayout> layouts() { return List.of(); }

    /** Layouts bound by the controller {@code machineType}. */
    default List<StructureLayout> structureLayouts() { return List.of(); }

    /**
     * Structure casings and energy parts. Names are global and may be used from {@link MachineLayout}.
     * A part with no energy role is a casing. {@link StructurePartEnergy} makes it an energy part.
     *
     * @deprecated Use {@link #layoutParts()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#parts", removeAt = "0.2")
    default List<StructurePartDeclaration> parts() { return List.of(); }

    /**
     * Casings, energy parts and functional parts. Names are checked together with {@link #parts()}.
     * A functional part also needs a {@code LayoutPartContributor} for its name.
     */
    default List<LayoutPart> layoutParts() { return List.of(); }

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

    /**
     * Behavior machines. Pair each machine type with a platform {@code BehaviorMachineContributor}.
     *
     * @deprecated Use {@link #addonMachines()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#behaviorMachines", removeAt = "0.2")
    default List<BehaviorMachineDeclaration> behaviorMachines() { return List.of(); }

    /**
     * Fuel maps. These are not machine recipe maps.
     *
     * @deprecated Use {@link #recipeTables()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#fuelMaps", removeAt = "0.2")
    default List<FuelMapDeclaration> fuelMaps() { return List.of(); }

    /**
     * Fuel generators. A multiblock names its layout through {@code machineType}.
     *
     * @deprecated Use {@link #addonMachines()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineContributor#fuelGenerators", removeAt = "0.2")
    default List<FuelGeneratorDeclaration> fuelGenerators() { return List.of(); }

    /**
     * Compressed gas drums. Ordinary fluid insertion is closed.
     * A behavior fills one through {@code MachineBehaviorContext#pushCompressedGas}.
     * Fuel machines and pipes drain it.
     */
    default List<CompressedGasTankDeclaration> compressedGasTanks() { return List.of(); }
}
