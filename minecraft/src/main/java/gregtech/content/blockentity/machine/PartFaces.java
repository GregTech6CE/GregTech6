package gregtech.content.blockentity.machine;

import gregtech.api.machine.MachineFace;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Part faces relative to {@link HorizontalDirectionalBlock#FACING}. Left and right match an energy part. */
public final class PartFaces {
    private PartFaces() {}

    public static Direction facing(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    public static Direction world(Direction facing, MachineFace face) {
        return switch (face) {
            case BOTTOM -> Direction.DOWN;
            case TOP -> Direction.UP;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
        };
    }

    public static MachineFace relative(Direction facing, Direction side) {
        if (side == null || facing == null) return null;
        if (side == Direction.DOWN) return MachineFace.BOTTOM;
        if (side == Direction.UP) return MachineFace.TOP;
        if (side == facing) return MachineFace.FRONT;
        if (side == facing.getOpposite()) return MachineFace.BACK;
        if (side == facing.getClockWise()) return MachineFace.LEFT;
        if (side == facing.getCounterClockWise()) return MachineFace.RIGHT;
        return null;
    }
}
