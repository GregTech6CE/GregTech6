package com.gregtech.gregtech.content.energy;
/** Original inventory battery packet caps; final partial injection remains accepted and clamped. */
public final class ItemBatteryRules {
 private ItemBatteryRules(){}
 public static long injectionPackets(long capacity,long stored,long voltage,long packet,long requested){
  if(packet<=0||requested<=0||voltage<=0||stored>=capacity)return 0;
  return Math.min(Math.min(voltage,requested),Math.max(1,(capacity-stored)/packet));
 }
 public static long extractionPackets(long stored,long voltage,long packet,long requested){
  if(packet<=0||requested<=0||voltage<=0||stored<=0)return 0;
  return Math.min(Math.min(voltage,requested),stored/packet);
 }
}
