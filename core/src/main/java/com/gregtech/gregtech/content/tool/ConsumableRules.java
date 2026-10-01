package com.gregtech.gregtech.content.tool;
/** Counter, ignition-unit cost and original scanner text independent of platform item storage. */
public final class ConsumableRules {private ConsumableRules(){}
 public static long remainingAfter(long remaining,long amount){return Math.max(0L,remaining-amount);}
 public static long ignitionUnits(long cost){return (cost+9999)/10000;}
 public static String blastResistance(double resistance){String value=(int)resistance+"."+(((int)(resistance*10))%10);String word=resistance<4?"(Terrible)":resistance<12?"(Ghast Proof)":resistance<16?"(Creeper Proof)":resistance<=40?"(TNT Proof)":"(Strong Dynamite Proof)";return "Blast Resistance: "+value+" "+word;}
}
