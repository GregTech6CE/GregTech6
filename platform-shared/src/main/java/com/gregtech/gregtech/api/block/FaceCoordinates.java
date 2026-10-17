package com.gregtech.gregtech.api.block;

import net.minecraft.core.Direction;

/** Texture-space coordinates, in pixels, viewed from outside any of the six block faces. */
public final class FaceCoordinates {
    public record Point(double x, double y) {}
    private FaceCoordinates() {}
    public static Point pixels(Direction face, double x, double y, double z) {
        return switch (face) {
            case NORTH -> new Point(16*(1-x),16*(1-y));
            case SOUTH -> new Point(16*x,16*(1-y));
            case WEST -> new Point(16*z,16*(1-y));
            case EAST -> new Point(16*(1-z),16*(1-y));
            case UP -> new Point(16*x,16*z);
            case DOWN -> new Point(16*x,16*(1-z));
        };
    }
}
