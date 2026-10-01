package com.gregtech.gregtech.content.energy;
public final class PumpWorkRules {private PumpWorkRules(){}public static final int SCAN_AREA=128,START_ENERGY=8192,DRAIN_ENERGY=2048,SCAN_DEPTH=64,TANK_CAPACITY=16000;
 public static long accepted(long stored,long size,long offered,long maximum){if(size==Long.MIN_VALUE||offered<=0)return 0;long packet=Math.abs(size);if(packet<8||packet>maximum||stored>=START_ENERGY)return 0;return Math.min(offered,(START_ENERGY-stored)/packet);}
}
