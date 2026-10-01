package com.gregtech.gregtech.jei;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

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

    @Override public void addVertex(float x, float y, float z, int color,
                                    float u, float v, int overlay, int light, float nx, float ny, float nz) {
        float shade = brightness(nx, ny, nz);
        int shaded=(color & 0xFF000000) | ((int)(((color >>> 16) & 255)*shade) << 16)
                | ((int)(((color >>> 8) & 255)*shade) << 8) | (int)((color & 255)*shade);
        parent.addVertex(x, y, z, shaded, u, v, overlay, light, 0, 1, 0);
    }

    // Custom item renderers using the chained API receive neutral illumination as well.
    @Override public VertexConsumer setNormal(float x, float y, float z) {
        parent.setNormal(0, 1, 0);
        return this;
    }
}
