package gregtech.content.transport.pipe.fluid;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * A snapshot of one connected, loaded fluid pipe network. It exists only for the shared-pool mode:
 * storage stays in each pipe's own tanks, and the pool is this snapshot's derived view.
 *
 * <p>Any layout change invalidates the whole snapshot; the next pool operation rebuilds it.</p>
 */
public final class FluidPipeNetwork {
    /** Shared by all members, so invalidation detaches the snapshot without visiting every pipe. */
    static final class Membership {
        private FluidPipeNetwork network;

        FluidPipeNetwork network() {
            return network;
        }
    }

    private final Level level;
    private final FluidPipeTopology topology;
    private final int[] pipeChunks;
    private final BlockPos[] chunkPositions;
    private final long[] checkedChunks;
    private long availabilityEpoch;
    private final Membership membership = new Membership();
    private boolean valid = true;

    private FluidPipeNetwork(Level level, FluidPipeTopology topology, int[] pipeChunks, BlockPos[] chunkPositions) {
        this.level = level;
        this.topology = topology;
        this.pipeChunks = pipeChunks;
        this.chunkPositions = chunkPositions;
        this.checkedChunks = new long[chunkPositions.length];
        membership.network = this;
    }

    /** Breadth-first over connected, loaded pipes from {@code start}. */
    static FluidPipeNetwork build(Level level, FluidPipeBlockEntity start) {
        FluidPipeTopology topology = FluidPipeTopology.scan(level, start);
        Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
        chunks.defaultReturnValue(-1);
        List<BlockPos> chunkPositions = new ArrayList<>();
        int[] pipeChunks = new int[topology.size()];
        for (int i = 0; i < topology.size(); i++) {
            pipeChunks[i] = chunkIndex(chunks, chunkPositions, topology.pipe(i).getBlockPos());
        }

        FluidPipeNetwork network =
                new FluidPipeNetwork(level, topology, pipeChunks, chunkPositions.toArray(new BlockPos[0]));
        for (int i = 0; i < topology.size(); i++) {
            FluidPipeNetwork previous = topology.pipe(i).network();
            if (previous != null) previous.invalidate();
        }
        for (int i = 0; i < topology.size(); i++) topology.pipe(i).membership(network.membership);
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

    boolean valid() {
        return valid;
    }

    boolean contains(FluidPipeBlockEntity pipe) {
        return topology.indexOf(pipe) >= 0;
    }

    int size() {
        return topology.size();
    }

    FluidPipeBlockEntity pipe(int index) {
        return topology.pipe(index);
    }

    /** Start one pool pass; chunk liveness is checked once per touched chunk per pass. */
    void beginPass() {
        availabilityEpoch++;
    }

    boolean live(int pipe) {
        if (!valid) return false;
        if (topology.pipe(pipe).isRemoved()) {
            invalidate();
            return false;
        }
        int chunk = pipeChunks[pipe];
        if (checkedChunks[chunk] != availabilityEpoch && !level.hasChunkAt(chunkPositions[chunk])) {
            invalidate();
            return false;
        }
        checkedChunks[chunk] = availabilityEpoch;
        return true;
    }

    void invalidate() {
        if (!valid) return;
        valid = false;
        membership.network = null;
    }
}
