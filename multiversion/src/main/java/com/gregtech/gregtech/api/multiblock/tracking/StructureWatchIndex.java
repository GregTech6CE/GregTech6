package com.gregtech.gregtech.api.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Section and chunk indexes for one server world. The data lives on the {@code ServerLevel}. */
public final class StructureWatchIndex {
    private StructureWatchIndex() {}

    private static WorldData existing(ServerLevel level) {
        return ((StructureLevelData) (Object) level).gt$structureData();
    }

    static WorldData data(ServerLevel level) {
        if (level.isClientSide) {
            throw new IllegalStateException("structure tracking data requested for a client level");
        }
        if (StructureScanProbe.enabled() && !level.getServer().isSameThread()) {
            throw new IllegalStateException("structure tracking data touched off the server thread");
        }
        WorldData data = existing(level);
        if (data == null) {
            data = new WorldData();
            ((StructureLevelData) (Object) level).gt$structureData(data);
        }
        return data;
    }

    public static void defer(ServerLevel level, long chunk) {
        data(level).pending.add(chunk);
    }

    public static boolean pending(ServerLevel level, long chunk) {
        WorldData data = existing(level);
        return data != null && data.pending.contains(chunk);
    }

    public static void onPortReleased(ServerLevel level, BlockPos pos, BlockPos owner) {
        if (level == null || pos == null) return;
        WorldData data = existing(level);
        if (data == null) return;
        List<StructureTracker> trackers = data.sections.get(sectionKey(pos));
        if (trackers == null) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            if (tracker.unloading() || tracker.dirty() || tracker.entity().isRemoved()) continue;
            if (owner != null && owner.equals(tracker.pos())) continue;
            Footprint watch = tracker.watch();
            if (watch == null || watch.indexOf(pos) < 0) continue;
            tracker.enqueue(false);
            addDirty(data, tracker);
        }
    }

    static void unloadWorld(ServerLevel level) {
        WorldData data = existing(level);
        if (data == null) return;
        data.dirty.clear();
        data.pending.clear();
        data.entities.clear();
        data.all.clear();
        data.sections.clear();
        data.chunks.clear();
        data.safety.clear();
        ((StructureLevelData) (Object) level).gt$structureData(null);
    }

    public static void onChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        WorldData data = existing(level);
        if (data == null) return;
        long section = sectionKey(pos);
        List<StructureTracker> trackers = data.sections.get(section);
        if (trackers == null || trackers.isEmpty()) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            Footprint watch = tracker.watch();
            if (watch == null) continue;
            int cell = watch.indexOf(pos);
            if (cell < 0) continue;
            if (watch.projection(cell).equivalent(level, pos, oldState, newState)) {
                if (StructureScanProbe.auditing() && !StructureScanProbe.inScan())
                    StructureTracking.auditEquivalent(tracker);
                continue;
            }
            if (pos.equals(tracker.pos())) tracker.noteFacing();
            boolean fell = tracker.invalidateCell(pos);
            addDirty(data, tracker);
            if (fell) {
                tracker.machine().structureFormedChanged(false);
                ClientFormedSync.send(tracker.entity());
            }
        }
    }

    static void chunkLoaded(ServerLevel level, ChunkPos pos) {
        WorldData data = data(level);
        long key = pos.toLong();
        BlockPos probe = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
        if (!level.hasChunkAt(probe)) {
            data.pending.add(key);
            return;
        }
        data.pending.remove(key);
        paintChunk(level, data, pos, true);
        List<StructureTracker> trackers = data.chunks.get(key);
        if (trackers == null) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            if (tracker.unloading() || tracker.entity().isRemoved()) continue;
            tracker.noteChunkReloaded(key);
            tracker.enqueue(false);
            addDirty(data, tracker);
        }
    }

    static void chunkUnloaded(ServerLevel level, ChunkPos pos) {
        WorldData data = existing(level);
        if (data == null) return;
        long key = pos.toLong();
        data.pending.remove(key);
        List<StructureTracker> trackers = data.chunks.get(key);
        if (trackers == null) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            if (tracker.unloading()) continue;
            boolean fell = tracker.suspend();
            removeDirty(data, tracker);
            if (fell) {
                tracker.machine().structureFormedChanged(false);
                ClientFormedSync.send(tracker.entity());
            }
        }
    }

    static void promotePending(ServerLevel level) {
        WorldData data = existing(level);
        if (data == null || data.pending.isEmpty()) return;
        Iterator<Long> pending = data.pending.iterator();
        while (pending.hasNext()) {
            long key = pending.next();
            ChunkPos pos = new ChunkPos(key);
            BlockPos probe = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
            if (!level.hasChunkAt(probe)) continue;
            pending.remove();
            paintChunk(level, data, pos, true);
            List<StructureTracker> trackers = data.chunks.get(key);
            if (trackers == null) continue;
            for (StructureTracker tracker : List.copyOf(trackers)) {
                if (tracker.unloading() || tracker.entity().isRemoved()) continue;
                tracker.noteChunkReloaded(key);
                tracker.enqueue(false);
                addDirty(data, tracker);
            }
        }
    }

    static StructureTracker tracker(BlockEntity entity) {
        Level level = entity.getLevel();
        if (!(level instanceof ServerLevel server)) return null;
        WorldData data = existing(server);
        return data == null ? null : data.entities.get(entity);
    }

    static StructureTracker attach(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        if (!(entity.getLevel() instanceof ServerLevel server)) return null;
        WorldData data = data(server);
        StructureTracker existing = data.entities.get(entity);
        if (existing != null) return existing;
        StructureTracker tracker = new StructureTracker(machine);
        data.entities.put(entity, tracker);
        data.all.add(tracker);
        data.safety.computeIfAbsent(tracker.safetyBucket(), key -> new LinkedHashSet<>()).add(tracker);
        return tracker;
    }

    static void watch(ServerLevel level, StructureTracker tracker, Footprint footprint) {
        WorldData data = data(level);
        clearWatch(level, tracker);
        tracker.watch(footprint);
        if (footprint == null) return;
        for (long section : footprint.sections()) add(data.sections, section, tracker);
        for (long chunk : footprint.chunks()) add(data.chunks, chunk, tracker);
        paint(level, footprint, true);
    }

    static void clearWatch(ServerLevel level, StructureTracker tracker) {
        WorldData data = existing(level);
        Footprint footprint = tracker.watch();
        if (data == null || footprint == null || footprint.size() == 0 && footprint.sections().length == 0) {
            tracker.watch(Footprint.EMPTY);
            return;
        }
        for (long section : footprint.sections()) remove(data.sections, section, tracker);
        for (long chunk : footprint.chunks()) remove(data.chunks, chunk, tracker);
        paint(level, footprint, false);
        tracker.watch(Footprint.EMPTY);
    }

    static void remove(StructureTracker tracker) {
        BlockEntity entity = tracker.entity();
        if (entity.getLevel() instanceof ServerLevel server) clearWatch(server, tracker);
        if (!(entity.getLevel() instanceof ServerLevel server)) return;
        WorldData data = existing(server);
        if (data == null) return;
        data.entities.remove(entity);
        data.all.remove(tracker);
        removeDirty(data, tracker);
        Set<StructureTracker> bucket = data.safety.get(tracker.safetyBucket());
        if (bucket != null) {
            bucket.remove(tracker);
            if (bucket.isEmpty()) data.safety.remove(tracker.safetyBucket());
        }
    }

    static void enqueue(StructureTracker tracker) {
        if (!(tracker.entity().getLevel() instanceof ServerLevel server)) return;
        addDirty(data(server), tracker);
    }

    static void enqueueAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            WorldData data = existing(level);
            if (data == null) continue;
            for (StructureTracker tracker : data.all) {
                if (tracker.unloading() || tracker.entity().isRemoved()) continue;
                tracker.enqueue(false);
                addDirty(data, tracker);
            }
        }
    }

    static List<StructureTracker> drainDirty(ServerLevel level) {
        WorldData data = existing(level);
        if (data == null || data.dirty.isEmpty()) return List.of();
        List<StructureTracker> batch = new ArrayList<>(data.dirty);
        data.dirty.clear();
        for (StructureTracker tracker : batch) tracker.queued(false);
        return batch;
    }

    /** Returns an unprocessed tracker to the head, keeping its queue slot. */
    static void requeue(StructureTracker tracker) {
        if (!(tracker.entity().getLevel() instanceof ServerLevel server)) return;
        WorldData data = data(server);
        data.dirty.remove(tracker);
        tracker.queued(true);
        data.dirty.addFirst(tracker);
    }

    private static void addDirty(WorldData data, StructureTracker tracker) {
        if (tracker.queued()) return;
        tracker.queued(true);
        data.dirty.addLast(tracker);
    }

    private static void removeDirty(WorldData data, StructureTracker tracker) {
        if (!tracker.queued()) return;
        tracker.queued(false);
        data.dirty.remove(tracker);
    }

    static boolean hasDirty(ServerLevel level) {
        WorldData data = existing(level);
        return data != null && !data.dirty.isEmpty();
    }

    static Iterable<StructureTracker> all(ServerLevel level) {
        WorldData data = existing(level);
        return data == null ? List.of() : data.all;
    }

    static Iterable<StructureTracker> safetyBucket(ServerLevel level, int bucket) {
        WorldData data = existing(level);
        if (data == null) return List.of();
        return data.safety.getOrDefault(bucket, java.util.Set.of());
    }

    private static void paint(ServerLevel level, Footprint footprint, boolean ignored) {
        WorldData data = data(level);
        for (long section : footprint.sections()) {
            SectionPos pos = SectionPos.of(section);
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX(), pos.getZ());
            if (chunk == null) continue;
            int index = chunk.getSectionIndex(pos.minBlockY());
            List<StructureTracker> owners = data.sections.get(section);
            ((StructureChunkWatch) (Object) chunk).gt$structureWatch(index, owners != null && !owners.isEmpty());
        }
    }

    private static void paintChunk(ServerLevel level, WorldData data, ChunkPos pos, boolean watched) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) return;
        List<StructureTracker> trackers = data.chunks.get(pos.toLong());
        if (trackers == null) return;
        Set<Long> sections = new HashSet<>();
        for (StructureTracker tracker : trackers) {
            for (long section : tracker.watch().sections()) {
                SectionPos sectionPos = SectionPos.of(section);
                if (sectionPos.getX() == pos.x && sectionPos.getZ() == pos.z) sections.add(section);
            }
        }
        for (long section : sections) {
            SectionPos sectionPos = SectionPos.of(section);
            int index = chunk.getSectionIndex(sectionPos.minBlockY());
            boolean any = data.sections.getOrDefault(section, List.of()).stream().findAny().isPresent();
            ((StructureChunkWatch) (Object) chunk).gt$structureWatch(index, any && watched);
        }
    }

    private static void add(Map<Long, List<StructureTracker>> map, long key, StructureTracker tracker) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(tracker);
    }

    private static void remove(Map<Long, List<StructureTracker>> map, long key, StructureTracker tracker) {
        List<StructureTracker> list = map.get(key);
        if (list == null) return;
        list.remove(tracker);
        if (list.isEmpty()) map.remove(key);
    }

    private static long sectionKey(BlockPos pos) {
        return SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static final class WorldData {
        final Map<Long, List<StructureTracker>> sections = new HashMap<>();
        final Map<Long, List<StructureTracker>> chunks = new HashMap<>();
        final Map<BlockEntity, StructureTracker> entities = new IdentityHashMap<>();
        final Set<StructureTracker> all = new LinkedHashSet<>();
        final Map<Integer, Set<StructureTracker>> safety = new HashMap<>();
        final Deque<StructureTracker> dirty = new ArrayDeque<>();
        final Set<Long> pending = new HashSet<>();
    }
}
