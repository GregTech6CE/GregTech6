package com.gregtech.gregtech.jei;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** The preview rectangle is local to the recipe; GUI scissors use absolute GUI coordinates. */
public final class PreviewViewport {
    public static final int LEFT = 0, TOP = 20, RIGHT = 176, BOTTOM = 128;

    private PreviewViewport() {}

    public record Bounds(int left, int top, int right, int bottom) {}

    public static Bounds screenBounds(Matrix4fc recipePose) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        var corner = new Vector3f();
        for (int x : new int[]{LEFT, RIGHT}) {
            for (int y : new int[]{TOP, BOTTOM}) {
                recipePose.transformPosition(corner.set(x, y, 0));
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
            }
        }
        // GuiGraphics applies the window's GUI scale itself; do not multiply by it here.
        return new Bounds((int)Math.floor(minX), (int)Math.floor(minY),
                (int)Math.ceil(maxX), (int)Math.ceil(maxY));
    }
}
