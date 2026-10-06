package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.block.machine.MachineRotationType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Immutable, side-independent declaration; contains no renderer or world references. */
public record ToolInteractionSpec(DirectionProperty facing, MachineRotationType rotation,
                                  ConnectionKind connection) {
    public enum ConnectionKind {
        FLUID, ITEM, ELECTRIC, AXLE, LASER, REDSTONE, LOGISTICS;
        public BooleanProperty property(Direction side) {
            return switch (this) {
                case FLUID -> com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(side);
                case ITEM -> com.gregtech.gregtech.block.machine.ItemPipeBlock.propFor(side);
                case ELECTRIC, LASER, REDSTONE -> com.gregtech.gregtech.block.energy.ElectricWireBlock.propFor(side);
                case LOGISTICS -> com.gregtech.gregtech.api.transport.PipeConnections.propFor(side);
                case AXLE -> com.gregtech.gregtech.block.energy.AxleBlock.propFor(side);
            };
        }
    }
    public static ToolInteractionSpec facing(DirectionProperty property, MachineRotationType policy) {
        return new ToolInteractionSpec(property, policy, null);
    }
    public static ToolInteractionSpec connections(ConnectionKind kind) {
        return new ToolInteractionSpec(null, null, kind);
    }
    public boolean allows(Direction side) {
        return connection != null || (rotation.isValid(side) && facing.getPossibleValues().contains(side));
    }
    public boolean allows(BlockState state, Direction side) {
        return allows(side) && (connection != ConnectionKind.AXLE
                || com.gregtech.gregtech.block.energy.AxleBlock.isValidConnectionState(state.cycle(connection.property(side))));
    }
    public int activeFaces(BlockState state) {
        if (connection == null) return 1 << state.getValue(facing).ordinal();
        int mask = 0;
        for (Direction side : Direction.values()) if (state.getValue(connection.property(side))) mask |= 1 << side.ordinal();
        return mask;
    }
}
