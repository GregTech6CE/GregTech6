/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The placed machine a behavior reads and writes. Valid during the callbacks on {@link MachineBehavior}.
 * Each accessor returns a view for one concern; the context and its views are valid only for the
 * same callback and must not be retained.
 */
public interface MachineBehaviorContext {
    Level level();

    BlockPos pos();

    Direction facing();

    int tier();

    /** Inventory owned by this machine, including output-role insertion. */
    MachineInventoryAccess inventory();

    /** Tanks owned by this machine. */
    FluidAccess fluids();

    /** Energy buffers, emission and bound-part supply. */
    EnergyAccess energy();

    /** Formed-layout bindings and repeat counts. */
    StructureAccess structure();

    /** Toggles, progress and the working-face flag. */
    MachinePanel panel();

    /** Interaction with blocks and item entities outside this machine. */
    WorldAccess world();

    BehaviorState state();
}
