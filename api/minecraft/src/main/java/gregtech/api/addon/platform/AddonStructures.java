/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.addon.platform;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Matching notifications and on-demand diagnostics shared by native and addon multiblocks. */
public final class AddonStructures {
    private AddonStructures() {}

    /**
     * Inspects a controller, or the controller owning a claimed part, on the logical server thread.
     * Never loads chunks, forces formation or claims parts. Returns immutable, detached details;
     * an empty list means the current definition has no detected failures. An unloaded target yields
     * {@link StructureProblem.Unloaded}; a non-controller or client world yields
     * {@link StructureProblem.Unavailable}. This is a full scan intended for tools, not every tick.
     * Older hosts throw {@link UnsupportedOperationException} rather than report false success.
     */
    public static java.util.List<StructureProblem> diagnose(Level level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        if (level.isClientSide) return java.util.List.of(new StructureProblem.Unavailable(pos));
        return java.util.List.copyOf(AddonPlatform.services().diagnoseStructure(level, pos.immutable()));
    }

    /** Localizable text used by the host magnifier; callers may also render the typed fields themselves. */
    public static net.minecraft.network.chat.Component describe(StructureProblem problem) {
        return AddonPlatform.services().describeStructureProblem(Objects.requireNonNull(problem, "problem"));
    }

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
