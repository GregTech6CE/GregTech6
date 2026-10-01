package com.gregtech.gregtech.api.block;

import net.minecraft.core.Direction;
import org.joml.Quaternionf;

/** Rotates a display whose front normal is +Z into the outward block face. */
public final class DisplayFaceOrientation {
    private DisplayFaceOrientation() {}
    public static Quaternionf rotation(Direction face) {
        return switch (face) {
            case UP -> new Quaternionf().rotationX(-(float)Math.PI / 2);
            case DOWN -> new Quaternionf().rotationX((float)Math.PI / 2);
            // Direction yaw describes the camera convention; our quad starts with a +Z normal.
            default -> new Quaternionf().rotationY(-(float)Math.toRadians(face.toYRot()));
        };
    }
}
