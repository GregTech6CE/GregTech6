package com.gregtech.gregtech.content.cover;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** GT6 facing coordinates. Rendering uses the inverse, so text and hit boxes cannot mirror apart. */
public final class CoverFaceCoordinates {
    private CoverFaceCoordinates(){}
    public record UV(double u,double v){}
    public static UV from(Direction side,double x,double y,double z){
        var uv=switch(side){
            case DOWN->new UV(x,1-z);case UP->new UV(x,z);
            case NORTH->new UV(1-x,1-y);case SOUTH->new UV(x,1-y);
            case WEST->new UV(z,1-y);case EAST->new UV(1-z,1-y);
        };
        return new UV(Math.max(0,Math.min(.999999,uv.u)),Math.max(0,Math.min(.999999,uv.v)));
    }
    public static Vec3 to(Direction side,double u,double v,double distance){
        return switch(side){
            case DOWN->new Vec3(u,-distance,1-v);case UP->new Vec3(u,1+distance,v);
            case NORTH->new Vec3(1-u,1-v,-distance);case SOUTH->new Vec3(u,1-v,1+distance);
            case WEST->new Vec3(-distance,1-v,u);case EAST->new Vec3(1+distance,1-v,1-u);
        };
    }
    /** Source manual selector/emitter geometry; -1 means outside a button. */
    public static int select(int mode,double u,double v){
        if(v>=1/16.0&&v<=4/16.0){
            if(u>=1/16.0&&u<=4/16.0)return (mode+15)&15;
            if(u>=12/16.0&&u<=15/16.0)return (mode+1)&15;
        }else if(v>=9/16.0&&v<=12/16.0&&u>=2/16.0&&u<=14/16.0){
            return mode^(u<=5/16.0?8:u<=8/16.0?4:u<=11/16.0?2:1);
        }
        return -1;
    }
}
