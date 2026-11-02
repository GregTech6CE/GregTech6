package gregtech.content.transport;

import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * A snapshot of one connected, loaded item pipe network: its pipes, the links between them, the
 * potential inventory faces beside them, and each source's resumable {@link WeightedItemPipeRoutes.Search}.
 *
 * <p>The snapshot holds what changes only with the world's layout — connections, wrench I/O
 * settings and retriever covers. Every connected non-pipe face remains a candidate exit, even while
 * its chunk or item capability is unavailable. Shutters, conveyor modes, filters and the current
 * target inventory are checked when items are sent.
 * Any layout change {@link #invalidate invalidates} the whole snapshot; the next pipe that needs
 * it builds a fresh one. Stable networks reuse their topology and keep a bounded cache of routes.</p>
 */
public final class ItemPipeNetwork implements WeightedItemPipeRoutes.Graph {
    /** Shared by all members, so invalidation detaches the snapshot without visiting every pipe. */
    public static final class Membership {
        private ItemPipeNetwork network;

        public ItemPipeNetwork network() {
            return network;
        }
    }

    private record Exit(int pipe, Direction face) {}

    private static final int[] NONE = new int[0];
    // Account for allocated array capacities, including the frontier and discovered exits.
    private static final long SEARCH_BUDGET_BYTES = 8L << 20;

    private final Level level;
    private final ItemPipeBlockEntity[] pipes;
    private final int[] pipeChunks;
    private final BlockPos[] chunkPositions;
    private final long[] checkedChunks;
    private long availabilityEpoch;
    private final Map<ItemPipeBlockEntity, Integer> index;
    private final int[][] links;
    private final int[][] exitsOf;
    private final Exit[] exits;
    private final Membership membership = new Membership();
    private final Map<ItemPipeBlockEntity, WeightedItemPipeRoutes.Search> searches =
            new LinkedHashMap<>(16, 0.75f, true);
    private long cachedBytes;
    /** Sources that delivered through this snapshot; woken when it is invalidated. */
    private final Set<ItemPipeBlockEntity> sources = new LinkedHashSet<>();

    private final Consumer<ItemPipeBlockEntity> wake;
    private boolean valid = true;

    private ItemPipeNetwork(
            Level level,
            ItemPipeBlockEntity[] pipes,
            Map<ItemPipeBlockEntity, Integer> index,
            int[][] links,
            int[][] exitsOf,
            Exit[] exits,
            Consumer<ItemPipeBlockEntity> wake) {
        this.level = level;
        this.pipes = pipes;
        this.pipeChunks = new int[pipes.length];
        Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
        chunks.defaultReturnValue(-1);
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < pipes.length; i++) {
            BlockPos pos = pipes[i].getBlockPos();
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            int chunk = chunks.get(key);
            if (chunk < 0) {
                chunk = positions.size();
                chunks.put(key, chunk);
                positions.add(pos);
            }
            pipeChunks[i] = chunk;
        }
        this.chunkPositions = positions.toArray(new BlockPos[0]);
        this.checkedChunks = new long[chunkPositions.length];
        this.index = index;
        this.links = links;
        this.exitsOf = exitsOf;
        this.exits = exits;
        this.wake = wake;
        membership.network = this;
    }

    /** Breadth-first over connected, loaded pipes from {@code start}, up to the visit limit. */
    static ItemPipeNetwork build(Level level, ItemPipeBlockEntity start, Consumer<ItemPipeBlockEntity> wake) {
        List<ItemPipeBlockEntity> found = new ArrayList<>();
        Map<ItemPipeBlockEntity, Integer> index = new IdentityHashMap<>();
        ArrayDeque<ItemPipeBlockEntity> queue = new ArrayDeque<>();
        index.put(start, 0);
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            ItemPipeBlockEntity pipe = queue.poll();
            for (Direction face : Direction.values()) {
                ItemPipeBlockEntity neighbor = connectedPipe(level, pipe, face);
                if (neighbor == null
                        || index.containsKey(neighbor)
                        || found.size() >= WeightedItemPipeRoutes.MAX_VISITED_PIPES) continue;
                index.put(neighbor, found.size());
                found.add(neighbor);
                queue.add(neighbor);
            }
        }

        ItemPipeBlockEntity[] pipes = found.toArray(new ItemPipeBlockEntity[0]);
        int[][] links = new int[pipes.length][];
        int[][] exitsOf = new int[pipes.length][];
        List<Exit> exits = new ArrayList<>();
        for (int i = 0; i < pipes.length; i++) {
            ItemPipeBlockEntity pipe = pipes[i];
            int[] linked = new int[6];
            int[] beside = new int[6];
            int linkCount = 0, exitCount = 0;
            for (Direction face : Direction.values()) {
                if (!pipe.connected(face)) continue;
                ItemPipeBlockEntity neighbor = connectedPipe(level, pipe, face);
                if (neighbor != null) {
                    Integer to = index.get(neighbor);
                    if (to != null && pipe.linksOut(face) && neighbor.linksIn(face.getOpposite()))
                        linked[linkCount++] = to;
                } else {
                    if (pipe.linksOut(face)) {
                        beside[exitCount++] = exits.size();
                        exits.add(new Exit(i, face));
                    }
                }
            }
            links[i] = linkCount == 0 ? NONE : java.util.Arrays.copyOf(linked, linkCount);
            exitsOf[i] = exitCount == 0 ? NONE : java.util.Arrays.copyOf(beside, exitCount);
        }
        ItemPipeNetwork network =
                new ItemPipeNetwork(level, pipes, index, links, exitsOf, exits.toArray(new Exit[0]), wake);
        // A bounded scan may overlap an older snapshot in a network larger than the visit limit.
        // Do not leave both snapshots live with independent route caches for the same pipes.
        for (ItemPipeBlockEntity pipe : pipes) {
            ItemPipeNetwork previous = pipe.network();
            if (previous != null) previous.invalidate();
        }
        for (ItemPipeBlockEntity pipe : pipes) pipe.membership(network.membership);
        return network;
    }

    /** The pipe across {@code face} when both sides declare the connection and its chunk is loaded. */
    private static ItemPipeBlockEntity connectedPipe(Level level, ItemPipeBlockEntity pipe, Direction face) {
        if (!pipe.connected(face)) return null;
        BlockPos next = pipe.getBlockPos().relative(face);
        if (!level.hasChunkAt(next)) return null;
        return level.getBlockEntity(next) instanceof ItemPipeBlockEntity neighbor
                        && !neighbor.isRemoved()
                        && neighbor.connected(face.getOpposite())
                ? neighbor
                : null;
    }

    public boolean valid() {
        return valid;
    }

    public int size() {
        return pipes.length;
    }

    /** Drop the snapshot and wake every source that used it, so blocked items retry on the new layout. */
    public void invalidate() {
        if (!valid) return;
        valid = false;
        membership.network = null;
        searches.clear();
        cachedBytes = 0;
        List<ItemPipeBlockEntity> waiting = new ArrayList<>(sources);
        sources.clear();
        for (ItemPipeBlockEntity source : waiting) {
            if (!source.isRemoved() && source.hasStoredItems()) wake.accept(source);
        }
    }

    /** An inventory beside this network reported room: let its waiting sources retry now. */
    void wakeSources() {
        sources.removeIf(source -> source.isRemoved() || !source.hasStoredItems());
        for (ItemPipeBlockEntity source : new ArrayList<>(sources)) wake.accept(source);
    }

    /**
     * Move {@code source}'s stacks to the nearest exits that take them.
     *
     * @return the number of stacks moved
     */
    public int deliverFrom(ItemPipeBlockEntity source) {
        Integer at = index.get(source);
        if (!valid || at == null || source.isRemoved()) return 0;
        sources.add(source);
        WeightedItemPipeRoutes.Search search = searches.get(source);
        if (search == null) {
            search = new WeightedItemPipeRoutes.Search(this, at, WeightedItemPipeRoutes.MAX_VISITED_PIPES);
            searches.put(source, search);
            cachedBytes += search.retainedBytes();
        }
        long before = search.retainedBytes();
        availabilityEpoch++;
        int moved = ItemPipeNetworkTransfer.deliver(search, new ItemPipeNetworkTransfer.Channel() {
            @Override
            public boolean hasItems() {
                return valid && source.hasStoredItems();
            }

            @Override
            public boolean hasCapacity(int pipe) {
                return live(pipe) && pipes[pipe].pipeCapacityCheck();
            }

            @Override
            public void charge(int pipe) {
                pipes[pipe].chargeTransfer();
            }

            @Override
            public boolean linkOpen(int from, int to) {
                BlockPos a = pipes[from].getBlockPos(), b = pipes[to].getBlockPos();
                Direction face = Direction.fromDelta(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
                return face != null
                        && pipes[from].connected(face)
                        && pipes[to].connected(face.getOpposite())
                        && pipes[from].canEmitTo(face)
                        && pipes[to].canLinkIn(face.getOpposite());
            }

            @Override
            public boolean send(int exit) {
                Exit target = exits[exit];
                try {
                    return valid && pipes[target.pipe()].sendTo(target.face(), source);
                } finally {
                    // Inventory callbacks may change chunk tickets before removal hooks run.
                    availabilityEpoch++;
                }
            }
        });
        // Target callbacks can invalidate the snapshot, clearing its cache during this pass.
        if (!valid) return moved;
        cachedBytes += search.retainedBytes() - before;
        if (!source.hasStoredItems()) sources.remove(source);
        trimSearches();
        return moved;
    }

    private boolean live(int pipe) {
        if (!valid) return false;
        int chunk = pipeChunks[pipe];
        if (pipes[pipe].isRemoved()
                || (checkedChunks[chunk] != availabilityEpoch && !level.hasChunkAt(chunkPositions[chunk]))) {
            invalidate();
            return false;
        }
        // Chunk availability can disappear before onChunkUnloaded/setRemoved. Check once per
        // touched chunk and delivery attempt, rather than resolving the entity on every hop.
        checkedChunks[chunk] = availabilityEpoch;
        return true;
    }

    /** Prefer retaining nearby searches; ties evict the least recently used source. */
    private void trimSearches() {
        while (cachedBytes > SEARCH_BUDGET_BYTES && !searches.isEmpty()) {
            ItemPipeBlockEntity largest = null;
            long size = -1;
            for (var entry : searches.entrySet()) {
                long bytes = entry.getValue().retainedBytes();
                if (bytes > size) {
                    largest = entry.getKey();
                    size = bytes;
                }
            }
            searches.remove(largest);
            cachedBytes -= size;
        }
    }

    // ── WeightedItemPipeRoutes.Graph ─────────────────────────────────────────

    @Override
    public int pipes() {
        return pipes.length;
    }

    @Override
    public long stepSize(int pipe) {
        return pipes[pipe].spec().stepSize();
    }

    @Override
    public int[] links(int pipe) {
        return links[pipe];
    }

    @Override
    public int[] exits(int pipe) {
        return exitsOf[pipe];
    }
}
