package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
/** Original RecipeMapHandlerPrefixForging / material weight calculation at 96 GU/t. */
public final class ExtrusionWorkRules {
 private ExtrusionWorkRules(){}
 public static long hotExtrusionTicks(GTMaterial material,long units){
  double kilograms=material.getDensity()*111.111111*units/GTValues.U;
  return Math.max(16,1+(long)Math.abs((material.getMeltingPoint()-293d)*kilograms/(75*96)));
 }
}
