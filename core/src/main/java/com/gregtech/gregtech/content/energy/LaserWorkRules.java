package com.gregtech.gregtech.content.energy;
/** Original waste-per-tick converters; capacities are bounded by the five-tier LaserSpec catalog. */
public final class LaserWorkRules {private LaserWorkRules(){}
 public static long capacity(LaserSpec spec){return spec.input()*2;}
 public static long stored(LaserSpec spec,long value){return Math.max(0,Math.min(capacity(spec),value));}
 public static long converted(LaserSpec spec,long value){return value*spec.output()/spec.input();}
 public static boolean active(LaserSpec spec,long output,boolean stopped){return !stopped&&output>=Math.max(1,spec.output()/2);}
 public static long packets(LaserSpec spec,long stored,long size,long amount){return size<=0||size>capacity(spec)||amount<=0?0:Math.max(0,Math.min(amount,(capacity(spec)-stored)/size));}
 public static boolean acceptsFace(boolean backOnly,int front,int side){return side<0||(backOnly?side==(front^1):side!=front);}
}
