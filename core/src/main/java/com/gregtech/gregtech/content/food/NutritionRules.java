package com.gregtech.gregtech.content.food;
public final class NutritionRules {
 private NutritionRules(){} public static int clamp(long value){return (int)Math.max(0,Math.min(127,value));}public static int change(int value,long delta){return clamp(value+Math.max(-127,Math.min(127,delta)));}
 public record Effect(String id,int duration,int amplifier){}
 public static java.util.List<Effect> effects(int stat,int value){int stage=value>=100?3:value>=75?2:value>=50?1:value>=25?0:-1;if(stage<0)return java.util.List.of();var r=new java.util.ArrayList<Effect>();
 switch(stat){case 0->{if(stage>0)r.add(new Effect("nausea",1200,stage-1));r.add(new Effect("strength",300,stage));}case 1->{if(stage>0)r.add(new Effect("weakness",1200,stage-1));r.add(new Effect("haste",300,stage));}case 2->r.add(new Effect("hunger",1200,stage));case 3->{if(stage>0)r.add(new Effect("mining_fatigue",1200,stage-1));r.add(new Effect("speed",300,stage));r.add(new Effect("jump_boost",300,stage));}case 4->{if(stage>0)r.add(new Effect("slowness",1200,stage-1));r.add(new Effect("resistance",300,stage));}default->{}}
 return java.util.List.copyOf(r);}
}
