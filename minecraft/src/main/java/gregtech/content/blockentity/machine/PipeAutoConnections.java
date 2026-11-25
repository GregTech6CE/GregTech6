package gregtech.content.blockentity.machine;

import gregtech.content.cover.CoverConnections;
import gregtech.content.transport.PipeConnectionRules;
import gregtech.content.transport.PipeConnectionRules.Action;
import gregtech.content.transport.PipeConnectionRules.Neighbor;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/** Observes loaded neighbors and applies automatic connections using each pipe family's own ports. */
record PipeAutoConnections<T extends BlockEntity>(
        Class<T> pipeType,
        Function<Direction, BooleanProperty> property,
        BiPredicate<T, Direction> manuallyDisconnected,
        BiPredicate<T, Direction> declaredEndpoint,
        BiPredicate<T, Direction> capabilityEndpoint) {

    int connectionMask(BlockState state) {
        int mask = 0;
        for (Direction side : Direction.values()) {
            BooleanProperty bit = property.apply(side);
            if (state.hasProperty(bit) && state.getValue(bit)) mask |= 1 << side.ordinal();
        }
        return mask;
    }

    void onPlace(T pipe, @Nullable Direction clickedFace) {
        Level level = pipe.getLevel();
        if (level == null || level.isClientSide) return;
        if (clickedFace != null) connect(level, pipe, clickedFace.getOpposite(), true);
        for (Direction side : Direction.values()) connect(level, pipe, side, false);
    }

    boolean tryConnect(T pipe, Direction side) {
        Level level = pipe.getLevel();
        return level != null && !level.isClientSide && connect(level, pipe, side, false);
    }

    private boolean connect(Level level, T pipe, Direction side, boolean clickedSide) {
        BlockPos pos = pipe.getBlockPos();
        BlockPos adjacent = pos.relative(side);
        if (manuallyDisconnected.test(pipe, side)
                || !level.hasChunkAt(adjacent)
                || !CoverConnections.canConnect(level, pos, side)) return false;

        Neighbor neighbor = neighbor(level, pipe, adjacent, side, clickedSide);
        Action action = PipeConnectionRules.decide(clickedSide, neighbor);
        if (action == Action.NONE) return false;

        boolean changed = open(level, pos, side);
        if (action == Action.OPEN_BOTH && pipeType.isInstance(level.getBlockEntity(adjacent))) {
            open(level, adjacent, side.getOpposite());
        }
        return changed;
    }

    private Neighbor neighbor(Level level, T pipe, BlockPos adjacent, Direction side, boolean clickedSide) {
        BlockEntity entity = level.getBlockEntity(adjacent);
        if (pipeType.isInstance(entity)) {
            Direction otherSide = side.getOpposite();
            if (manuallyDisconnected.test(pipeType.cast(entity), otherSide)) return Neighbor.LOCKED_PIPE;
            return level.getBlockState(adjacent).getValue(property.apply(otherSide))
                    ? Neighbor.OPEN_PIPE
                    : Neighbor.CLOSED_PIPE;
        }
        if (declaredEndpoint.test(pipe, side)) return Neighbor.DECLARED_ENDPOINT;
        // A capability can initialize a third-party handler. Probe it only on a placement click.
        if (clickedSide && capabilityEndpoint.test(pipe, side)) return Neighbor.CAPABILITY_ENDPOINT;
        return Neighbor.NONE;
    }

    private boolean open(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BooleanProperty bit = property.apply(side);
        if (state.getValue(bit)) return false;
        level.setBlockAndUpdate(pos, state.setValue(bit, true));
        return true;
    }
}
