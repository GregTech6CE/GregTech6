package com.gregtech.gregtech.content.tool;
/** Original GT6 electric material wear, shared independently of EU storage and platform NBT. */
public final class ElectricToolWearRules {
 private ElectricToolWearRules(){}
 public static long maximum(long durability){long base=Math.max(0,durability);return base>Long.MAX_VALUE/100?Long.MAX_VALUE:Math.max(1,base*100);}
 public static int chanceBound(int quality){return (int)Math.min(Integer.MAX_VALUE,Math.max(10L,quality*20L));}
 public static long addDamage(long old,long amount){return old>Long.MAX_VALUE-amount?Long.MAX_VALUE:old+amount;}
 public static int scrapRandomBound(String tool){return switch(tool){case "Drill","Mining Drill"->3;case "Mixer"->7;case "Chainsaw"->9;case "Wrench"->17;case "Screwdriver"->5;case "BuzzSaw"->17;case "Trimmer"->16;default->throw new IllegalArgumentException("Unknown electric tool "+tool);};}
}
