package com.gregtech.gregtech.content.tool;
/** One shared paint flag/color state. Original gt.color and gt.painted remain platform NBT boundaries. */
public final class PaintState {private boolean painted;private int rgb=0xFFFFFF;
public boolean painted(){return painted;}public int color(){return rgb;}
public boolean paint(int color){color&=0xFFFFFF;if(color==rgb)return false;rgb=color;painted=true;return true;}
public boolean mix(int color){return paint(painted?PaintingRules.mix(color,rgb):color);}
public boolean clear(){if(!painted)return false;painted=false;rgb=0xFFFFFF;return true;}
public boolean restore(boolean flag,int color){color&=0xFFFFFF;boolean changed=painted!=flag||rgb!=color;painted=flag;rgb=color;return changed;}
}
