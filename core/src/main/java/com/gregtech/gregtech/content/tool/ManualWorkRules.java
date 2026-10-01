package com.gregtech.gregtech.content.tool;
public final class ManualWorkRules {private ManualWorkRules(){}public static final int OUTPUTS=12,GRIND_CLICKS=10,MAX_GU=32;
 public static boolean siftDue(long time){return time%5==0;}public static int siftIncrement(int hasteAmplifier){return hasteAmplifier<0?1:hasteAmplifier+2;}public static int siftThreshold(int fatigueAmplifier){return 4*(fatigueAmplifier<0?2:fatigueAmplifier+3);}
 public static int abrasiveUses(String material){return switch(material){case "SoulSand","EndSandWhite","EndSandBlack"->16;case "RedSand"->8;case "Sand"->4;default->0;};}
 public static double exhaustionDivisor(String kind){return switch(kind){case "MORTAR"->250;case "GRINDSTONE"->10000;default->1000;};}
}
