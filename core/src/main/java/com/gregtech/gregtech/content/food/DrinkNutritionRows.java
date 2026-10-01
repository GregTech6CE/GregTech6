package com.gregtech.gregtech.content.food;
/** Shared original GT6 nutrition rows; Minecraft bindings stay in platform code. */
public final class DrinkNutritionRows {
 private DrinkNutritionRows() {}
 public record Row(String field,int foodLevel,float saturation,int alcohol,int caffeine,int dehydration,int sugar,int fat,int radiation) {}
}
