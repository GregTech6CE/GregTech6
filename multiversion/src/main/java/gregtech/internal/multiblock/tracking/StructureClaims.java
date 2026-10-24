package gregtech.internal.multiblock.tracking;

import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.PartBindings;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Skips bind and design writes when a formed structure's cells have not changed. */
public final class StructureClaims {
    public enum Result { APPLIED, UNCHANGED, FAILED }

    private StructureClaims() {}

    public static Result update(BlockEntity controller, boolean expectFormed,
                                PartBindings<BlockPos, MultiblockLayout.Role> bindings,
                                Map<BlockPos, MultiblockLayout.Role> parts, int designHash,
                                Predicate<BlockPos> canClaim, BiConsumer<BlockPos, MultiblockLayout.Role> claim,
                                Consumer<BlockPos> release) {
        StructureTracker tracker = StructureTracking.tracker(controller);
        if (expectFormed && tracker != null && tracker.claimsMatch(parts, designHash)
                && stillBound(controller.getLevel(), controller.getBlockPos(), parts))
            return Result.UNCHANGED;
        StructureScanProbe.pause();
        boolean claimed;
        try {
            claimed = bindings.update(parts, canClaim, claim, release);
        } finally {
            StructureScanProbe.resume();
        }
        if (tracker != null) {
            if (claimed && expectFormed) tracker.rememberClaims(parts, designHash);
            else tracker.forgetClaims();
        }
        return claimed ? Result.APPLIED : Result.FAILED;
    }

    public static void cleared(BlockEntity controller) {
        StructureTracker tracker = StructureTracking.tracker(controller);
        if (tracker != null) tracker.forgetClaims();
    }

    public static void runQuiet(Runnable work) {
        StructureScanProbe.pause();
        try {
            work.run();
        } finally {
            StructureScanProbe.resume();
        }
    }

    public static int designHash(int seed, Iterable<Integer> designs) {
        int hash = 31 * seed + 1;
        int count = 0;
        if (designs != null) {
            for (int design : designs) {
                hash = 31 * hash + design;
                count++;
            }
        }
        return hash * 31 + count;
    }

    private static boolean stillBound(Level level, BlockPos controller, Map<BlockPos, MultiblockLayout.Role> parts) {
        if (level == null) return false;
        for (var entry : parts.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.hasChunkAt(pos)) return false;
            if (!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)) return false;
            if (!port.isBoundTo(controller)) return false;
        }
        return true;
    }
}
