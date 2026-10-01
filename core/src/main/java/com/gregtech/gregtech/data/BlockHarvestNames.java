package com.gregtech.gregtech.data;
/** Original harvest tag names and wooden-hull ID precedence, shared across platform block policies. */
public final class BlockHarvestNames {
 private BlockHarvestNames(){}
 public static boolean woodenHull(String path){return path.startsWith("wood_barrel")||path.startsWith("axle_wood_")||path.equals("gearbox_wood")||path.endsWith("_treated_wood")||path.endsWith("_wood")||path.startsWith("tank_wood")||path.startsWith("wood_tank")||path.contains("wooden")||path.startsWith("chest_wood");}
 public static String tag(String tool){return switch(tool){case "PICKAXE","AXE","SHOVEL"->"minecraft:mineable/"+tool.toLowerCase(java.util.Locale.ROOT);case "SWORD"->"gregtech:mineable/sword";case "WRENCH"->"gregtech:mineable/wrench";case "CROWBAR"->"gregtech:mineable/crowbar";case "CUTTER"->"gregtech:mineable/wire_cutter";case "SHEARS"->"gregtech:mineable/shears";case "HAND"->"gregtech:mineable/hand";default->throw new IllegalArgumentException("Unknown harvest tool "+tool);};}
}
