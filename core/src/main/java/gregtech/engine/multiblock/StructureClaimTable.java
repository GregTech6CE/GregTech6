package gregtech.engine.multiblock;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

/**
 * Transient ownership for one world, confined to its server thread. Coordinates are opaque longs;
 * the platform supplies the part-to-chunk projection. Loaded parts and reservations have separate
 * indexes, but both exclude competing owners. No block entities, roles or saved data live here.
 */
public final class StructureClaimTable {
    private final LongUnaryOperator chunkOf;
    private final Index loaded;
    private final Index reserved;

    public StructureClaimTable(LongUnaryOperator chunkOf) {
        this(chunkOf, HashMap::new);
    }

    /** Platforms can supply primitive long maps without introducing a dependency into core. */
    public StructureClaimTable(LongUnaryOperator chunkOf, Supplier<Map<Long, Long>> maps) {
        this.chunkOf = Objects.requireNonNull(chunkOf);
        loaded = new Index(Objects.requireNonNull(maps.get()));
        reserved = new Index(Objects.requireNonNull(maps.get()));
        if (loaded.parts == reserved.parts || !loaded.parts.isEmpty() || !reserved.parts.isEmpty())
            throw new IllegalArgumentException("Claim indexes require separate empty maps");
    }

    /** The owner of a loaded part, including an owner whose own chunk is absent. */
    public Long owner(long part) {
        return loaded.parts.get(part);
    }

    public Long reservedOwner(long part) {
        return reserved.parts.get(part);
    }

    public boolean canClaim(long part, long controller) {
        Long owner = loaded.parts.get(part);
        if (owner == null) owner = reserved.parts.get(part);
        return owner == null || owner.longValue() == controller;
    }

    public boolean claim(long part, long controller) {
        if (!canClaim(part, controller)) return false;
        reserved.remove(part);
        loaded.put(part, controller);
        return true;
    }

    /** Validates the whole set before changing either index; does not release previous claims. */
    public boolean claimAll(Collection<Long> parts, long controller) {
        Objects.requireNonNull(parts);
        // Snapshot also validates nulls before any mutation and tolerates duplicate candidates.
        Set<Long> candidates = Set.copyOf(parts);
        for (long part : candidates) if (!canClaim(part, controller)) return false;
        for (long part : candidates) claim(part, controller);
        return true;
    }

    /** A stale controller cannot release another controller's claim or reservation. */
    public boolean release(long part, long controller) {
        if (Objects.equals(loaded.parts.get(part), controller)) {
            loaded.remove(part);
            return true;
        }
        if (Objects.equals(reserved.parts.get(part), controller)) {
            reserved.remove(part);
            return true;
        }
        return false;
    }

    /** Destruction/disassembly, including reservations in absent part chunks. */
    public Set<Long> releaseController(long controller) {
        Set<Long> released = new HashSet<>(loaded.byOwner(controller));
        released.addAll(reserved.byOwner(controller));
        for (long part : released) release(part, controller);
        return Set.copyOf(released);
    }

    /** Controller unload keeps loaded parts, but reservations cannot outlive their controller. */
    public void controllerUnloaded(long controller) {
        for (long part : reserved.byOwner(controller)) reserved.remove(part);
    }

    /**
     * Part chunk unload never changes block state. Only a surviving loaded controller keeps a
     * reservation; an absent controller loses this part's ownership under the bounded rule.
     */
    public Set<Long> chunkUnloaded(long chunk, LongPredicate controllerLoaded) {
        Objects.requireNonNull(controllerLoaded);
        Set<Long> parts = loaded.byChunk(chunk);
        Map<Long, Boolean> owners = new HashMap<>();
        for (long part : parts) {
            long owner = loaded.parts.get(part);
            owners.computeIfAbsent(owner, controllerLoaded::test);
        }
        for (long part : parts) {
            long owner = loaded.parts.get(part);
            partUnloaded(part, owners.get(owner));
        }
        return Set.copyOf(owners.keySet());
    }

    public void partUnloaded(long part, boolean controllerLoaded) {
        Long owner = loaded.parts.get(part);
        if (owner == null) return;
        loaded.remove(part);
        if (controllerLoaded) reserved.put(part, owner);
    }

    /** Runs synchronously at chunk-load notification, before availability checks or evaluation. */
    public void chunkLoaded(long chunk) {
        for (long part : reserved.byChunk(chunk)) {
            long owner = reserved.parts.get(part);
            reserved.remove(part);
            loaded.put(part, owner);
        }
    }

    public Set<Long> loadedParts(long controller) {
        return loaded.byOwner(controller);
    }

    public Set<Long> reservedParts(long controller) {
        return reserved.byOwner(controller);
    }

    /** Snapshot for deferred orphan checks; never infer orphanhood from missing trackers. */
    public Set<Long> controllers() {
        Set<Long> owners = new HashSet<>(loaded.owners.keySet());
        owners.addAll(reserved.owners.keySet());
        return Set.copyOf(owners);
    }

    public int size() {
        return loaded.parts.size();
    }

    public int reservationCount() {
        return reserved.parts.size();
    }

    public void clear() {
        loaded.clear();
        reserved.clear();
    }

    private final class Index {
        private final Map<Long, Long> parts;
        private final Map<Long, Set<Long>> owners = new HashMap<>();
        private final Map<Long, Set<Long>> chunks = new HashMap<>();

        Index(Map<Long, Long> parts) {
            this.parts = parts;
        }

        void put(long part, long owner) {
            Long previous = parts.putIfAbsent(part, owner);
            if (previous != null) {
                if (previous.longValue() != owner) throw new IllegalStateException("Conflicting claim");
                return;
            }
            owners.computeIfAbsent(owner, ignored -> new HashSet<>()).add(part);
            chunks.computeIfAbsent(chunkOf.applyAsLong(part), ignored -> new HashSet<>())
                    .add(part);
        }

        void remove(long part) {
            Long owner = parts.remove(part);
            if (owner == null) return;
            removeFrom(owners, owner, part);
            removeFrom(chunks, chunkOf.applyAsLong(part), part);
        }

        Set<Long> byOwner(long owner) {
            return Set.copyOf(owners.getOrDefault(owner, Set.of()));
        }

        Set<Long> byChunk(long chunk) {
            return Set.copyOf(chunks.getOrDefault(chunk, Set.of()));
        }

        void clear() {
            parts.clear();
            owners.clear();
            chunks.clear();
        }

        private void removeFrom(Map<Long, Set<Long>> index, long key, long part) {
            Set<Long> entries = index.get(key);
            entries.remove(part);
            if (entries.isEmpty()) index.remove(key);
        }
    }
}
