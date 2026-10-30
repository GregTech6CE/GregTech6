/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.platform;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Notifications for addon-owned data used by a multiblock's matching rules. */
public final class AddonStructures {
    private AddonStructures() {}

    /**
     * Notify after matching-relevant data at {@code pos} changes without a block-state change.
     * Call on the logical server thread. Client calls and cells without watchers are no-ops.
     * Affected formed structures stop immediately and are re-evaluated by the end-of-tick
     * scheduler. This does not load chunks or immediately scan a structure.
     *
     * @throws NullPointerException if either argument is null
     * @throws IllegalStateException if called off the server thread or before host service binding
     */
    public static void notifyChanged(Level level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        if (level.isClientSide) return;
        AddonPlatform.services().structureChanged(level, pos.immutable());
    }
}
