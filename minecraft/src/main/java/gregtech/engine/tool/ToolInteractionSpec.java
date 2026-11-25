package gregtech.engine.tool;

import gregtech.engine.energy.AxleConnectionRules;
import gregtech.engine.transport.PipeConnections;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Immutable, side-independent declaration; contains no renderer or world references. */
public record ToolInteractionSpec(
        DirectionProperty facing,
        MachineRotationType rotation,
        ConnectionKind connection,
        Function<Direction, BooleanProperty> connectionProperties) {
    public enum ConnectionKind {
        FLUID,
        ITEM,
        ELECTRIC,
        AXLE,
        LASER,
        REDSTONE,
        LOGISTICS
    }

    public static ToolInteractionSpec facing(DirectionProperty property, MachineRotationType policy) {
        return new ToolInteractionSpec(property, policy, null, null);
    }

    public static ToolInteractionSpec connections(
            ConnectionKind kind, Function<Direction, BooleanProperty> properties) {
        return new ToolInteractionSpec(null, null, kind, properties);
    }

    BooleanProperty connectionProperty(Direction side) {
        return connectionProperties.apply(side);
    }

    public boolean allows(Direction side) {
        return connection != null
                || (rotation.isValid(side) && facing.getPossibleValues().contains(side));
    }

    public boolean allows(BlockState state, Direction side) {
        if (connection != ConnectionKind.AXLE) return allows(side);
        return allowsConnection(state, side, !state.getValue(connectionProperty(side)));
    }

    boolean allowsConnection(BlockState state, Direction side, boolean connected) {
        return connection != ConnectionKind.AXLE
                || AxleConnectionRules.valid(activeFaces(state.setValue(connectionProperty(side), connected)));
    }

    public int activeFaces(BlockState state) {
        if (connection == null) return 1 << state.getValue(facing).ordinal();
        return PipeConnections.activeFaces(state, connectionProperties);
    }
}
