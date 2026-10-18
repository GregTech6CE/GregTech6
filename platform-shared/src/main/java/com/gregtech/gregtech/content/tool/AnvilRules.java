package com.gregtech.gregtech.content.tool;
import net.minecraft.core.Direction;
public final class AnvilRules {public enum Surface{TOP,SMALL,BIG}private AnvilRules(){}
 public static Surface surface(Direction facing,Direction side,double half){return surface(facing,side,half,false);}
 public static Surface surface(Direction facing,Direction side,double half,boolean powered){return Surface.values()[AnvilDomainRules.surface(facing.ordinal(),side.ordinal(),half,powered)];}
 public static long wear(long energy,long duration,int fatigue,int haste){return AnvilDomainRules.wear(energy,duration,fatigue,haste);}
}
