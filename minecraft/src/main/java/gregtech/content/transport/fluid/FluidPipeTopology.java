package gregtech.content.transport.fluid;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Members of one physical fluid pipe component, in breadth-first order from the requested pipe. */
public final class FluidPipeTopology {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final List<FluidPipeBlockEntity> pipes = new ArrayList<>();
    private final Map<FluidPipeBlockEntity, Integer> indices = new IdentityHashMap<>();

    private FluidPipeTopology() {}

    public static FluidPipeTopology scan(Level level, FluidPipeBlockEntity start) {
        FluidPipeTopology topology = new FluidPipeTopology();
        BlockPos pos = start.getBlockPos();
        if (start.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != start) return topology;

        topology.add(start);
        for (int i = 0; i < topology.size(); i++) {
            FluidPipeBlockEntity pipe = topology.pipe(i);
            for (Direction side : DIRECTIONS) {
                FluidPipeBlockEntity neighbor = connectedNeighbor(level, pipe, side);
                if (neighbor != null && !topology.indices.containsKey(neighbor)) topology.add(neighbor);
            }
        }
        return topology;
    }

    /** Both ends must declare this face before the neighbor can be visited. */
    public static @Nullable FluidPipeBlockEntity connectedNeighbor(
            Level level, FluidPipeBlockEntity pipe, Direction side) {
        if (!pipe.connected(side)) return null;
        BlockPos next = pipe.getBlockPos().relative(side);
        if (!level.hasChunkAt(next)) return null;
        return level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbor
                        && neighbor.connected(side.getOpposite())
                ? neighbor
                : null;
    }

    private void add(FluidPipeBlockEntity pipe) {
        indices.put(pipe, pipes.size());
        pipes.add(pipe);
    }

    public int size() {
        return pipes.size();
    }

    public FluidPipeBlockEntity pipe(int index) {
        return pipes.get(index);
    }

    public int indexOf(FluidPipeBlockEntity pipe) {
        return indices.getOrDefault(pipe, -1);
    }
}
