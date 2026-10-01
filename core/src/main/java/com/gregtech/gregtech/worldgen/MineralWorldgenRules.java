package com.gregtech.gregtech.worldgen;
import java.util.Random;
/** Exact original shared mineral-region, pylon and bedrock shape decisions. */
public final class MineralWorldgenRules {
 private MineralWorldgenRules(){}
 public record Point(int x,int z){}
 public static Point coltanCentre(long seed){Random r=new Random(seed+5);return new Point((int)(r.nextGaussian()*1500),(int)(r.nextGaussian()*1500));}
 public static int coltanCount(int amount,java.util.function.IntUnaryOperator random){return Math.max(1,amount/2+random.applyAsInt(1+amount)/2);}
 public static String coltanMaterial(int roll){return switch(roll){case 0->"Columbite";case 1->"Tantalite";default->"Coltan";};}
 public static int distanceSquared(int x,int z,int minX,int minZ){return (x-minX)*(x-minX)+(z-minZ)*(z-minZ);}
 public static long bedrockWeight(int chance){return Math.max(1,1_000_000L/Math.max(1,chance));}
 public record Tier(int fromL,int toL,int radius){}
 public static final java.util.List<Tier> PYLON_TIERS=java.util.List.of(new Tier(8,10,0),new Tier(5,7,1),new Tier(2,4,2),new Tier(0,1,3));
 public static final int[] MUFFIN_INNER={5,4,2,1,0,2,5},MUFFIN_OUTER={11,12,14,15,16,14,11};
 public interface VeinSink {boolean isBedrockFloor(int x,int y,int z);boolean bedrock(int x,int y,int z,boolean small);boolean ore(int x,int y,int z,boolean small);}
 public static boolean bedrockVein(int minX,int minZ,int floor,int tailLimit,java.util.function.IntUnaryOperator random,VeinSink sink){
  if(!sink.isBedrockFloor(minX+8,floor,minZ+8))return false;boolean placed=false;
  for(int x=5;x<11;x++)for(int z=5;z<11;z++)switch(random.applyAsInt(6)){case 0->placed|=sink.bedrock(minX+x,floor,minZ+z,false);case 1,2->placed|=sink.bedrock(minX+x,floor,minZ+z,true);default->{}}
  sink.bedrock(minX+6+random.applyAsInt(4),floor,minZ+6+random.applyAsInt(4),false);
  for(int y=1;y<MUFFIN_INNER.length;y++)for(int x=MUFFIN_INNER[y];x<MUFFIN_OUTER[y];x++)for(int z=MUFFIN_INNER[y];z<MUFFIN_OUTER[y];z++)switch(random.applyAsInt(6)){case 0->placed|=sink.ore(minX+x,floor+y,minZ+z,false);case 1,2->placed|=sink.ore(minX+x,floor+y,minZ+z,true);default->{}}
  for(int i=5+random.applyAsInt(3);i>0;i--){int x=5+random.applyAsInt(6),z=5+random.applyAsInt(6);for(int y=MUFFIN_INNER.length;y<tailLimit;y++){switch(random.applyAsInt(7)){case 0->x++;case 1->x--;case 2->z++;case 3->z--;default->{}}if(x<=0||x>=15||z<=0||z>=15){placed|=sink.ore(minX+x,floor+y,minZ+z,true);break;}if(random.applyAsInt(3)!=0)placed|=sink.ore(minX+x,floor+y,minZ+z,true);}}
  return placed;
 }
}
