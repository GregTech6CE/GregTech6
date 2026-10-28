package gregtech.engine.machine;

import gregtech.engine.energy.FaceConfig;
import net.minecraft.core.Direction;

/** Conversion between machine-relative face indices and absolute world directions. */
public final class MachineDirections {
    private MachineDirections() {}

    /** Convert absolute side to TOP/BOTTOM/LEFT/RIGHT/FRONT/BACK using the block facing. */
    public static int relative(Direction facing, Direction side) {
        if (side == Direction.DOWN) return FaceConfig.BOTTOM;
        if (side == Direction.UP) return FaceConfig.TOP;
        return switch (facing) {
            case NORTH ->
                switch (side) {
                    case NORTH -> FaceConfig.FRONT;
                    case SOUTH -> FaceConfig.BACK;
                    case WEST -> FaceConfig.RIGHT;
                    case EAST -> FaceConfig.LEFT;
                    default -> FaceConfig.LEFT;
                };
            case SOUTH ->
                switch (side) {
                    case NORTH -> FaceConfig.BACK;
                    case SOUTH -> FaceConfig.FRONT;
                    case WEST -> FaceConfig.LEFT;
                    case EAST -> FaceConfig.RIGHT;
                    default -> FaceConfig.LEFT;
                };
            case WEST ->
                switch (side) {
                    case NORTH -> FaceConfig.LEFT;
                    case SOUTH -> FaceConfig.RIGHT;
                    case WEST -> FaceConfig.FRONT;
                    case EAST -> FaceConfig.BACK;
                    default -> FaceConfig.LEFT;
                };
            case EAST ->
                switch (side) {
                    case NORTH -> FaceConfig.RIGHT;
                    case SOUTH -> FaceConfig.LEFT;
                    case WEST -> FaceConfig.BACK;
                    case EAST -> FaceConfig.FRONT;
                    default -> FaceConfig.LEFT;
                };
            default -> side.get3DDataValue();
        };
    }

    /** Convert a machine-relative face index to the absolute world direction. */
    public static Direction absolute(Direction facing, int relDir) {
        if (relDir == FaceConfig.BOTTOM) return Direction.DOWN;
        if (relDir == FaceConfig.TOP) return Direction.UP;
        return switch (facing) {
            case NORTH ->
                switch (relDir) {
                    case FaceConfig.LEFT -> Direction.EAST;
                    case FaceConfig.FRONT -> Direction.NORTH;
                    case FaceConfig.RIGHT -> Direction.WEST;
                    case FaceConfig.BACK -> Direction.SOUTH;
                    default -> Direction.NORTH;
                };
            case SOUTH ->
                switch (relDir) {
                    case FaceConfig.LEFT -> Direction.WEST;
                    case FaceConfig.FRONT -> Direction.SOUTH;
                    case FaceConfig.RIGHT -> Direction.EAST;
                    case FaceConfig.BACK -> Direction.NORTH;
                    default -> Direction.NORTH;
                };
            case WEST ->
                switch (relDir) {
                    case FaceConfig.LEFT -> Direction.NORTH;
                    case FaceConfig.FRONT -> Direction.WEST;
                    case FaceConfig.RIGHT -> Direction.SOUTH;
                    case FaceConfig.BACK -> Direction.EAST;
                    default -> Direction.NORTH;
                };
            case EAST ->
                switch (relDir) {
                    case FaceConfig.LEFT -> Direction.SOUTH;
                    case FaceConfig.FRONT -> Direction.EAST;
                    case FaceConfig.RIGHT -> Direction.NORTH;
                    case FaceConfig.BACK -> Direction.WEST;
                    default -> Direction.NORTH;
                };
            default -> Direction.NORTH;
        };
    }
}
