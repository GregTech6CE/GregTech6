package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * GT6's 3x3x4 turbines and dynamos can point along any of the six axes.
 * Local {@code back} always grows into the machine, away from its main housing.
 */
public final class AxialStructureTransform {
    private AxialStructureTransform() {}

    public static BlockPos at(BlockPos controller, Direction front, int right, int up, int back) {
        Direction width = front.getAxis().isVertical() ? Direction.EAST : front.getClockWise();
        Direction height = front.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
        return controller.relative(width, right).relative(height, up).relative(front.getOpposite(), back);
    }

    /** The fluid exits from the structure's lowest world-Y layer, including vertical builds. */
    public static boolean isLowestLayer(Direction front, int up, int back) {
        return switch (front) {
            case UP -> back == 3;
            case DOWN -> back == 0;
            default -> up == -1;
        };
    }

    public static MultiblockLayout.Role fluidRole(Direction front, int up, int back) {
        if (back == 0) return isLowestLayer(front, up, back)
                ? MultiblockLayout.Role.FLUID_IO : MultiblockLayout.Role.FLUID_INPUT;
        return isLowestLayer(front, up, back)
                ? MultiblockLayout.Role.FLUID_OUTPUT : MultiblockLayout.Role.CASING;
    }

    /** Rear-centre is the RU/EU output; fluid ports otherwise follow the lowest world layer. */
    public static MultiblockLayout.Role role(Direction front, int right, int up, int back, boolean fluids) {
        if (right == 0 && up == 0 && back == 3) return MultiblockLayout.Role.ENERGY_OUTPUT;
        return fluids ? fluidRole(front, up, back) : MultiblockLayout.Role.CASING;
    }
}
