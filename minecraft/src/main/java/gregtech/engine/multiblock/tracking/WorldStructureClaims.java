package gregtech.engine.multiblock.tracking;

import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.engine.multiblock.StructureClaimTable;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Platform boundary for the transient claim table owned by the existing ServerLevel field. */
public final class WorldStructureClaims {
    private final StructureClaimTable claims = create();
    private final Map<Long, ControllerKind> controllers = new HashMap<>();
    private final Map<Long, Set<Long>> controllerChunks = new HashMap<>();
    private final Map<Integer, Set<Long>> safety = new HashMap<>();
    private final Map<Long, Long> orphanChecks = new HashMap<>();
    private final Map<Long, Set<StructureTracker>> waiters = new HashMap<>();
    private final Map<StructureTracker, Set<Long>> waiting = new IdentityHashMap<>();

    private WorldStructureClaims() {}

    public static StructureClaimTable table(ServerLevel level) {
        return data(level).claims;
    }

    static WorldStructureClaims data(ServerLevel level) {
        return StructureWatchIndex.data(level).claims;
    }

    static WorldStructureClaims createWorldData() {
        return new WorldStructureClaims();
    }

    static StructureClaimTable create() {
        return new StructureClaimTable(part -> ChunkPos.asLong(BlockPos.of(part)), Long2LongOpenHashMap::new);
    }

    public static BlockPos owner(ServerLevel level, BlockPos part) {
        Long owner = table(level).owner(part.asLong());
        return owner == null ? null : BlockPos.of(owner);
    }

    public static boolean canClaim(ServerLevel level, BlockPos part, BlockPos owner) {
        WorldStructureClaims data = data(level);
        if (data.claims.canClaim(part.asLong(), owner.asLong())) return true;
        BlockEntity contender = loadedEntity(level, owner);
        StructureTracker tracker = contender == null ? null : StructureTracking.tracker(contender);
        if (tracker != null && !tracker.unloading()) {
            data.waiters.computeIfAbsent(part.asLong(), key -> new HashSet<>()).add(tracker);
            data.waiting.computeIfAbsent(tracker, key -> new HashSet<>()).add(part.asLong());
            Long occupant = data.claims.owner(part.asLong());
            if (occupant == null) occupant = data.claims.reservedOwner(part.asLong());
            tracker.claimConflict(part, BlockPos.of(occupant));
        }
        return false;
    }

    public static boolean claim(ServerLevel level, BlockPos part, BlockPos owner) {
        if (!canClaim(level, part, owner)) return false;
        WorldStructureClaims data = data(level);
        BlockEntity controller = loadedEntity(level, owner);
        if (controller == null || controller.isRemoved()) return false;
        if (!data.claims.claim(part.asLong(), owner.asLong())) return false;
        data.register(level, controller);
        return true;
    }

    public static boolean release(ServerLevel level, BlockPos part, BlockPos owner) {
        WorldStructureClaims data = data(level);
        if (!data.claims.release(part.asLong(), owner.asLong())) return false;
        data.deleted(level, part.asLong());
        data.prune(owner.asLong());
        return true;
    }

    /** Releases missing/reserved cells too, which PartBindings' loaded-BE callbacks cannot reach. */
    static void retain(ServerLevel level, BlockPos owner, Set<BlockPos> candidates) {
        WorldStructureClaims data = data(level);
        Set<Long> old = new HashSet<>(data.claims.loadedParts(owner.asLong()));
        old.addAll(data.claims.reservedParts(owner.asLong()));
        for (long part : old)
            if (!candidates.contains(BlockPos.of(part))) data.releasePart(level, part, owner.asLong());
    }

    static void releaseController(ServerLevel level, BlockPos owner) {
        retain(level, owner, Set.of());
    }

    private void releasePart(ServerLevel level, long part, long owner) {
        BlockEntity entity = loadedEntity(level, BlockPos.of(part));
        if (claims.owner(part) != null && entity instanceof MultiblockPortBlockEntity port)
            port.release(BlockPos.of(owner));
        // There may be no part BE (replacement, missing chunk, or orphan cleanup).
        release(level, BlockPos.of(part), BlockPos.of(owner));
    }

    static void controllerUnloaded(ServerLevel level, BlockPos owner) {
        WorldStructureClaims data = data(level);
        data.claims.controllerUnloaded(owner.asLong());
        ControllerKind kind = data.controllers.get(owner.asLong());
        if (kind != null) kind.loadedAt = -1;
        data.orphanChecks.remove(owner.asLong());
        data.prune(owner.asLong());
    }

    static void chunkUnloaded(ServerLevel level, ChunkPos chunk) {
        WorldStructureClaims data = data(level);
        for (long owner : Set.copyOf(data.controllerChunks.getOrDefault(chunk.toLong(), Set.of())))
            controllerUnloaded(level, BlockPos.of(owner));
        Set<Long> owners = data.claims.chunkUnloaded(chunk.toLong(), owner -> {
            BlockPos pos = BlockPos.of(owner);
            if (ChunkPos.asLong(pos) == chunk.toLong()) return false;
            BlockEntity entity = loadedEntity(level, pos);
            StructureTracker tracker = entity == null ? null : StructureTracking.tracker(entity);
            return entity != null && !entity.isRemoved() && tracker != null && !tracker.unloading();
        });
        for (long owner : owners) data.prune(owner);
    }

    static void chunkLoaded(ServerLevel level, ChunkPos chunk) {
        WorldStructureClaims data = data(level);
        // This must precede hasChunkAt and pending-chunk handling in StructureWatchIndex.
        data.claims.chunkLoaded(chunk.toLong());
        for (long owner : data.controllerChunks.getOrDefault(chunk.toLong(), Set.of())) {
            data.controllers.get(owner).loadedAt = level.getGameTime();
            data.orphanChecks.put(owner, level.getGameTime() + 1);
        }
    }

    public static void partRemoved(ServerLevel level, BlockPos part) {
        BlockPos owner = owner(level, part);
        if (owner == null) return;
        release(level, part, owner);
        BlockEntity controller = loadedEntity(level, owner);
        if (controller instanceof TrackedMultiblock machine) {
            StructureTracker tracker = StructureTracking.tracker(controller);
            if (tracker == null || tracker.unloading()) return;
            // BE removal can arrive after setBlock's cell invalidation. Keep it local: forcing
            // a full evaluation here would undo the incremental evaluator's dirty-cell cache.
            boolean fell = tracker.invalidateCell(part);
            StructureWatchIndex.enqueue(tracker);
            if (fell) {
                StructureTracking.formedChanged(machine, false);
                ClientFormedSync.send(controller);
            }
        }
    }

    public static void partUnloaded(ServerLevel level, BlockPos part) {
        WorldStructureClaims data = data(level);
        Long owner = data.claims.owner(part.asLong());
        if (owner == null) return; // ChunkEvent.Unload already moved/dropped this entry.
        BlockEntity entity = loadedEntity(level, BlockPos.of(owner));
        StructureTracker tracker = entity == null ? null : StructureTracking.tracker(entity);
        data.claims.partUnloaded(part.asLong(), tracker != null && !tracker.unloading() && !entity.isRemoved());
        data.prune(owner);
    }

    static void beginEvaluation(StructureTracker tracker) {
        if (!(tracker.entity().getLevel() instanceof ServerLevel level)) return;
        data(level).forgetWaiter(tracker);
        tracker.clearClaimConflict();
    }

    static boolean waiting(StructureTracker tracker) {
        return tracker.entity().getLevel() instanceof ServerLevel level
                && data(level).waiting.containsKey(tracker);
    }

    static void removed(StructureTracker tracker) {
        if (tracker.entity().getLevel() instanceof ServerLevel level)
            data(level).forgetWaiter(tracker);
    }

    private void forgetWaiter(StructureTracker tracker) {
        Set<Long> parts = waiting.remove(tracker);
        if (parts == null) return;
        for (long part : parts) {
            Set<StructureTracker> contenders = waiters.get(part);
            if (contenders == null) continue;
            contenders.remove(tracker);
            if (contenders.isEmpty()) waiters.remove(part);
        }
    }

    private void deleted(ServerLevel level, long part) {
        Set<StructureTracker> contenders = waiters.remove(part);
        if (contenders == null) return;
        for (StructureTracker tracker : contenders) {
            Set<Long> parts = waiting.get(tracker);
            if (parts != null) {
                parts.remove(part);
                if (parts.isEmpty()) waiting.remove(tracker);
            }
            if (tracker.unloading() || tracker.entity().isRemoved()) continue;
            tracker.enqueue(false);
            StructureWatchIndex.enqueue(tracker);
        }
    }

    /** Deferred chunk-load orphan checks, plus the current safety bucket. Never test tracker presence. */
    static void checkOrphans(ServerLevel level, boolean all) {
        WorldStructureClaims data = data(level);
        long now = level.getGameTime();
        Set<Long> owners = all
                ? Set.copyOf(data.controllers.keySet())
                : Set.copyOf(
                        data.safety.getOrDefault((int) Math.floorMod(now, StructureScheduler.SAFETY_PERIOD), Set.of()));
        for (long owner : owners) {
            ControllerKind kind = data.controllers.get(owner);
            BlockPos pos = BlockPos.of(owner);
            if (kind == null || !level.hasChunkAt(pos)) continue;
            if (kind.loadedAt < 0) {
                kind.loadedAt = now;
                data.orphanChecks.put(owner, now + 1);
            } else data.orphanChecks.putIfAbsent(owner, kind.loadedAt + 1);
        }
        for (var check : Map.copyOf(data.orphanChecks).entrySet()) {
            if (now < check.getValue()) continue;
            long owner = check.getKey();
            BlockPos pos = BlockPos.of(owner);
            if (!level.hasChunkAt(pos)) continue;
            // A holder's availability and getChunkNow can change at different points in the
            // lifecycle. An unreadable chunk is not evidence that its controller disappeared.
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            ControllerKind kind = data.controllers.get(owner);
            data.orphanChecks.remove(owner);
            if (kind == null) continue;
            BlockEntity occupant = loadedEntity(level, pos);
            if (occupant == null
                    || occupant.isRemoved()
                    || occupant.getType() != kind.type
                    || occupant.getBlockState().getBlock() != kind.block) releaseController(level, pos);
        }
    }

    private void register(ServerLevel level, BlockEntity entity) {
        long owner = entity.getBlockPos().asLong();
        if (controllers.containsKey(owner)) return;
        controllers.put(owner, new ControllerKind(entity, level.getGameTime()));
        controllerChunks
                .computeIfAbsent(ChunkPos.asLong(entity.getBlockPos()), key -> new HashSet<>())
                .add(owner);
        safety.computeIfAbsent(bucket(owner), key -> new HashSet<>()).add(owner);
    }

    private void prune(long owner) {
        if (!claims.loadedParts(owner).isEmpty() || !claims.reservedParts(owner).isEmpty()) return;
        if (controllers.remove(owner) == null) return;
        removeIndex(controllerChunks, ChunkPos.asLong(BlockPos.of(owner)), owner);
        removeIndex(safety, bucket(owner), owner);
        orphanChecks.remove(owner);
    }

    private static <K> void removeIndex(Map<K, Set<Long>> index, K key, long owner) {
        Set<Long> owners = index.get(key);
        if (owners == null) return;
        owners.remove(owner);
        if (owners.isEmpty()) index.remove(key);
    }

    private static int bucket(long owner) {
        return Math.floorMod(Long.hashCode(owner), StructureScheduler.SAFETY_PERIOD);
    }

    private static BlockEntity loadedEntity(ServerLevel level, BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntities().get(pos);
    }

    void clear() {
        claims.clear();
        controllers.clear();
        controllerChunks.clear();
        safety.clear();
        orphanChecks.clear();
        waiters.clear();
        waiting.clear();
    }

    private static final class ControllerKind {
        final BlockEntityType<?> type;
        final Block block;
        long loadedAt;

        ControllerKind(BlockEntity entity, long now) {
            type = entity.getType();
            block = entity.getBlockState().getBlock();
            loadedAt = now;
        }
    }
}
