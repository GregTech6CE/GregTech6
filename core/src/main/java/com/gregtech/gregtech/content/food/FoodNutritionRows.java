package com.gregtech.gregtech.content.food;
/** Shared original GT6 nutrition rows; Minecraft bindings stay in platform code. */
public final class FoodNutritionRows {
 private FoodNutritionRows() {}
 public record Food(int level,float saturation,int hydration,int temperature,boolean alwaysEdible,boolean rotten) {}
 public record Stats(int alcohol,int caffeine,int dehydration,int sugar,int fat,int radiation) {}
 public record Entry(String id,String display,int meta,Food food,Stats stats,String container,int containerCount,boolean hasFoodStat,int line) {}
}
