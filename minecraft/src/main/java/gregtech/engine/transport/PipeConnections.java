package gregtech.engine.transport;

import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Original canonical six connector face names, independent of electrical block registration. */
public final class PipeConnections {
    public static final BooleanProperty DOWN = BooleanProperty.create("down"),
            UP = BooleanProperty.create("up"),
            NORTH = BooleanProperty.create("north"),
            SOUTH = BooleanProperty.create("south"),
            WEST = BooleanProperty.create("west"),
            EAST = BooleanProperty.create("east");
    public static final BooleanProperty[] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    private PipeConnections() {}

    public static BooleanProperty propFor(Direction direction) {
        return CONNECTIONS[direction.ordinal()];
    }

    public static int activeFaces(BlockState state, Function<Direction, BooleanProperty> properties) {
        int mask = 0;
        for (Direction side : Direction.values())
            if (state.getValue(properties.apply(side))) mask |= 1 << side.ordinal();
        return mask;
    }
}
