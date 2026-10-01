package com.gregtech.gregtech.worldgen;
import java.util.List;
/** Exact source pit/peat/seam and litter data; world mutation remains at platform boundary. */
public final class TerrainWorldgenRules {
 private TerrainWorldgenRules(){}
 public static final int PIT_CHANCE=1,PIT_DIVIDER=320,PIT_ABOVE_SEA=16,PIT_BELOW_SEA=8,PIT_MAX_DEPTH=7;
 public static final int TURF_DIVIDER=32,TURF_ABOVE_SEA=1,TURF_BELOW_SEA=12,TURF_MAX_DEPTH=2;
 public record Pit(String name,String blockId){}
 public static final List<Pit> PITS=List.of(new Pit("pit.clay.vanilla","minecraft:clay"),new Pit("pit.clay.brown","gregtech:clay_brown"),new Pit("pit.clay.yellow","gregtech:clay_yellow"),new Pit("pit.clay.blue","gregtech:clay_blue"),new Pit("pit.clay.white","gregtech:clay_white"));
 public static final List<String> SEAM_CLAYS=List.of("clay_blue","clay_brown","clay_white","clay_yellow");
 public static final int SEAM_MIN_RADIUS=4,SEAM_MAX_RADIUS=7,SEAM_MIN_DEPTH=2,SEAM_MAX_DEPTH=4;
 public record Litter(String itemId,String material,boolean rawOre){}
 public static Litter litter(java.util.function.IntUnaryOperator random){if(random.applyAsInt(2)!=0)return new Litter(null,null,false);if(random.applyAsInt(12)==0)return new Litter(null,"MeteoricIron",random.applyAsInt(4)==0);return new Litter("minecraft:flint",null,false);}
}
