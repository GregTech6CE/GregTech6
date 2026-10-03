package com.gregtech.gregtech.content.plant;
public final class BushGrowthRules {
 private BushGrowthRules(){} public static final int CYCLE_TICKS=128,GROWTH_PER_STAGE=256,LIGHT_GATE=9;
 public record Growth(int counter,int stage){}
 public static Growth advance(int counter,int stage,int increments){if(stage>=3)return new Growth(counter,stage);for(int i=0;i<increments;i++){counter=(counter+1)&255;if(counter==0&&stage<3)stage++;}return new Growth(counter,stage);}
}
