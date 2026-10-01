package com.gregtech.gregtech.api.tool;
/** Original GTToolType crafting durability costs. Full tool behavior stays in native adapters. */
public final class ToolCraftingRules {
 private ToolCraftingRules(){}
 public static int damage(String kind){return switch(kind){
  case "sword" -> 100;
  case "pickaxe" -> 100;
  case "shovel" -> 100;
  case "axe" -> 100;
  case "hoe" -> 100;
  case "saw" -> 100;
  case "hammer" -> 400;
  case "soft_hammer" -> 100;
  case "wrench" -> 800;
  case "file" -> 100;
  case "crowbar" -> 100;
  case "screwdriver" -> 100;
  case "club" -> 800;
  case "wire_cutter" -> 400;
  case "scoop" -> 100;
  case "branch_cutter" -> 100;
  case "universal_spade" -> 100;
  case "knife" -> 100;
  case "butchery_knife" -> 100;
  case "sense" -> 100;
  case "plow" -> 100;
  case "plunger" -> 100;
  case "rolling_pin" -> 50;
  case "chisel" -> 400;
  case "flint_and_tinder" -> 100;
  case "monkey_wrench" -> 800;
  case "bending_cylinder" -> 100;
  case "small_bending_cylinder" -> 100;
  case "double_axe" -> 100;
  case "construction_pick" -> 100;
  case "magnifying_glass" -> 400;
  case "scissors" -> 100;
  case "pincers" -> 100;
  case "spade" -> 100;
  case "gem_tipped_pickaxe" -> 100;
  case "hand_drill" -> 100;
  case "builder_wand" -> 100;
  default -> 0;
 };}
}
