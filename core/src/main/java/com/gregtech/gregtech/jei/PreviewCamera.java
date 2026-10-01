package com.gregtech.gregtech.jei;

/** Bounded camera state, independent of the client renderer and world. */
public final class PreviewCamera {
    public static final double DEFAULT_YAW = 225;
    private double yaw=DEFAULT_YAW, pitch=30, scale=1, x, y;
    public double yaw(){return yaw;}
    public double pitch(){return pitch;}
    public double scale(){return scale;}
    public double x(){return x;}
    public double y(){return y;}
    public void rotate(double dx,double dy) {
        if(!Double.isFinite(dx)||!Double.isFinite(dy))return;
        yaw=((yaw+dx)%360+360)%360;
        pitch=Math.max(-89,Math.min(89,pitch+dy));
    }
    public void pan(double dx,double dy) {
        if(!Double.isFinite(dx)||!Double.isFinite(dy))return;
        x=Math.max(-160,Math.min(160,x+dx));
        y=Math.max(-100,Math.min(100,y+dy));
    }
    public void zoom(double scroll) {
        if(Double.isFinite(scroll))scale=Math.max(0.25,Math.min(4,scale*Math.pow(1.15,scroll)));
    }
    public void reset(){yaw=DEFAULT_YAW;pitch=30;scale=1;x=0;y=0;}
}
