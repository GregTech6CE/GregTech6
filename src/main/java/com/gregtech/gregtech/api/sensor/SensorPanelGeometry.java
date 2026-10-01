package com.gregtech.gregtech.api.sensor;

import net.minecraft.core.Direction;

/** GT6 panel is two pixels thick, attached to the block opposite its display. */
public final class SensorPanelGeometry {
    private SensorPanelGeometry() {}
    public static int[] bounds(Direction facing) {
        int[] bounds = {0, 0, 0, 16, 16, 16};
        int axis = facing.getAxis() == Direction.Axis.X ? 0 : facing.getAxis() == Direction.Axis.Y ? 1 : 2;
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) bounds[axis] = 14;
        else bounds[axis + 3] = 2;
        return bounds;
    }
}
