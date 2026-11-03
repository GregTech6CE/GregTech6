package gregtech.content.transport.energy;

import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.blockentity.energy.ElectricWireBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.WirePacketRules;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A snapshot of one connected, loaded electric wire network.
 *
 * <p>The snapshot holds what only changes with the world's layout: the wires, the links between
 * them, and every connected non-wire face. An endpoint is resolved live when energy reaches it.
 * Any layout change {@link #invalidate invalidates} the whole snapshot; the next injection that
 * needs it builds a fresh one. Traversal follows the same depth-first order, voltage loss and
 * ampere sharing as the original recursive walk.</p>
 */
public final class ElectricNetwork {
    /** Shared by all members, so invalidation detaches the snapshot without visiting every wire. */
    public static final class Membership {
        private ElectricNetwork network;

        public ElectricNetwork network() {
            return network;
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    private final Level level;
    private final ElectricWireBlockEntity[] wires;
    private final int[] wireChunks;
    private final BlockPos[] chunkPositions;
    private final long[] checkedChunks;
    private long availabilityEpoch;
    private final Map<ElectricWireBlockEntity, Integer> index;
    private final int[][] wireLinks;
    private final int[][] endpointLinks;
    private final BlockPos[] endpointPositions;
    private final int[] endpointChunks;
    private final Membership membership = new Membership();
    private Traversal traversal;
    private boolean valid = true;

    private ElectricNetwork(
            Level level,
            ElectricWireBlockEntity[] wires,
            Map<ElectricWireBlockEntity, Integer> index,
            int[][] wireLinks,
            int[][] endpointLinks,
            BlockPos[] endpointPositions,
            int[] endpointChunks,
            int[] wireChunks,
            BlockPos[] chunkPositions) {
        this.level = level;
        this.wires = wires;
        this.index = index;
        this.wireLinks = wireLinks;
        this.endpointLinks = endpointLinks;
        this.endpointPositions = endpointPositions;
        this.endpointChunks = endpointChunks;
        this.wireChunks = wireChunks;
        this.chunkPositions = chunkPositions;
        this.checkedChunks = new long[chunkPositions.length];
        membership.network = this;
    }

    /** Breadth-first over connected, loaded wires from {@code start}. */
    static ElectricNetwork build(Level level, ElectricWireBlockEntity start) {
        List<ElectricWireBlockEntity> found = new ArrayList<>();
        Map<ElectricWireBlockEntity, Integer> index = new IdentityHashMap<>();
        ArrayDeque<ElectricWireBlockEntity> queue = new ArrayDeque<>();
        index.put(start, 0);
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            ElectricWireBlockEntity wire = queue.poll();
            for (Direction side : DIRECTIONS) {
                if (!connected(wire, side)) continue;
                BlockPos next = wire.getBlockPos().relative(side);
                if (!level.hasChunkAt(next)) continue;
                if (!(level.getBlockEntity(next) instanceof ElectricWireBlockEntity neighbor)
                        || neighbor.isRemoved()
                        || !connected(neighbor, side.getOpposite())
                        || index.containsKey(neighbor)) continue;
                index.put(neighbor, found.size());
                found.add(neighbor);
                queue.add(neighbor);
            }
        }

        ElectricWireBlockEntity[] wires = found.toArray(new ElectricWireBlockEntity[0]);
        Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
        chunks.defaultReturnValue(-1);
        List<BlockPos> chunkPositions = new ArrayList<>();
        int[] wireChunks = new int[wires.length];
        for (int i = 0; i < wires.length; i++) {
            wireChunks[i] = chunkIndex(chunks, chunkPositions, wires[i].getBlockPos());
        }

        Long2IntOpenHashMap endpointIds = new Long2IntOpenHashMap();
        endpointIds.defaultReturnValue(-1);
        List<BlockPos> endpoints = new ArrayList<>();
        int[][] wireLinks = new int[wires.length][6];
        int[][] endpointLinks = new int[wires.length][6];
        for (int i = 0; i < wires.length; i++) {
            java.util.Arrays.fill(wireLinks[i], -1);
            java.util.Arrays.fill(endpointLinks[i], -1);
            ElectricWireBlockEntity wire = wires[i];
            for (Direction side : DIRECTIONS) {
                if (!connected(wire, side)) continue;
                int ordinal = side.ordinal();
                BlockPos next = wire.getBlockPos().relative(side);
                if (level.hasChunkAt(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    if (be instanceof ElectricWireBlockEntity neighbor) {
                        if (!neighbor.isRemoved() && connected(neighbor, side.getOpposite())) {
                            Integer to = index.get(neighbor);
                            if (to != null) wireLinks[i][ordinal] = to;
                        }
                        // A wire that does not link back is not an endpoint either.
                        continue;
                    }
                }
                long key = next.asLong();
                int id = endpointIds.get(key);
                if (id < 0) {
                    id = endpoints.size();
                    endpointIds.put(key, id);
                    endpoints.add(next.immutable());
                }
                endpointLinks[i][ordinal] = id;
            }
        }
        BlockPos[] endpointPositions = endpoints.toArray(new BlockPos[0]);
        int[] endpointChunks = new int[endpointPositions.length];
        for (int i = 0; i < endpointPositions.length; i++) {
            endpointChunks[i] = chunkIndex(chunks, chunkPositions, endpointPositions[i]);
        }

        ElectricNetwork network = new ElectricNetwork(
                level,
                wires,
                index,
                wireLinks,
                endpointLinks,
                endpointPositions,
                endpointChunks,
                wireChunks,
                chunkPositions.toArray(new BlockPos[0]));
        for (ElectricWireBlockEntity wire : wires) {
            ElectricNetwork previous = wire.network();
            if (previous != null) previous.invalidate();
        }
        for (ElectricWireBlockEntity wire : wires) wire.membership(network.membership);
        return network;
    }

    private static int chunkIndex(Long2IntOpenHashMap chunks, List<BlockPos> positions, BlockPos pos) {
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        int chunk = chunks.get(key);
        if (chunk < 0) {
            chunk = positions.size();
            chunks.put(key, chunk);
            positions.add(pos);
        }
        return chunk;
    }

    /** Both sides declare the connection, in the same sense the original traversal required. */
    private static boolean connected(ElectricWireBlockEntity wire, Direction side) {
        return !wire.isRemoved() && wire.getBlockState().getValue(ElectricWireBlock.propFor(side));
    }

    public boolean valid() {
        return valid;
    }

    public boolean contains(ElectricWireBlockEntity wire) {
        return index.containsKey(wire);
    }

    /** Drop the snapshot; the next injection rebuilds it from the current layout. */
    public void invalidate() {
        if (!valid) return;
        valid = false;
        membership.network = null;
    }

    /** Move one offered packet through the network from {@code root}. */
    public long inject(ElectricWireBlockEntity root, Direction fromSide, long packetVoltage, long packetAmperage) {
        if (!valid) return 0;
        Integer entry = index.get(root);
        if (entry == null) return 0;
        if (!WirePacketRules.canTransfer(packetVoltage, packetAmperage, root.lossPerBlock())) return 0;
        availabilityEpoch++;
        if (!live(entry)) return 0;

        Traversal stack = acquire();
        int stamp = stack.nextStamp();
        stack.wireStamp[entry] = stamp;
        stack.depth = 1;
        stack.wires[0] = entry;
        stack.from[0] = fromSide.ordinal();
        stack.voltages[0] = WirePacketRules.afterLoss(packetVoltage, root.lossPerBlock());
        stack.amperages[0] = packetAmperage;
        stack.used[0] = 0;
        stack.next[0] = 0;

        long result = 0;
        while (stack.depth > 0) {
            int top = stack.depth - 1;
            int wireIndex = stack.wires[top];
            int from = stack.from[top];
            long voltage = stack.voltages[top];
            long amperage = stack.amperages[top];
            long used = stack.used[top];
            int next = stack.next[top];
            boolean descended = false;

            while (next < 6) {
                if (!valid) break;
                int ordinal = next++;
                if (ordinal == from) continue;
                if (used >= amperage) {
                    next = 6;
                    break;
                }
                int child = wireLinks[wireIndex][ordinal];
                if (child >= 0) {
                    if (stack.wireStamp[child] == stamp) continue;
                    long remaining = amperage - used;
                    ElectricWireBlockEntity childWire = wires[child];
                    if (!WirePacketRules.canTransfer(voltage, remaining, childWire.lossPerBlock())) continue;
                    if (!live(child)) break;
                    stack.wireStamp[child] = stamp;
                    if (stack.depth == stack.wires.length) stack.grow();
                    int at = stack.depth++;
                    stack.wires[at] = child;
                    stack.from[at] = DIRECTIONS[ordinal].getOpposite().ordinal();
                    stack.voltages[at] = WirePacketRules.afterLoss(voltage, childWire.lossPerBlock());
                    stack.amperages[at] = remaining;
                    stack.used[at] = 0;
                    stack.next[at] = 0;
                    descended = true;
                    break;
                }
                int endpoint = endpointLinks[wireIndex][ordinal];
                if (endpoint < 0 || stack.endpointStamp[endpoint] == stamp) continue;
                stack.endpointStamp[endpoint] = stamp;
                // Unloaded endpoint chunks only skip their own face; the snapshot stays valid.
                if (!chunkLoaded(endpointChunks[endpoint])) continue;
                BlockEntity neighbor = level.getBlockEntity(endpointPositions[endpoint]);
                if (neighbor instanceof ElectricWireBlockEntity) {
                    // The face became a wire after this snapshot was built; never inject it as an endpoint.
                    invalidate();
                    break;
                }
                long remaining = amperage - used;
                used += EnergyTransfer.insertEnergyInto(
                        GregTechTags.Energy.EU,
                        DIRECTIONS[ordinal].getOpposite(),
                        voltage,
                        remaining,
                        wires[wireIndex],
                        neighbor);
            }

            if (descended) {
                stack.used[top] = used;
                stack.next[top] = next;
                continue;
            }

            stack.depth--;
            long returned = used > 0 ? (wires[wireIndex].addToEnergyTransferred(voltage, used) ? used : amperage) : 0;
            if (stack.depth == 0) result = returned;
            else stack.used[stack.depth - 1] += returned;
        }
        release(stack);
        return result;
    }

    private boolean live(int wire) {
        if (!valid) return false;
        if (wires[wire].isRemoved()) {
            invalidate();
            return false;
        }
        return liveChunk(wireChunks[wire]);
    }

    private boolean liveChunk(int chunk) {
        if (!valid) return false;
        // Chunk availability can disappear before the unload hooks run.
        if (!chunkLoaded(chunk)) {
            invalidate();
            return false;
        }
        return true;
    }

    /** Availability of one touched chunk, cached for the current injection. */
    private boolean chunkLoaded(int chunk) {
        if (checkedChunks[chunk] != availabilityEpoch && !level.hasChunkAt(chunkPositions[chunk])) {
            return false;
        }
        checkedChunks[chunk] = availabilityEpoch;
        return true;
    }

    private Traversal acquire() {
        Traversal stack = traversal;
        if (stack != null) {
            traversal = null;
            stack.depth = 0;
            return stack;
        }
        return new Traversal(wires.length + 1, wires.length, endpointPositions.length);
    }

    private void release(Traversal stack) {
        if (traversal == null) {
            stack.depth = 0;
            traversal = stack;
        }
    }

    private static final class Traversal {
        /**
         * Visit marks are per traversal: a relay may inject into this same network while an outer
         * traversal is still running, and the nested traversal must not overwrite its marks.
         */
        int[] wireStamp;
        int[] endpointStamp;
        int stamp;
        int[] wires;
        int[] from;
        long[] voltages;
        long[] amperages;
        long[] used;
        int[] next;
        int depth;

        Traversal(int capacity, int wireCount, int endpointCount) {
            wireStamp = new int[wireCount];
            endpointStamp = new int[endpointCount];
            wires = new int[capacity];
            from = new int[capacity];
            voltages = new long[capacity];
            amperages = new long[capacity];
            used = new long[capacity];
            next = new int[capacity];
        }

        int nextStamp() {
            int next = ++stamp;
            if (next == 0) {
                java.util.Arrays.fill(wireStamp, 0);
                java.util.Arrays.fill(endpointStamp, 0);
                next = ++stamp;
            }
            return next;
        }

        void grow() {
            int capacity = wires.length * 2;
            wires = java.util.Arrays.copyOf(wires, capacity);
            from = java.util.Arrays.copyOf(from, capacity);
            voltages = java.util.Arrays.copyOf(voltages, capacity);
            amperages = java.util.Arrays.copyOf(amperages, capacity);
            used = java.util.Arrays.copyOf(used, capacity);
            next = java.util.Arrays.copyOf(next, capacity);
        }
    }
}
