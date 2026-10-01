package com.gregtech.gregtech.content.energy;
/** Original signed packet loss and saturating counters; graph traversal stays at the world boundary. */
public final class WirePacketRules {
 private WirePacketRules(){}
 public static boolean canTransfer(long voltage,long amperage,long loss){return voltage!=Long.MIN_VALUE&&amperage>0&&Math.abs(voltage)>loss;}
 public static long afterLoss(long voltage,long loss){return voltage>0?voltage-loss:voltage+loss;}
 public static long saturatedAdd(long left,long right){return left>Long.MAX_VALUE-right?Long.MAX_VALUE:left+right;}
 public static long wattage(long magnitude,long amperage){return magnitude>Long.MAX_VALUE/amperage?Long.MAX_VALUE:magnitude*amperage;}
}
