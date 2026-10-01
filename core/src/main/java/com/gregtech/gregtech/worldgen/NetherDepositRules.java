package com.gregtech.gregtech.worldgen;
/** Original Nether regional identities and crystal random-walk executor shared by both loaders. */
public final class NetherDepositRules {
 private NetherDepositRules(){}
 public static final java.util.List<String> CRYSTALS=java.util.List.of("crystal_ore_arsenopyrite","crystal_ore_chalcopyrite","crystal_ore_cinnabar","crystal_ore_cobaltite","crystal_ore_galena","crystal_ore_kesterite","crystal_ore_molybdenite","crystal_ore_pyrite","crystal_ore_sphalerite","crystal_ore_stannite","crystal_ore_stibnite","crystal_ore_tetrahedrite");
 public static final int WATER_LEVEL=31,QUARTZ_BASE_Y=40,QUARTZ_OPTIONS=200;public static final float[] QUARTZ_NOISE_Y={0,64};
 public interface CrystalSink {boolean air(int x,int y,int z);int crystalNeighbors(int x,int y,int z);void place(int x,int y,int z);}
 public static void growCrystal(int x,int seedY,int z,java.util.function.IntUnaryOperator random,CrystalSink sink){for(int i=0;i<1500;i++){int px=x+random.applyAsInt(8)-random.applyAsInt(8),py=seedY-random.applyAsInt(12),pz=z+random.applyAsInt(8)-random.applyAsInt(8);if(sink.air(px,py,pz)&&sink.crystalNeighbors(px,py,pz)==1)sink.place(px,py,pz);}}
}
