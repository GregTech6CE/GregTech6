package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.*;
import com.gregtech.gregtech.content.tool.AnvilWorkpieceGeometry;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import java.util.*;

/** Captures the real cuboid renderer: nonzero volume, outward winding, pose normals and full entity fields. */
public final class AnvilRenderContracts {
    public static void main(String[] args) {
        for(var facing:Direction.Plane.HORIZONTAL) for(int slot=0;slot<2;slot++) for(int shape:new int[]{8,9}) {
            var pose=new PoseStack();pose.translate(12,5,-7);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(37));
            var sink=new Capture();
            AnvilCuboidRenderer.draw(pose.last(),sink,AnvilWorkpieceGeometry.bounds(shape,slot,facing),.2f,.4f,.6f,.8f,0x917341,0x00F000A0);
            check(sink.vertices.size()==24,"six solid quad faces required");
            for(int i=0;i<24;i+=4) {
                var a=sink.vertices.get(i);var b=sink.vertices.get(i+1);var c=sink.vertices.get(i+2);
                var normal=new Vector3f(b.pos).sub(a.pos).cross(new Vector3f(c.pos).sub(a.pos)).normalize();
                check(normal.dot(a.normal)>.999f,"outward winding must agree with transformed lighting normal");
            }
            float minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
            for(var v:sink.vertices) { minY=Math.min(minY,v.pos.y);maxY=Math.max(maxY,v.pos.y); }
            check(maxY-minY>=.124f,"hammer may never degrade to a flat item quad");
        }
        System.out.println("PASS anvil actual vertex emission: solid head/shaft, every slot/facing, winding, pose normals, UV/light/color/overlay");
    }
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    private record Vertex(Vector3f pos,Vector3f normal) {}
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();Vector3f pos,normal;int fields;
        public VertexConsumer vertex(double x,double y,double z){pos=new Vector3f((float)x,(float)y,(float)z);fields|=1;return this;}
        public VertexConsumer color(int r,int g,int b,int a){check(r==0x91&&g==0x73&&b==0x41&&a==255,"material tint retained");fields|=2;return this;}
        public VertexConsumer uv(float u,float v){check(u>=.2f&&u<=.4f&&v>=.6f&&v<=.8f,"atlas UV bounds");fields|=4;return this;}
        public VertexConsumer overlayCoords(int u,int v){fields|=8;return this;}
        public VertexConsumer uv2(int u,int v){check(u==0xA0&&v==0xF0,"packed world lighting");fields|=16;return this;}
        public VertexConsumer normal(float x,float y,float z){normal=new Vector3f(x,y,z);fields|=32;return this;}
        public void endVertex(){check(fields==63,"complete entity-format vertex");vertices.add(new Vertex(pos,normal));fields=0;}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    }
}
