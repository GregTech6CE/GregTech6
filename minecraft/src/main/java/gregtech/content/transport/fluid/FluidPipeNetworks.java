package gregtech.content.transport.fluid;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** On-demand access to the fluid pipe snapshot used by the shared-pool mode. */
public final class FluidPipeNetworks {
    private FluidPipeNetworks() {}

    /** The current snapshot containing {@code pipe}, built on demand. */
    public static FluidPipeNetwork networkOf(FluidPipeBlockEntity pipe) {
        Level level = pipe.getLevel();
        if (level == null || level.isClientSide || pipe.isRemoved()) return null;
        FluidPipeNetwork network = pipe.network();
        if (network != null && network.valid() && network.contains(pipe)) return network;
        return FluidPipeNetwork.build(level, pipe);
    }

    /** The layout around {@code pipe} changed: its snapshot and adjacent ones are stale. */
    public static void invalidate(FluidPipeBlockEntity pipe) {
        FluidPipeNetwork network = pipe.network();
        if (network != null) network.invalidate();
        Level level = pipe.getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos origin = pipe.getBlockPos();
        for (Direction face : Direction.values()) {
            BlockPos next = origin.relative(face);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbor) {
                FluidPipeNetwork adjacent = neighbor.network();
                if (adjacent != null) adjacent.invalidate();
            }
        }
    }

    public static void left(FluidPipeBlockEntity pipe) {
        invalidate(pipe);
    }
}
