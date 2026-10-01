package com.gregtech.gregtech.jei;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Exercises the actual scissor calculation called before the model camera transform. No GPU needed. */
public final class PreviewViewportContracts {
    public static void main(String[] args) {
        expect(new Matrix4f(), 0, 20, 176, 128);
        // JEI offsets each recipe on screen. The former local scissor discards this entire preview.
        var translated = new Matrix4f().translation(240, 100, 0);
        var bounds = PreviewViewport.screenBounds(translated);
        var center = translated.transformPosition(new Vector3f(88, 77, 150));
        if (center.x < bounds.left() || center.x >= bounds.right()
                || center.y < bounds.top() || center.y >= bounds.bottom())
            throw new AssertionError("JEI recipe offset clips the whole structure: center=" + center + " scissor=" + bounds);
        expect(translated, 240, 120, 416, 228);
        expect(new Matrix4f().translation(240, 320, 0), 240, 340, 416, 448);
        expect(new Matrix4f().translation(13.25f, 8.75f, 0).scale(1.5f, 1.5f, 1), 13, 38, 278, 201);
        expect(new Matrix4f().translation(240, 100, 0).scale(-1, 1, 1), 64, 120, 240, 228);
        expect(new Matrix4f().translation(240, 100, 0).rotateZ((float)Math.PI / 2), 112, 100, 220, 276);
        System.out.println("PASS preview viewport: JEI offsets, multiple rows, scaling and mirrored poses");
    }

    private static void expect(Matrix4f pose, int left, int top, int right, int bottom) {
        var actual = PreviewViewport.screenBounds(pose);
        if (!actual.equals(new PreviewViewport.Bounds(left, top, right, bottom)))
            throw new AssertionError("Wrong viewport: " + actual);
    }
}
