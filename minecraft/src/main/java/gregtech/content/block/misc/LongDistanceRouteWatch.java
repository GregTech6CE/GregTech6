package gregtech.content.block.misc;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Transient route proof. Saved endpoint coordinates alone never authenticate a route. */
final class LongDistanceRouteWatch {
    private final Map<BlockPos, BlockState> watched = new HashMap<>();
    private final gregtech.engine.logistics.PendingRouteFrontier<BlockPos> pending =
            new gregtech.engine.logistics.PendingRouteFrontier<>();
    private long observedVersion = Long.MIN_VALUE;
    private boolean validated;

    void clear() {
        watched.clear();
        pending.clear();
        observedVersion = Long.MIN_VALUE;
        validated = false;
    }

    void begin(Level level) {
        clear();
        validated = true;
        observedVersion = LongDistanceTopology.version(level);
    }

    /** Non-loading lookup; Forge may expose its already-readable currentlyLoading chunk. */
    private static net.minecraft.world.level.chunk.LevelChunk resident(Level level, BlockPos pos) {
        return level instanceof net.minecraft.server.level.ServerLevel server && level.hasChunkAt(pos)
                ? server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4)
                : null;
    }

    static boolean ready(Level level, BlockPos pos) {
        return resident(level, pos) != null;
    }

    static net.minecraft.world.level.block.entity.BlockEntity entity(Level level, BlockPos pos) {
        var chunk = resident(level, pos);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    boolean loaded(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        if (ready(level, pos)) return true;
        pending.addScanBoundary(chunkPosition(pos));
        return false;
    }

    BlockState observe(Level level, BlockPos pos) {
        if (!loaded(level, pos)) return Blocks.AIR.defaultBlockState();
        var state = resident(level, pos).getBlockState(pos);
        watched.put(pos.immutable(), state);
        return state;
    }

    /** Once a route succeeds, loading an unexplored side branch is not a broken route. */
    void resolved() {
        pending.clear();
    }

    private static BlockPos chunkPosition(BlockPos pos) {
        return new BlockPos(pos.getX() & ~15, 0, pos.getZ() & ~15);
    }

    boolean validated() {
        return validated;
    }

    boolean changed(Level level) {
        if (!validated) return false;
        // Readable availability can change after the load epoch was already observed.
        // Poll at most 16 missing chunks; a remembered watch only needs comparison.
        var ready = pending.pollReady(pos -> ready(level, pos));
        if (ready != null && ready.rescan()) return true;
        long version = LongDistanceTopology.version(level);
        if (ready == null && version == observedVersion) return false;
        observedVersion = version;
        for (var entry : watched.entrySet()) {
            // Preserve original authenticated-cache semantics across unloaded middle sections.
            // A server chunk-load event advances the epoch, so these cells are checked again.
            if (!ready(level, entry.getKey())) {
                pending.addWatchedChunk(chunkPosition(entry.getKey()));
                continue;
            }
            var previous = entry.getValue();
            var current = resident(level, entry.getKey()).getBlockState(entry.getKey());
            if (current == previous) continue;
            if (current.getBlock() == previous.getBlock()
                    && current.getBlock() instanceof LongDistanceTransformerBlock
                    && current.getValue(LongDistanceTransformerBlock.FACING)
                            == previous.getValue(LongDistanceTransformerBlock.FACING)) continue;
            return true;
        }
        return false;
    }
}
