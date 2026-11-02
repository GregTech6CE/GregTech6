/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.machine;

import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.machine.MachineCasing;
import gregtech.api.multiblock.StructureLayout;
import java.util.Objects;

/**
 * A controller and its required structure, submitted together through
 * {@link AddonMachineContributor#addonMachines()}. The layout may be reused by several
 * machine types; its name need not equal the controller's machine type.
 */
public record MultiblockMachine(AddonMachine controller, StructureLayout structure) implements AddonMachine {
    public MultiblockMachine {
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(structure, "structure");
        if (controller instanceof RecipeMachine recipe) {
            if (!recipe.largeRecipe()) throw new IllegalArgumentException("A multiblock recipe controller must use largeRecipe");
        } else if (controller instanceof FuelGenerator fuel) {
            if (!fuel.multi()) throw new IllegalArgumentException("A multiblock fuel controller must use multi");
        } else if (!(controller instanceof BehaviorMachine)) {
            throw new IllegalArgumentException("Unsupported multiblock controller: " + controller.getClass().getName());
        }
    }

    @Override
    public MachineCasing casing() {
        return controller.casing();
    }
}
