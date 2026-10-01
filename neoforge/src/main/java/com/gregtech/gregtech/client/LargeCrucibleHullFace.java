package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;

/** Whole-vessel face emission, separated so UV orientation is testable without a GPU. */
final class LargeCrucibleHullFace {
    private LargeCrucibleHullFace() {}
    static void draw(PoseStack.Pose pose, VertexConsumer out, Direction face,
                     float x0, float y0, float z0, float x1, float y1, float z1,
                     float u0, float u1, float v0, float v1, int rgb, float shade, int light) {
        float[] vertices = switch (face) {
                case DOWN -> new float[]{x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1};
                case UP -> new float[]{x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0};
                case NORTH -> new float[]{x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0};
                case SOUTH -> new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1};
                case WEST -> new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0};
                case EAST -> new float[]{x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1};
            };
                for (int i = 0; i < 4; i++) {
                    // Normalize within this wall, not the world: a vessel spans three blocks.
                    float x = (vertices[i*3] - x0) / (x1 - x0);
                    float y = (vertices[i*3+1] - y0) / (y1 - y0);
                    float z = (vertices[i*3+2] - z0) / (z1 - z0);
                    float horizontal = switch (face) {
                        case NORTH -> 1-x;
                        case SOUTH -> x;
                        case WEST -> z;
                        case EAST -> 1-z;
                        default -> x;
                    };
                    float vertical = switch (face) {
                        case UP -> z;
                        case DOWN -> 1-z;
                        default -> 1-y;
                    };
                    float u = u0 + horizontal * (u1-u0);
                    float v = v0 + vertical * (v1-v0);
                    out.addVertex(pose.pose(), vertices[i*3], vertices[i*3+1], vertices[i*3+2])
                            .setColor(((rgb>>16)&255)/255F*shade, ((rgb>>8)&255)/255F*shade, (rgb&255)/255F*shade, 1)
                            .setUv(u,v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(pose,face.getStepX(),face.getStepY(),face.getStepZ());
                }
    }
}
