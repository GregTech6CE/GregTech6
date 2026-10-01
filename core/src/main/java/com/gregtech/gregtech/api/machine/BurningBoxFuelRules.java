package com.gregtech.gregtech.api.machine;
/** Original fuel-recipe duration/rate/efficiency conversion, used by both platforms. */
public final class BurningBoxFuelRules {
 private BurningBoxFuelRules(){}
 public static long recipeHeat(long euPerTick,long duration,int efficiency){return Math.abs(euPerTick)*duration*efficiency/10000;}
}
