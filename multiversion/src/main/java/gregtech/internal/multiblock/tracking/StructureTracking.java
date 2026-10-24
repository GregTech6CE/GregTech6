package gregtech.internal.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Query, attach and evaluate entry points used by every controller. */
public final class StructureTracking {
    private StructureTracking() {}

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
        StructureTracker tracker = StructureWatchIndex.tracker(machine.trackedEntity());
        if (tracker == null) return;
        tracker.markUnloading();
        boolean fell = tracker.suspend();
        Level level = machine.trackedEntity().getLevel();
        if (level != null) StructureWatchIndex.clearWatch(level, tracker);
        if (fell) machine.structureFormedChanged(false);
    }

    public static boolean unloading(TrackedMultiblock machine) {
        StructureTracker tracker = StructureWatchIndex.tracker(machine.trackedEntity());
        return tracker != null && tracker.unloading();
    }

    public static void onRemoved(TrackedMultiblock machine) {
        StructureTracker tracker = StructureWatchIndex.tracker(machine.trackedEntity());
        if (tracker != null) StructureWatchIndex.remove(tracker);
    }

    public static StructureTracker tracker(BlockEntity entity) {
        return entity == null ? null : StructureWatchIndex.tracker(entity);
    }

    public static StructureTracker attach(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Level level = entity.getLevel();
        if (level == null || level.isClientSide || entity.isRemoved()) return null;
        if (!(level instanceof ServerLevel server)) return null;
        StructureTracker existing = StructureWatchIndex.tracker(entity);
        if (existing != null) return existing;
        StructureTracker tracker = StructureWatchIndex.attach(machine);
        StructureDefinition definition = machine.structureDefinition();
        if (definition.formsImmediately()) {
            if (tracker.markFormed(StructureEpoch.get())) machine.structureFormedChanged(true);
            return tracker;
        }
        install(server, tracker, definition.watchFootprint(machine));
        tracker.enqueue(false, false);
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
        boolean refreshWatch = tracker.consumeFacing() || tracker.watch().size() == 0
                || tracker.epoch() != StructureEpoch.get();
        if (refreshWatch) install(server, tracker, definition.watchFootprint(machine));
        if (!chunksLoaded(server, tracker.watch())) {
            boolean fell = tracker.suspend();
            if (fell) {
                machine.structureFormedChanged(false);
                ClientFormedSync.send(entity);
            }
            return;
        }
        boolean wasFormed = tracker.formed();
        boolean safety = tracker.safety();
        boolean formed = scan(machine);
        if (StructureScanProbe.auditing()) {
            StructureScanProbe.assertInside(entity, definition.declaredFootprint(machine));
        }
        if (definition.tightenWatch()) {
            Footprint declared = definition.declaredFootprint(machine);
            install(server, tracker, StructureScanProbe.tighten(entity.getBlockPos(), declared));
        }
        boolean edge;
        if (formed) edge = tracker.markFormed(StructureEpoch.get());
        else edge = tracker.markBroken(StructureEpoch.get());
        if (edge) {
            machine.structureFormedChanged(formed);
            ClientFormedSync.send(entity);
        } else if (safety && wasFormed != formed) {
            machine.structureFormedChanged(formed);
            ClientFormedSync.send(entity);
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

    /** One safety scan of this controller. The world queue is left alone. */
    public static void evaluateForTests(BlockEntity entity, boolean safety) {
        StructureTracker tracker = tracker(entity);
        if (tracker == null || entity.isRemoved()) return;
        tracker.enqueue(false, safety);
        evaluate(tracker);
    }

    /** Section index helper shared with tests that do not want to construct a chunk. */
    public static boolean maskWatched(LevelChunk chunk, int sectionIndex) {
        return ((StructureChunkWatch) (Object) chunk).gt$structureWatched(sectionIndex);
    }
}
