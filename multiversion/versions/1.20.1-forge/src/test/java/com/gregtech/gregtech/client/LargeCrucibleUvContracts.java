package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.Direction;
import java.util.*;

/** Capture actual hull vertices; test texture axes, not a duplicate vertex-index lookup table. */
public final class LargeCrucibleUvContracts {
    public static void main(String[] args) {
        List<String> errors=new ArrayList<>();
        for(Direction face:Direction.values()) {
            Capture sink=new Capture();
            LargeCrucibleHullFace.draw(new PoseStack().last(),sink,face,-.999f,0,-.999f,1.999f,3,1.999f,
                    .2f,.4f,.6f,.8f,0xFFFFFF,1,0xF000F0);
            if(sink.vertices.size()!=4)throw new AssertionError("quad required");
            for(float[] a:sink.vertices) {
                float x=(a[0]+.999f)/2.998f,y=a[1]/3,z=(a[2]+.999f)/2.998f;
                float expectedU=switch(face){case NORTH->1-x;case SOUTH->x;case WEST->z;case EAST->1-z;default->x;};
                float expectedV=switch(face){case UP->z;case DOWN->1-z;default->1-y;};
                if(Math.abs(a[3]-(.2f+.2f*expectedU))>1e-5||Math.abs(a[4]-(.6f+.2f*expectedV))>1e-5)
                    errors.add(face+" vertex "+Arrays.toString(a)+" expected normalized UV "+expectedU+","+expectedV);
            }
        }
        if(!errors.isEmpty())throw new AssertionError(String.join("\n",errors));
        System.out.println("PASS: all six emitted hull faces have upright, outward-facing UV axes");
    }
    private static final class Capture implements VertexConsumer {
        final List<float[]> vertices=new ArrayList<>();float x,y,z,u,v;
        public VertexConsumer vertex(double x,double y,double z){this.x=(float)x;this.y=(float)y;this.z=(float)z;return this;}
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        public VertexConsumer overlayCoords(int u,int v){return this;}
        public VertexConsumer uv2(int u,int v){return this;}
        public VertexConsumer normal(float x,float y,float z){return this;}
        public void endVertex(){vertices.add(new float[]{x,y,z,u,v});}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    }
}
