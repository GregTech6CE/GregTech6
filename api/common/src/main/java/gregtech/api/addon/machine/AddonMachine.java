/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.machine.MachineCasing;

/** A recipe machine, a behavior machine, or a fuel generator. */
public interface AddonMachine {
    MachineCasing casing();

    /** Bind a required structure explicitly, for recipe, behavior and fuel controllers alike. */
    default MultiblockMachine withStructure(gregtech.api.multiblock.StructureLayout structure) {
        return new MultiblockMachine(this, structure);
    }
}
