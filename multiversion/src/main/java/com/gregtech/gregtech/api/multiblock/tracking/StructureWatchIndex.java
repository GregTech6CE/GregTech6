package com.gregtech.gregtech.api.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Section and chunk indexes for one server world. */
public final class StructureWatchIndex {
    private static final Map<Level, WorldData> WORLDS = new WeakHashMap<>();

    private StructureWatchIndex() {}

    static WorldData data(Level level) {
        return WORLDS.computeIfAbsent(level, key -> new WorldData());
    }

    public static void defer(ServerLevel level, long chunk) {
        data(level).pending.add(chunk);
    }

    public static boolean pending(Level level, long chunk) {
        WorldData data = WORLDS.get(level);
        return data != null && data.pending.contains(chunk);
    }

    public static void onPortReleased(ServerLevel level, BlockPos pos, BlockPos owner) {
        if (level == null || pos == null) return;
        WorldData data = WORLDS.get(level);
        if (data == null) return;
        List<StructureTracker> trackers = data.sections.get(sectionKey(pos));
        if (trackers == null) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            if (tracker.unloading() || tracker.dirty() || tracker.entity().isRemoved()) continue;
            if (owner != null && owner.equals(tracker.pos())) continue;
            Footprint watch = tracker.watch();
            if (watch == null || watch.indexOf(pos) < 0) continue;
            tracker.enqueue(false, false);
            data.dirty.add(tracker);
        }
    }

    static void unloadWorld(Level level) {
        WORLDS.remove(level);
    }

    public static void onChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        WorldData data = WORLDS.get(level);
        if (data == null) return;
        long section = sectionKey(pos);
        List<StructureTracker> trackers = data.sections.get(section);
        if (trackers == null || trackers.isEmpty()) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            Footprint watch = tracker.watch();
            if (watch == null) continue;
            int cell = watch.indexOf(pos);
            if (cell < 0) continue;
            if (watch.projection(cell).equivalent(level, pos, oldState, newState)) continue;
            if (pos.equals(tracker.pos())) tracker.noteFacing();
            boolean fell = tracker.invalidate();
            data.dirty.add(tracker);
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
            tracker.enqueue(false, false);
            data.dirty.add(tracker);
        }
    }

    static void chunkUnloaded(ServerLevel level, ChunkPos pos) {
        WorldData data = WORLDS.get(level);
        if (data == null) return;
        long key = pos.toLong();
        data.pending.remove(key);
        List<StructureTracker> trackers = data.chunks.get(key);
        if (trackers == null) return;
        for (StructureTracker tracker : List.copyOf(trackers)) {
            if (tracker.unloading()) continue;
            boolean fell = tracker.suspend();
            data.dirty.remove(tracker);
            if (fell) tracker.machine().structureFormedChanged(false);
        }
    }

    static void promotePending(ServerLevel level) {
        WorldData data = WORLDS.get(level);
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
                tracker.enqueue(false, false);
                data.dirty.add(tracker);
            }
        }
    }

    static StructureTracker tracker(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) return null;
        WorldData data = WORLDS.get(level);
        return data == null ? null : data.entities.get(entity);
    }

    static StructureTracker attach(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Level level = entity.getLevel();
        WorldData data = data(level);
        StructureTracker existing = data.entities.get(entity);
        if (existing != null) return existing;
        StructureTracker tracker = new StructureTracker(machine);
        data.entities.put(entity, tracker);
        data.all.add(tracker);
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

    static void clearWatch(Level level, StructureTracker tracker) {
        WorldData data = WORLDS.get(level);
        Footprint footprint = tracker.watch();
        if (data == null || footprint == null || footprint.size() == 0 && footprint.sections().length == 0) {
            tracker.watch(Footprint.EMPTY);
            return;
        }
        for (long section : footprint.sections()) remove(data.sections, section, tracker);
        for (long chunk : footprint.chunks()) remove(data.chunks, chunk, tracker);
        if (level instanceof ServerLevel server) paint(server, footprint, false);
        tracker.watch(Footprint.EMPTY);
    }

    static void remove(StructureTracker tracker) {
        BlockEntity entity = tracker.entity();
        Level level = entity.getLevel();
        if (level != null) clearWatch(level, tracker);
        if (level == null) return;
        WorldData data = WORLDS.get(level);
        if (data == null) return;
        data.entities.remove(entity);
        data.all.remove(tracker);
        data.dirty.remove(tracker);
    }

    static void enqueue(StructureTracker tracker) {
        Level level = tracker.entity().getLevel();
        if (level == null) return;
        data(level).dirty.add(tracker);
    }

    static void enqueueAll() {
        for (WorldData data : WORLDS.values()) {
            for (StructureTracker tracker : data.all) {
                if (tracker.unloading() || tracker.entity().isRemoved()) continue;
                tracker.enqueue(false, false);
                data.dirty.add(tracker);
            }
        }
    }

    static List<StructureTracker> drainDirty(Level level) {
        WorldData data = WORLDS.get(level);
        if (data == null || data.dirty.isEmpty()) return List.of();
        List<StructureTracker> batch = new ArrayList<>(data.dirty);
        data.dirty.clear();
        return batch;
    }

    static void requeue(StructureTracker tracker) {
        Level level = tracker.entity().getLevel();
        if (level == null) return;
        data(level).dirty.add(tracker);
    }

    static boolean hasDirty(Level level) {
        WorldData data = WORLDS.get(level);
        return data != null && !data.dirty.isEmpty();
    }

    static Iterable<StructureTracker> all(Level level) {
        WorldData data = WORLDS.get(level);
        return data == null ? List.of() : data.all;
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

    static final class WorldData {
        final Map<Long, List<StructureTracker>> sections = new HashMap<>();
        final Map<Long, List<StructureTracker>> chunks = new HashMap<>();
        final Map<BlockEntity, StructureTracker> entities = new IdentityHashMap<>();
        final Set<StructureTracker> all = new LinkedHashSet<>();
        final Set<StructureTracker> dirty = new LinkedHashSet<>();
        final Set<Long> pending = new HashSet<>();
    }
}
