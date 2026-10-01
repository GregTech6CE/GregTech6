package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Solid cuboids for placed workpieces, with entity-format vertices and pose-space normals. */
final class AnvilCuboidRenderer {
    private AnvilCuboidRenderer() {}
    static void drawSurface(PoseStack.Pose pose, VertexConsumer out, AABB b, int rgb, int light, boolean horizontal) {
        float x0=(float)b.minX,x1=(float)b.maxX,y0=(float)b.minY,y1=(float)b.maxY,z0=(float)b.minZ,z1=(float)b.maxZ;
        if (horizontal) {
            face(pose,out,0,1,0,1,rgb,light,Direction.DOWN,new float[]{x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1});
            face(pose,out,0,1,0,1,rgb,light,Direction.UP,new float[]{x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0});
        } else {
            face(pose,out,0,1,0,1,rgb,light,Direction.NORTH,new float[]{x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0});
            face(pose,out,0,1,0,1,rgb,light,Direction.SOUTH,new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1});
            face(pose,out,0,1,0,1,rgb,light,Direction.WEST,new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0});
            face(pose,out,0,1,0,1,rgb,light,Direction.EAST,new float[]{x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1});
        }
    }
    static void draw(PoseStack.Pose pose, VertexConsumer out, AABB b, TextureAtlasSprite sprite, int rgb, int light) {
        draw(pose,out,b,sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1(),rgb,light);
    }
    static void draw(PoseStack.Pose pose, VertexConsumer out, AABB b, float u0,float u1,float v0,float v1,int rgb,int light) {
        float x0=(float)b.minX,x1=(float)b.maxX,y0=(float)b.minY,y1=(float)b.maxY,z0=(float)b.minZ,z1=(float)b.maxZ;
        face(pose,out,u0,u1,v0,v1,rgb,light,Direction.DOWN,new float[]{x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1});
        face(pose,out,u0,u1,v0,v1,rgb,light,Direction.UP,new float[]{x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0});
        face(pose,out,u0,u1,v0,v1,rgb,light,Direction.NORTH,new float[]{x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0});
        face(pose,out,u0,u1,v0,v1,rgb,light,Direction.SOUTH,new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1});
        face(pose,out,u0,u1,v0,v1,rgb,light,Direction.WEST,new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0});
        face(pose,out,u0,u1,v0,v1,rgb,light,Direction.EAST,new float[]{x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1});
    }
    static void face(PoseStack.Pose pose, VertexConsumer out, float u0,float u1,float v0,float v1, int rgb, int light, Direction side, float[] vertices) {
        for(int i=0;i<12;i+=3) {
            float x=vertices[i],y=vertices[i+1],z=vertices[i+2];
            float u=switch(side) {case WEST->z;case EAST->1-z;case SOUTH->1-x;default->x;};
            float v=side==Direction.UP?z:side==Direction.DOWN?1-z:1-y;
            out.addVertex(pose.pose(),x,y,z).setColor((rgb>>16)&255,(rgb>>8)&255,rgb&255,255)
                    .setUv(u0+u*(u1-u0),v0+v*(v1-v0)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose,side.getStepX(),side.getStepY(),side.getStepZ());
        }
    }
}
