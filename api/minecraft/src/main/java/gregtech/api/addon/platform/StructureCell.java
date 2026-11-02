/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.platform;

import gregtech.api.multiblock.StructureRole;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One cell from the last successful structure evaluation. Includes ordinary casings, plain blocks,
 * air and repeat copies, but excludes the controller and ignored locations. This is an immutable
 * snapshot, not permission to modify or claim another machine's parts. State properties may change
 * without a new structural evaluation; query the world when current non-structural state matters.
 */
public record StructureCell(BlockPos pos, StructureRole role, BlockState state) {
    public StructureCell {
        pos = Objects.requireNonNull(pos, "pos").immutable();
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(state, "state");
    }
}
