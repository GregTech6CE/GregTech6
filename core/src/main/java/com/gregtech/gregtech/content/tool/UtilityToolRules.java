package com.gregtech.gregtech.content.tool;
import java.util.List;
public final class UtilityToolRules {private UtilityToolRules(){}public static final int CRANK_TICKS=10,CRANK_SIGNAL=15,SCAFFOLD_SEARCH=256;public static final long SAP_CAPACITY=8000;
 public record Rope(String id,String material){}public static final List<Rope> ROPES=List.of(new Rope("rope","Brown"),new Rope("rope_silk","White"),new Rope("rope_grass","Yellow"),new Rope("rope_vine","Green"),new Rope("rope_plastic","Plastic"),new Rope("rope_steel","Steel"));
 public static long crankSize(int strength,int weakness){long s=2L+strength,w=1L+weakness;return -(8*s+w-1)/w;}
 public static int scaffoldDesign(boolean vertical,boolean above,boolean below){return vertical?above?2:below?1:3:0;}
 public static boolean dustDue(long time){return time%5==0;}public static boolean dust(String prefix){return prefix.equals("dust")||prefix.equals("dustSmall")||prefix.equals("dustTiny")||prefix.equals("dustDiv72");}
 public static int buttonStrength(int strength,int increment){return Math.floorMod(strength-1+increment,15)+1;}public static long buttonLength(long length,int increment){return length>Long.MAX_VALUE-increment?20:length+increment;}
}
