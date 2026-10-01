package com.gregtech.gregtech.content.logistics;
/** Original 17997 packet semantics: simulation accepts the full count, execution may overfill. */
public final class LogisticsCorePowerRules{private LogisticsCorePowerRules(){}public static final long MIN=256,RECOMMENDED=512,MAX=1024;
 public record Plan(long accepted,long energy,boolean explode){}
 public static Plan inject(long energy,long capacity,long size,long amount,boolean execute){if(size==Long.MIN_VALUE||size==0||amount<=0||energy>capacity)return new Plan(0,energy,false);long packet=Math.abs(size);if(!execute)return new Plan(amount,energy,false);if(packet>MAX)return new Plan(amount,energy,true);long accepted=Math.min(amount,(Long.MAX_VALUE-energy)/packet);return new Plan(accepted,energy+accepted*packet,false);}}
