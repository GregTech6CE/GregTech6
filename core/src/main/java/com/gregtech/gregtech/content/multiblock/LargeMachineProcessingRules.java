package com.gregtech.gregtech.content.multiblock;
/** Source large-machine overclock, energy, batch efficiency and compact matter rules. */
public final class LargeMachineProcessingRules {private LargeMachineProcessingRules(){}public static final long MATTER_INPUT_MAX=2097152;
 public static long inputMinimum(boolean timed,boolean heat){return timed||heat?1:512;}public static long inputMaximum(boolean timed){return timed?16:4096;}public static boolean constantEnergy(boolean timed,String name){return !timed&&!java.util.Set.of("largecrusher","largeshredder","largesqueezer").contains(name);}public static int efficiency(String name){return switch(name){case "largeoven"->2500;case "largecentrifuge","largeelectrolyzer","largesluice","largecrusher","largeshredder","largesqueezer"->5000;default->10000;};}public static int compactMatterEfficiency(int tier){return 5000+1250*(Math.max(1,Math.min(5,tier))-1);}
}
