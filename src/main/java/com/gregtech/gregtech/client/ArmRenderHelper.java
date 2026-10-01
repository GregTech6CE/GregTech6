package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Shared vertex utilities for dynamic cuboid rendering (cover plates etc.). */
final class ArmRenderHelper {

    private ArmRenderHelper() {}

    // --- UV helpers: map world coordinate to atlas UV, clamped to [0,1] so extended arms repeat edge texels ---

    /** Standard: w increases → U increases. */
    private static float u(float w, float u0, float u1) {
        float t = w < 0.0f ? 0.0f : w > 1.0f ? 1.0f : w;
        return u0 + t * (u1 - u0);
    }

    /** Inverted U: w increases → U decreases (south/east faces per Minecraft auto-UV). */
    private static float ui(float w, float u0, float u1) {
        float t = w < 0.0f ? 0.0f : w > 1.0f ? 1.0f : w;
        return u1 - t * (u1 - u0);
    }

    /** Standard V: w increases → V increases (UP face Z→v). */
    private static float v(float w, float v0, float v1) {
        float t = w < 0.0f ? 0.0f : w > 1.0f ? 1.0f : w;
        return v0 + t * (v1 - v0);
    }

    /** Inverted V: w increases → V decreases. Minecraft auto-UV inverts Y→v on vertical faces
     *  and Z→v on the DOWN face (atlas V=0 is top of texture, but fromY maps to 16-toY). */
    private static float vi(float w, float v0, float v1) {
        float t = w < 0.0f ? 0.0f : w > 1.0f ? 1.0f : w;
        return v0 + (v1 - v0) * (1.0f - t);
    }

    // --- Classic Minecraft face-brightness factors ---

    private static float shade(float nx, float ny, float nz) {
        if (ny > 0.5f) return 1.0f;
        if (ny < -0.5f) return 0.5f;
        if (Math.abs(nx) > 0.5f) return 0.6f;
        return 0.8f;
    }

    /** Render a cuboid with position-based UV mapping and directional face shading. */
    static void drawCuboid(Matrix4f mat, VertexConsumer vc,
                           float x0, float y0, float z0, float x1, float y1, float z1,
                           float r, float g, float b,
                           TextureAtlasSprite sprite,
                           int packedLight) {
        drawCuboid(mat, vc, x0, y0, z0, x1, y1, z1, r, g, b, sprite, packedLight, null);
    }

    /**
     * Render a cuboid, optionally omitting both faces on {@code skipAxis}. Arm
     * extensions sit flush between our static arm cap and the neighbor's core
     * face — drawing their own end caps would z-fight with both.
     */
    static void drawCuboid(Matrix4f mat, VertexConsumer vc,
                           float x0, float y0, float z0, float x1, float y1, float z1,
                           float r, float g, float b,
                           TextureAtlasSprite sprite,
                           int packedLight,
                           @javax.annotation.Nullable Direction.Axis skipAxis) {
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        int overlay = OverlayTexture.NO_OVERLAY;

        // -Y (down): X→u, Z→vi, CCW from below
        if (skipAxis != Direction.Axis.Y) {
            float s = shade(0, -1, 0), sr = r * s, sg = g * s, sb = b * s;
            vertex(vc, mat, x0, y0, z0, u(x0,u0,u1), vi(z0,v0,v1), sr,sg,sb, overlay, packedLight,  0,-1, 0);
            vertex(vc, mat, x1, y0, z0, u(x1,u0,u1), vi(z0,v0,v1), sr,sg,sb, overlay, packedLight,  0,-1, 0);
            vertex(vc, mat, x1, y0, z1, u(x1,u0,u1), vi(z1,v0,v1), sr,sg,sb, overlay, packedLight,  0,-1, 0);
            vertex(vc, mat, x0, y0, z1, u(x0,u0,u1), vi(z1,v0,v1), sr,sg,sb, overlay, packedLight,  0,-1, 0);
        }

        // +Y (up): X→u, Z→v, CCW from above
        if (skipAxis != Direction.Axis.Y) {
            float s = shade(0, 1, 0), sr = r * s, sg = g * s, sb = b * s;
            vertex(vc, mat, x0, y1, z0, u(x0,u0,u1), v(z0,v0,v1), sr,sg,sb, overlay, packedLight,  0, 1, 0);
            vertex(vc, mat, x0, y1, z1, u(x0,u0,u1), v(z1,v0,v1), sr,sg,sb, overlay, packedLight,  0, 1, 0);
            vertex(vc, mat, x1, y1, z1, u(x1,u0,u1), v(z1,v0,v1), sr,sg,sb, overlay, packedLight,  0, 1, 0);
            vertex(vc, mat, x1, y1, z0, u(x1,u0,u1), v(z0,v0,v1), sr,sg,sb, overlay, packedLight,  0, 1, 0);
        }

        // -Z (north): X→u, Y→vi, CCW from -Z
        if (skipAxis != Direction.Axis.Z) {
            float s = shade(0, 0, -1), sr = r * s, sg = g * s, sb = b * s;
            vertex(vc, mat, x0, y0, z0, u(x0,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0,-1);
            vertex(vc, mat, x0, y1, z0, u(x0,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0,-1);
            vertex(vc, mat, x1, y1, z0, u(x1,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0,-1);
            vertex(vc, mat, x1, y0, z0, u(x1,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0,-1);
        }

        // +Z (south): X→ui, Y→vi, CCW from +Z
        if (skipAxis != Direction.Axis.Z) {
            float s = shade(0, 0, 1), sr = r * s, sg = g * s, sb = b * s;
            vertex(vc, mat, x0, y0, z1, ui(x0,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0, 1);
            vertex(vc, mat, x1, y0, z1, ui(x1,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0, 1);
            vertex(vc, mat, x1, y1, z1, ui(x1,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0, 1);
            vertex(vc, mat, x0, y1, z1, ui(x0,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight,  0, 0, 1);
        }

        // -X (west): Z→u, Y→vi, CCW from -X
        if (skipAxis != Direction.Axis.X) {
            float s = shade(-1, 0, 0), sr = r * s, sg = g * s, sb = b * s;
            vertex(vc, mat, x0, y0, z0, u(z0,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight, -1, 0, 0);
            vertex(vc, mat, x0, y0, z1, u(z1,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight, -1, 0, 0);
            vertex(vc, mat, x0, y1, z1, u(z1,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight, -1, 0, 0);
            vertex(vc, mat, x0, y1, z0, u(z0,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight, -1, 0, 0);
        }

        // +X (east): Z→ui, Y→vi, CCW from +X
        if (skipAxis != Direction.Axis.X) {
            float s = shade(1, 0, 0), sr = r * s, sg = g * s, sb = b * s;
            vertex(vc, mat, x1, y0, z0, ui(z0,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight,  1, 0, 0);
            vertex(vc, mat, x1, y1, z0, ui(z0,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight,  1, 0, 0);
            vertex(vc, mat, x1, y1, z1, ui(z1,u0,u1), vi(y1,v0,v1), sr,sg,sb, overlay, packedLight,  1, 0, 0);
            vertex(vc, mat, x1, y0, z1, ui(z1,u0,u1), vi(y0,v0,v1), sr,sg,sb, overlay, packedLight,  1, 0, 0);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z, float u, float v,
                               float r, float g, float b,
                               int overlay, int packedLight,
                               float nx, float ny, float nz) {
        vc.vertex(mat, x, y, z).color(r, g, b, 1).uv(u, v)
                .overlayCoords(overlay).uv2(packedLight).normal(nx, ny, nz).endVertex();
    }

    static TextureAtlasSprite getSprite(ResourceLocation id) {
        return net.minecraft.client.Minecraft.getInstance()
                .getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(id);
    }
}
