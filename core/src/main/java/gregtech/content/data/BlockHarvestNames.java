package gregtech.content.data;
/** Original harvest tag names and wooden-hull ID precedence, shared across platform block policies. */
public final class BlockHarvestNames {
 private BlockHarvestNames(){}
 public static boolean woodenHull(String path){return path.startsWith("wood_barrel")||path.startsWith("axle_wood_")||path.equals("gearbox_wood")||path.endsWith("_treated_wood")||path.endsWith("_wood")||path.startsWith("tank_wood")||path.startsWith("wood_tank")||path.contains("wooden")||path.startsWith("chest_wood");}
 public static String tag(String tool){return switch(tool){case "PICKAXE","AXE","SHOVEL"->"minecraft:mineable/"+tool.toLowerCase(java.util.Locale.ROOT);case "SWORD"->"gt6ce:mineable/sword";case "WRENCH"->"gt6ce:mineable/wrench";case "CROWBAR"->"gt6ce:mineable/crowbar";case "CUTTER"->"gt6ce:mineable/wire_cutter";case "SHEARS"->"gt6ce:mineable/shears";case "HAND"->"gt6ce:mineable/hand";default->throw new IllegalArgumentException("Unknown harvest tool "+tool);};}
}
