package com.gregtech.gregtech.content.bumble;
/** Exact original genome generation/inheritance; NBT and item components stay at platform boundaries. */
public final class BumbleGenomeRules {
 private BumbleGenomeRules(){}
 public interface RandomBits {int nextInt(int bound);boolean nextBoolean();}
 public record Genome(float minhum,float maxhum,long offspring,long work,long aggro,long life,long mintemp,long maxtemp,boolean rain,boolean storm,boolean night,boolean day,boolean inside,boolean outside){}
 public static long clamp(long value,long min,long max){return Math.max(min,Math.min(max,value));}
 public static Genome fromEnvironment(long temperature,float rainfall,boolean hasSky,boolean day,boolean night,RandomBits random){
  float low=rainfall-.10f-random.nextInt(41)/100f,high=rainfall+.10f+random.nextInt(41)/100f;
  long tLow=temperature-15-random.nextInt(31),tHigh=temperature+15+random.nextInt(31);
  long children=1+random.nextInt(4),work=1+random.nextInt(10000),aggro=100+random.nextInt(9901),life=1200+random.nextInt(142801);
  boolean rain=false,storm=false;if(hasSky){rain=random.nextInt(10000)<(int)(rainfall*10000);storm=random.nextInt(20000)<(int)(rainfall*10000);}
  return new Genome(low<.01f?0:low,high<.01f?.01f:high,children,work,aggro,life,tLow,tHigh,rain,storm,night||!day,day||!night,!hasSky,hasSky);
 }
 public static Genome inherit(Genome a,Genome b,RandomBits random){
  float low=(random.nextBoolean()?a:b).minhum(),high=(random.nextBoolean()?a:b).maxhum();
  long children=(random.nextBoolean()?a:b).offspring(),work=(random.nextBoolean()?a:b).work(),aggro=(random.nextBoolean()?a:b).aggro(),life=(random.nextBoolean()?a:b).life(),tLow=(random.nextBoolean()?a:b).mintemp(),tHigh=(random.nextBoolean()?a:b).maxtemp();
  boolean rain=(random.nextBoolean()?a:b).rain(),storm=(random.nextBoolean()?a:b).storm(),night=(random.nextBoolean()?a:b).night(),day=(random.nextBoolean()?a:b).day()||!night,inside=(random.nextBoolean()?a:b).inside(),outside=(random.nextBoolean()?a:b).outside()||!inside;
  return new Genome(low,high,children,work,aggro,life,tLow,tHigh,rain,storm,night,day,inside,outside);
 }
}
