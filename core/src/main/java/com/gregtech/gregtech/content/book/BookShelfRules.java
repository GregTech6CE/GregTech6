package com.gregtech.gregtech.content.book;
/** Shared original shelf metadata/28-slot picker/book classification and cuboid layout. */
public final class BookShelfRules {private BookShelfRules(){}public static final int SLOTS=28,COLUMNS=7;
 public record VariantSpec(String path,int originalId,String kind,String materialName,String displayMaterialName,String texture,float hardness,float resistance,int flammability){}
 private static final java.util.Set<String> NORMAL=java.util.Set.of("minecraft:book","minecraft:writable_book","minecraft:written_book","gregtech:dusty_guide_book","gregtech:dusty_material_dictionary");
 private static final java.util.Set<String> DISPLAY=java.util.Set.of("minecraft:paper","minecraft:map","minecraft:filled_map","minecraft:name_tag","minecraft:item_frame","minecraft:painting","minecraft:oak_button","minecraft:stone_button","minecraft:lever","minecraft:redstone_torch","minecraft:cobblestone");
 private static final java.util.Set<String> CONTROLS=java.util.Set.of("minecraft:oak_button","minecraft:stone_button","minecraft:lever","minecraft:redstone_torch","minecraft:cobblestone");
 public static int enchantPower(String id){return id.equals("minecraft:enchanted_book")?2:NORMAL.contains(id)?1:0;}
 public static boolean canPlace(String id){return enchantPower(id)>0||DISPLAY.contains(id);}
 public static boolean canAutoExtract(String id){return !CONTROLS.contains(id);}
 public static int slotFor(boolean front,double x,double y){int base=y<.5?(front?6:20):(front?13:27);return base-Math.max(0,Math.min(6,(int)Math.floor(8*(x-1./16))));}
 public record Box(double x0,double y0,double z0,double x1,double y1,double z1){}
 public static Box bookBounds(int slot){if(slot<0||slot>=SLOTS)throw new IllegalArgumentException("book slot "+slot);int col=slot%7;boolean front=slot<14;int x=front?1+col*2:13-col*2,y=slot%14<7?9:1,z=front?2:9;return new Box(x/16.,y/16.,z/16.,(x+2)/16.,(y+6)/16.,(z+5)/16.);}
}
