package com.gregtech.gregtech.api.transport;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
/** Original canonical six connector face names, independent of electrical block registration. */
public final class PipeConnections {
    public static final BooleanProperty DOWN=BooleanProperty.create("down"),UP=BooleanProperty.create("up"),NORTH=BooleanProperty.create("north"),SOUTH=BooleanProperty.create("south"),WEST=BooleanProperty.create("west"),EAST=BooleanProperty.create("east");
    public static final BooleanProperty[] CONNECTIONS={DOWN,UP,NORTH,SOUTH,WEST,EAST};
    private PipeConnections() {}
    public static BooleanProperty propFor(Direction direction) { return CONNECTIONS[direction.ordinal()]; }
}
