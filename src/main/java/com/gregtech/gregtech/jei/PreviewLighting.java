package com.gregtech.gregtech.jei;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;

/** Preview-only ambient fill: retain face contrast without the inventory shader's dark back faces. */
public final class PreviewLighting extends VertexConsumerWrapper {
    public PreviewLighting(VertexConsumer parent) { super(parent); }

    public static float brightness(float x, float y, float z) {
        double length = Math.sqrt((double)x*x + (double)y*y + (double)z*z);
        if (!Double.isFinite(length) || length == 0) return 1;
        // Camera-space key light from above-left, with enough ambient fill for every face.
        double dot = (-.35*x - .65*y + .68*z) / (length * Math.sqrt(.35*.35 + .65*.65 + .68*.68));
        return (float)(.75 + .25 * Math.max(0, Math.min(1, dot)));
    }

    @Override public void vertex(float x, float y, float z, float r, float g, float b, float a,
                                 float u, float v, int overlay, int light, float nx, float ny, float nz) {
        float shade = brightness(nx, ny, nz);
        parent.vertex(x, y, z, r*shade, g*shade, b*shade, a, u, v, overlay, light, 0, 1, 0);
    }

    // Custom item renderers using the chained API receive neutral illumination as well.
    @Override public VertexConsumer normal(float x, float y, float z) {
        parent.normal(0, 1, 0);
        return this;
    }
}
