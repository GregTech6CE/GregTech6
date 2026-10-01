package com.gregtech.gregtech.content.tool;
/** Pure GT6 top/side routing and overflow-safe wear. Direction ordinals are DOWN,UP,NORTH,SOUTH,WEST,EAST. */
public final class AnvilDomainRules {private AnvilDomainRules(){}
 public static int surface(int facing,int side,double half,boolean powered){
  if(side==1 || powered && side==0)return 0;
  if(facing<2||facing>5||side<2||side>5)throw new IllegalArgumentException("Anvil face");
  boolean small=(side/2==facing/2)?((facing==2||facing==4)?half<.5:half>.5):side==switch(facing){case 4->2;case 5->3;case 2->4;default->5;};return small?1:2;
 }
    public static long wear(long energyPerTick, long duration, int fatigueLevel, int hasteLevel) {
        // BigInteger keeps high-tier recipe energy and potion multipliers from overflowing.
        var power = java.math.BigInteger.valueOf(energyPerTick).abs().multiply(java.math.BigInteger.valueOf(Math.max(1, duration)));
        var base = power.add(java.math.BigInteger.valueOf(3)).divide(java.math.BigInteger.valueOf(4)).max(java.math.BigInteger.valueOf(10000));
        var divisor = java.math.BigInteger.valueOf(1L + Math.max(0, hasteLevel));
        return base.multiply(java.math.BigInteger.valueOf(1L + Math.max(0, fatigueLevel)))
                .add(divisor.subtract(java.math.BigInteger.ONE)).divide(divisor).min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
