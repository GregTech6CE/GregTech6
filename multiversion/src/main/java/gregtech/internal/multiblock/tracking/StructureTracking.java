package gregtech.internal.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Query, attach and evaluate entry points used by every controller. */
public final class StructureTracking {
    private static boolean consistency;

    private StructureTracking() {}

    /** Test builds only: after every incremental evaluation, a full scan must agree with it. */
    public static void consistencyCheck(boolean on) {
        consistency = on;
    }

    public static boolean consistencyCheck() {
        return consistency;
    }

    public static boolean query(TrackedMultiblock machine) {
        if (StructureScanProbe.inScan()) return false;
        if (StructureTrackingMode.legacy()) return scan(machine);
        BlockEntity entity = machine.trackedEntity();
        Level level = entity.getLevel();
        if (level == null || entity.isRemoved()) return false;
        if (level.isClientSide) return ClientFormedSync.get(entity);
        StructureTracker tracker = attach(machine);
        return tracker != null && tracker.formed();
    }

    public static void invalidate(TrackedMultiblock machine) {
        if (StructureScanProbe.inScan()) return;
        if (StructureTrackingMode.legacy()) {
            scan(machine);
            return;
        }
        BlockEntity entity = machine.trackedEntity();
        if (entity.getLevel() == null || entity.getLevel().isClientSide || entity.isRemoved()) return;
        StructureTracker tracker = attach(machine);
        if (tracker == null) return;
        boolean fell = tracker.invalidate();
        StructureWatchIndex.enqueue(tracker);
        if (fell) {
            machine.structureFormedChanged(false);
            ClientFormedSync.send(entity);
        }
    }

    public static void onChunkUnloaded(TrackedMultiblock machine) {
        if (StructureTrackingMode.legacy()) return;
        BlockEntity entity = machine.trackedEntity();
        Level level = entity.getLevel();
        if (!(level instanceof ServerLevel server) || entity.isRemoved()) return;
        WorldStructureClaims.controllerUnloaded(server, entity.getBlockPos());
        StructureTracker tracker = StructureWatchIndex.tracker(entity);
        if (tracker == null) return;
        StructureTrackingEvents.record("controller.onChunkUnloaded " + entity.getBlockPos().asLong());
        tracker.markUnloading();
        boolean fell = tracker.suspend();
        StructureWatchIndex.clearWatch(server, tracker);
        if (fell) machine.structureFormedChanged(false);
    }

    public static boolean unloading(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        if (entity.getLevel() == null || entity.getLevel().isClientSide) return false;
        StructureTracker tracker = StructureWatchIndex.tracker(entity);
        return tracker != null && tracker.unloading();
    }

    public static void onRemoved(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        if (entity.getLevel() == null || entity.getLevel().isClientSide) return;
        StructureTracker tracker = StructureWatchIndex.tracker(entity);
        if (!StructureTrackingMode.legacy() && entity.getLevel() instanceof ServerLevel server
                && (tracker == null || !tracker.unloading()))
            WorldStructureClaims.releaseController(server, entity.getBlockPos());
        if (tracker != null) {
            StructureTrackingEvents.record("controller.setRemoved " + entity.getBlockPos().asLong());
            StructureWatchIndex.remove(tracker);
        }
    }

    public static StructureTracker tracker(BlockEntity entity) {
        return entity == null ? null : StructureWatchIndex.tracker(entity);
    }

    /** Called by a scan body with the layer counts it matched, in layout repeat order. */
    public static void noteMatchedLayers(BlockEntity entity, int[] layers) {
        StructureTracker tracker = tracker(entity);
        if (tracker != null) tracker.matchedLayers(layers);
    }

    public static StructureTracker attach(TrackedMultiblock machine) {
        if (StructureTrackingMode.legacy()) return null;
        BlockEntity entity = machine.trackedEntity();
        Level level = entity.getLevel();
        if (level == null || level.isClientSide || entity.isRemoved()) return null;
        if (!(level instanceof ServerLevel server)) return null;
        if (!isCurrentEntity(server, entity)) return null;
        StructureTracker existing = StructureWatchIndex.tracker(entity);
        if (existing != null) return existing;
        StructureTracker tracker = StructureWatchIndex.attach(machine);
        StructureTrackingEvents.record("tracker.attach " + entity.getBlockPos().asLong());
        StructureDefinition definition = machine.structureDefinition();
        if (definition.formsImmediately()) {
            if (tracker.markFormed(StructureEpoch.get())) machine.structureFormedChanged(true);
            return tracker;
        }
        install(server, tracker, definition.watchFootprint(machine));
        tracker.enqueue(false);
        StructureWatchIndex.enqueue(tracker);
        return tracker;
    }

    static void evaluate(StructureTracker tracker) {
        TrackedMultiblock machine = tracker.machine();
        BlockEntity entity = tracker.entity();
        Level level = entity.getLevel();
        if (!(level instanceof ServerLevel server) || entity.isRemoved() || tracker.unloading()) {
            tracker.discard();
            return;
        }
        BlockPos controllerPos = entity.getBlockPos();
        LevelChunk controllerChunk = server.getChunkSource().getChunkNow(controllerPos.getX() >> 4, controllerPos.getZ() >> 4);
        if (controllerChunk == null) {
            // Losing FULL availability precedes native onChunkUnloaded/setRemoved. Keep the
            // tracker until those callbacks so they can still distinguish unload from removal.
            if (tracker.suspend()) machine.structureFormedChanged(false);
            return;
        }
        if (controllerChunk.getBlockEntities().get(controllerPos) != entity) {
            // External chunk editors can remove a BE without calling setRemoved. Its stale
            // tracker must never claim parts again while the deferred orphan check is pending.
            tracker.discard();
            StructureWatchIndex.remove(tracker);
            return;
        }
        WorldStructureClaims.beginEvaluation(tracker);
        StructureDefinition definition = machine.structureDefinition();
        if (definition.formsImmediately()) {
            StructureWatchIndex.clearWatch(server, tracker);
            boolean rose = tracker.markFormed(StructureEpoch.get());
            if (rose) {
                machine.structureFormedChanged(true);
                ClientFormedSync.send(entity);
            }
            return;
        }
        boolean facingChanged = tracker.consumeFacing();
        boolean epochChanged = tracker.epoch() != StructureEpoch.get();
        boolean full = tracker.takeFullEval();
        if (definition instanceof StructureDefinition.Declarative declarative && declarative.incremental()) {
            evaluateDeclarative(server, tracker, declarative, facingChanged, epochChanged, full);
            return;
        }
        boolean refreshWatch = facingChanged || full || tracker.watch().size() == 0 || epochChanged;
        if (refreshWatch) install(server, tracker, definition.watchFootprint(machine));
        if (!chunksLoaded(server, tracker.watch())) {
            boolean fell = tracker.suspend();
            if (fell) {
                machine.structureFormedChanged(false);
                ClientFormedSync.send(entity);
            }
            return;
        }
        boolean formed = scan(machine);
        if (StructureScanProbe.auditing()) {
            StructureScanProbe.assertInside(entity, definition.declaredFootprint(machine));
        }
        if (definition.tightenWatch()) {
            install(server, tracker, definition.watchFootprint(machine));
        }
        boolean edge;
        if (formed) edge = tracker.markFormed(StructureEpoch.get());
        else edge = tracker.markBroken(StructureEpoch.get());
        if (edge) {
            machine.structureFormedChanged(formed);
            ClientFormedSync.send(entity);
        }
    }

    /**
     * A declarative definition keeps a compiled pattern and one cached verdict per cell. Only changed
     * cells and the repeat layers above them are read; a full evaluation happens on attach, rotation,
     * epoch change, a safety scan, or when the pattern itself changed.
     */
    private static void evaluateDeclarative(ServerLevel server, StructureTracker tracker,
                                            StructureDefinition.Declarative definition,
                                            boolean facingChanged, boolean epochChanged, boolean full) {
        TrackedMultiblock machine = tracker.machine();
        BlockEntity entity = tracker.entity();
        Level level = entity.getLevel();
        int epoch = StructureEpoch.get();
        Direction front = LayoutStructures.facing(entity);
        CompiledPattern pattern = tracker.pattern();
        if (pattern == null || tracker.patternFront() != front || tracker.patternEpoch() != epoch) {
            pattern = definition.compile(front);
            tracker.pattern(pattern, front, epoch);
            full = true;
        }
        if (pattern == null) {
            tracker.takeChangedCells();
            tracker.takeReloadedChunks();
            machine.releaseStructureClaims();
            boolean edge = tracker.markBroken(epoch);
            if (edge) {
                machine.structureFormedChanged(false);
                ClientFormedSync.send(entity);
            }
            return;
        }
        if (full || facingChanged || epochChanged) install(server, tracker, definition.watchFootprint(machine));
        if (!chunksLoaded(server, tracker.watch())) {
            tracker.takeChangedCells();
            tracker.takeReloadedChunks();
            boolean fell = tracker.suspend();
            if (fell) {
                machine.structureFormedChanged(false);
                ClientFormedSync.send(entity);
            }
            return;
        }
        PatternState state = tracker.patternState();
        int capacity = pattern.capacity(level.getHeight());
        if (state == null || state.pattern() != pattern || state.capacity() != capacity) {
            state = new PatternState(pattern, level.getHeight());
            tracker.patternState(state);
            full = true;
        }
        if (full) {
            state.reset();
            tracker.takeChangedCells();
            tracker.takeReloadedChunks();
        } else {
            state.applyChanges(entity.getBlockPos(), tracker.takeChangedCells(), tracker.takeReloadedChunks());
        }
        boolean matched;
        StructureScanProbe.openIncremental();
        try {
            state.evaluate(level, entity.getBlockPos());
            matched = state.complete() && machine.structurePartsValid();
            // Claims can write part visuals (the crucible does); those are not external changes.
            if (matched) matched = machine.applyStructure(state.match(entity.getBlockPos()));
            else machine.applyIncompleteStructure(state.match(entity.getBlockPos()));
        } finally {
            StructureScanProbe.close();
        }
        tracker.matchedLayers(state.matchedLayers());
        install(server, tracker, definition.watchFootprint(machine));
        boolean edge = matched ? tracker.markFormed(epoch) : tracker.markBroken(epoch);
        if (edge) {
            machine.structureFormedChanged(matched);
            ClientFormedSync.send(entity);
        }
        if (consistency) {
            boolean scanned = scan(machine);
            if (scanned != matched) {
                throw new IllegalStateException("incremental structure evaluation disagrees with a full scan: incremental="
                        + matched + " full=" + scanned + " at " + entity.getBlockPos()
                        + " (" + machine.getClass().getSimpleName() + ")");
            }
        }
    }

    /** Test-only: when a projection calls a change equivalent, a full scan must agree with the cached bit. */
    static void auditEquivalent(StructureTracker tracker) {
        if (tracker.unloading() || tracker.entity().isRemoved()) return;
        if (!(tracker.entity().getLevel() instanceof ServerLevel server) || !isCurrentEntity(server, tracker.entity())) return;
        auditProjection(tracker.watch(), tracker.formed(), () -> scan(tracker.machine()));
    }

    public static void auditProjection(Footprint watch, boolean cachedFormed, java.util.function.BooleanSupplier fullScan) {
        boolean scanned = fullScan.getAsBoolean();
        if (scanned != cachedFormed) {
            throw new IllegalStateException("structure projection audit: cached formed=" + cachedFormed
                    + " but a full scan returned " + scanned + " for a watch of " + watch.size() + " cells");
        }
    }

    private static boolean scan(TrackedMultiblock machine) {
        StructureScanProbe.open();
        try {
            return machine.scanStructure();
        } finally {
            StructureScanProbe.close();
        }
    }

    private static void install(ServerLevel level, StructureTracker tracker, Footprint footprint) {
        StructureWatchIndex.watch(level, tracker, footprint == null ? Footprint.EMPTY : footprint);
    }

    private static boolean chunksLoaded(ServerLevel level, Footprint footprint) {
        if (footprint == null) return true;
        for (long packed : footprint.chunks()) {
            ChunkPos chunk = new ChunkPos(packed);
            BlockPos probe = new BlockPos(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ());
            if (!level.hasChunkAt(probe)) return false;
        }
        return true;
    }

    private static boolean isCurrentEntity(ServerLevel level, BlockEntity entity) {
        BlockPos pos = entity.getBlockPos();
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockEntities().get(pos) == entity;
    }

    /** One direct evaluation of this controller. The world queue is left alone. */
    public static void evaluateForTests(BlockEntity entity) {
        StructureTracker tracker = tracker(entity);
        if (tracker == null || entity.isRemoved()) return;
        tracker.enqueue(false);
        evaluate(tracker);
    }

    /** Section index helper shared with tests that do not want to construct a chunk. */
    public static boolean maskWatched(LevelChunk chunk, int sectionIndex) {
        return ((StructureChunkWatch) (Object) chunk).gt$structureWatched(sectionIndex);
    }
}
