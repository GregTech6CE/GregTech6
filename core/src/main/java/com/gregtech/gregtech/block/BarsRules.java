package com.gregtech.gregtech.block;
/** Original independent four-segment bar choice/drop executor, independent of Minecraft. */
public final class BarsRules {private BarsRules(){}public record Spec(String id,String material,int tint,int harvest,float resistance){}
 public static final java.util.List<Spec> SPECS=java.util.List.of(new Spec("bars_iron","iron",0xD8D8D8,2,5),new Spec("bars_steel","steel",0x808080,2,8),new Spec("bars_brass","brass",0xD2A85B,1,5),new Spec("bars_bronze","bronze",0xCD7F32,2,5),new Spec("bars_wrought_iron","wrought_iron",0xC0C0C0,2,5),new Spec("bars_stainless","stainless",0xE0E0E0,2,5),new Spec("bars_tungsten_steel","tungsten_steel",0x7070A0,4,16));
 public static Spec spec(String id){return SPECS.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();}
 public static int placement(char axis,double x,double z){if(axis=='X')return z<.5?1:2;if(axis=='Z')return x<.5?4:8;return x<z?(x+z<1?4:2):(x+z<1?1:8);}
 public static int chosen(int mask,char axis,int faceBit,double x,double z){int bit=Integer.bitCount(mask)==3?15^mask:placement('Y',x,z);if((mask&bit)!=0||axis!='Y')bit=placement(axis,x,z);if((mask&bit)!=0&&axis!='Y')bit=faceBit;return bit;}
 public static int dropCount(int mask){return Math.max(1,Integer.bitCount(mask));}
}
