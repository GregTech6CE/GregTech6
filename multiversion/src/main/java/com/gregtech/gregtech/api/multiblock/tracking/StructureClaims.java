package com.gregtech.gregtech.api.multiblock.tracking;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Applies one evaluation's cells as a per-cell delta: cells whose role is unchanged and whose part
 * still carries this controller's binding are left alone, so a rescan binds and writes nothing.
 */
public final class StructureClaims {
    public enum Result { APPLIED, UNCHANGED, FAILED }

    private StructureClaims() {}

    public static Result update(BlockEntity controller,
                                PartBindings<BlockPos, MultiblockLayout.Role> bindings,
                                Map<BlockPos, MultiblockLayout.Role> parts,
                                Predicate<BlockPos> canClaim, BiConsumer<BlockPos, MultiblockLayout.Role> claim,
                                Consumer<BlockPos> release) {
        StructureScanProbe.pause();
        PartBindings.Outcome outcome;
        try {
            outcome = bindings.update(parts, canClaim,
                    (pos, role) -> stillBound(controller, pos, role), claim, release);
        } finally {
            StructureScanProbe.resume();
        }
        if (controller.getLevel() instanceof ServerLevel server)
            WorldStructureClaims.retain(server, controller.getBlockPos(),
                    outcome == PartBindings.Outcome.FAILED ? java.util.Set.of() : parts.keySet());
        return switch (outcome) {
            case APPLIED -> Result.APPLIED;
            case UNCHANGED -> Result.UNCHANGED;
            case FAILED -> Result.FAILED;
        };
    }

    public static void cleared(BlockEntity controller) {
        if (controller.getLevel() instanceof ServerLevel server)
            WorldStructureClaims.releaseController(server, controller.getBlockPos());
    }

    public static void runQuiet(Runnable work) {
        StructureScanProbe.pause();
        try {
            work.run();
        } finally {
            StructureScanProbe.resume();
        }
    }

    /** The part entity can be recreated by a chunk reload; its derived cache must agree too. */
    private static boolean stillBound(BlockEntity controller, BlockPos pos, MultiblockLayout.Role role) {
        Level level = controller.getLevel();
        if (level == null) return false;
        return level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port
                && port.hasBindingCache(controller.getBlockPos(), role);
    }
}
