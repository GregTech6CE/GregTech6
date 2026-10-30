/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.addon.pipe.FluidPipeDeclaration;
import gregtech.api.addon.pipe.ItemPipeDeclaration;
import gregtech.api.addon.recipe.RecipeTable;
import gregtech.api.multiblock.StructureLayout;
import java.util.List;

/**
 * Machine entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/gregtech.api.addon.machine.AddonMachineContributor}.
 * GregTech loads it after materials are initialized and before pipes or machine blocks are registered.
 * Return pure data. Do not register blocks.
 */
public interface AddonMachineContributor {
    /** The addon's own loader mod id, not {@code gt6ce}, {@code gregtech6} or {@code minecraft}. */
    String addonId();

    /** Machine recipe tables and fuel tables. A fuel table is not a machine table. */
    default List<RecipeTable> recipeTables() { return List.of(); }

    /**
     * Recipe machines, behavior machines and fuel generators.
     * Pair each behavior machine type with a platform {@code BehaviorMachineContributor}.
     */
    default List<AddonMachine> addonMachines() { return List.of(); }

    /** Layouts bound by the controller {@code machineType}. */
    default List<StructureLayout> structureLayouts() { return List.of(); }

    /**
     * Casings, energy parts and functional parts.
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
     * Compressed gas drums. Ordinary fluid insertion is closed.
     * A behavior fills one through {@code MachineBehaviorContext#pushCompressedGas}.
     * Fuel machines and pipes drain it.
     */
    default List<CompressedGasTankDeclaration> compressedGasTanks() { return List.of(); }
}
