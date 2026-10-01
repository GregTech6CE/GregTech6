package com.gregtech.gregtech.content.tool;
/** Brokestar GT6 dye order, paint mixing and entity/grass policy over existing identities. */
public final class PaintingRules {private PaintingRules(){}public static final long CAPACITY=5120,BLOCK_COST=10,ENTITY_COST=50;public record Dye(int index,String id,int rgb){public String fullId(){return "spray_paint_"+id;}public String usedId(){return fullId()+"_2";}}
public static final java.util.List<Dye> DYES=java.util.List.of(new Dye(0,"black",0x202020),new Dye(1,"red",0xFF0000),new Dye(2,"green",0x00FF00),new Dye(3,"brown",0x604000),new Dye(4,"blue",0x0000FF),new Dye(5,"purple",0x800080),new Dye(6,"cyan",0x00FFFF),new Dye(7,"light_gray",0xC0C0C0),new Dye(8,"gray",0x808080),new Dye(9,"pink",0xFFC0C0),new Dye(10,"lime",0x80FF80),new Dye(11,"yellow",0xFFFF00),new Dye(12,"light_blue",0x8080FF),new Dye(13,"magenta",0xFF00FF),new Dye(14,"orange",0xFF8000),new Dye(15,"white",0xFFFFFF));
public static int vanillaDyeId(int index){return ~index&15;}
public static int mix(int a,int b){return ((((a>>16&255)+(b>>16&255))>>1)<<16)|((((a>>8&255)+(b>>8&255))>>1)<<8)|(((a&255)+(b&255))>>1);}
public record EntityFacts(boolean alive,boolean sheep,boolean sheared,boolean wolf,boolean tamed,int dyeId){}
public static int entityDyeId(EntityFacts facts,int index){int dye=vanillaDyeId(index);return facts!=null&&facts.alive()&&facts.dyeId()!=dye&&(facts.sheep()&&!facts.sheared()||facts.wolf()&&facts.tamed())?dye:-1;}
public static String grassId(int index){return switch(index){case 2->"grassblock_medium";case 10->"grassblock_light";case 0->"grassblock_dark";case 7->"grassblock_normal";case 11->"grassblock_yellow";case 3->"grassblock_brown";default->null;};}
public static final java.util.Set<String> GRASS_IDS=java.util.Set.of("grassblock_medium","grassblock_light","grassblock_dark","grassblock_normal","grassblock_yellow","grassblock_brown");
}
