package com.gregtech.gregtech.content.logistics;
/** Original 17997 packet semantics: simulation accepts the full count, execution may overfill. */
public final class LogisticsCorePowerRules{private LogisticsCorePowerRules(){}public static final long MIN=256,RECOMMENDED=512,MAX=1024;
 public record Plan(long accepted,long energy,boolean explode){}
 public static Plan inject(long energy,long capacity,long size,long amount,boolean execute){var plan=com.gregtech.gregtech.content.energy.DirectMachinePowerRules.inject(energy,capacity,MAX,size,amount,execute);return new Plan(plan.accepted(),plan.energy(),plan.explode());}}
