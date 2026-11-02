package gregtech.content.transport.fluid;

import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
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

/**
 * A snapshot of one connected, loaded fluid pipe network. It exists only for the shared-pool mode:
 * storage stays in each pipe's own tanks, and the pool is this snapshot's derived view.
 *
 * <p>Any layout change invalidates the whole snapshot; the next pool operation rebuilds it.</p>
 */
public final class FluidPipeNetwork {
    /** Shared by all members, so invalidation detaches the snapshot without visiting every pipe. */
    public static final class Membership {
        private FluidPipeNetwork network;

        public FluidPipeNetwork network() {
            return network;
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    private final Level level;
    private final FluidPipeBlockEntity[] pipes;
    private final Map<FluidPipeBlockEntity, Integer> index;
    private final int[] pipeChunks;
    private final BlockPos[] chunkPositions;
    private final long[] checkedChunks;
    private long availabilityEpoch;
    private final Membership membership = new Membership();
    private boolean valid = true;

    private FluidPipeNetwork(
            Level level,
            FluidPipeBlockEntity[] pipes,
            Map<FluidPipeBlockEntity, Integer> index,
            int[] pipeChunks,
            BlockPos[] chunkPositions) {
        this.level = level;
        this.pipes = pipes;
        this.index = index;
        this.pipeChunks = pipeChunks;
        this.chunkPositions = chunkPositions;
        this.checkedChunks = new long[chunkPositions.length];
        membership.network = this;
    }

    /** Breadth-first over connected, loaded pipes from {@code start}. */
    static FluidPipeNetwork build(Level level, FluidPipeBlockEntity start) {
        List<FluidPipeBlockEntity> found = new ArrayList<>();
        Map<FluidPipeBlockEntity, Integer> index = new IdentityHashMap<>();
        ArrayDeque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
        index.put(start, 0);
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            FluidPipeBlockEntity pipe = queue.poll();
            for (Direction side : DIRECTIONS) {
                if (!connected(pipe, side)) continue;
                BlockPos next = pipe.getBlockPos().relative(side);
                if (!level.hasChunkAt(next)) continue;
                if (!(level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbor)
                        || neighbor.isRemoved()
                        || !connected(neighbor, side.getOpposite())
                        || index.containsKey(neighbor)) continue;
                index.put(neighbor, found.size());
                found.add(neighbor);
                queue.add(neighbor);
            }
        }

        FluidPipeBlockEntity[] pipes = found.toArray(new FluidPipeBlockEntity[0]);
        Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
        chunks.defaultReturnValue(-1);
        List<BlockPos> chunkPositions = new ArrayList<>();
        int[] pipeChunks = new int[pipes.length];
        for (int i = 0; i < pipes.length; i++) {
            pipeChunks[i] = chunkIndex(chunks, chunkPositions, pipes[i].getBlockPos());
        }

        FluidPipeNetwork network =
                new FluidPipeNetwork(level, pipes, index, pipeChunks, chunkPositions.toArray(new BlockPos[0]));
        for (FluidPipeBlockEntity pipe : pipes) {
            FluidPipeNetwork previous = pipe.network();
            if (previous != null) previous.invalidate();
        }
        for (FluidPipeBlockEntity pipe : pipes) pipe.membership(network.membership);
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

    private static boolean connected(FluidPipeBlockEntity pipe, Direction side) {
        return !pipe.isRemoved() && pipe.getBlockState().getValue(FluidPipeBlock.propFor(side));
    }

    public boolean valid() {
        return valid;
    }

    public boolean contains(FluidPipeBlockEntity pipe) {
        return index.containsKey(pipe);
    }

    public int size() {
        return pipes.length;
    }

    public FluidPipeBlockEntity pipe(int index) {
        return pipes[index];
    }

    /** Start one pool pass; chunk liveness is checked once per touched chunk per pass. */
    public void beginPass() {
        availabilityEpoch++;
    }

    public boolean live(int pipe) {
        if (!valid) return false;
        if (pipes[pipe].isRemoved()) {
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

    public void invalidate() {
        if (!valid) return;
        valid = false;
        membership.network = null;
    }
}
