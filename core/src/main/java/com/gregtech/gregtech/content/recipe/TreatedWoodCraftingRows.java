package com.gregtech.gregtech.content.recipe;
/** Original treated-wood hand patterns from Loader_OreProcessing:194 / Loader_Recipes_Woods:217-218.
 * Same encoded contract as GTWoodRecipes.CRAFT_ROWS; no independent material namespace. */
public final class TreatedWoodCraftingRows {
 private TreatedWoodCraftingRows(){}
 public static final String[] BOLT_PATTERN={"s "," S"};
 public static final String[] SMALL_GEAR_PATTERN={"P "," s"};
 public static final String[] GEAR_PATTERN={"BPB","PsP","BPB"};
 public static final String[] ROWS={
  "treated/wood_bolt|shaped|nomirror|s / S|s=tool:saw;S=item:minecraft:stick|material:bolt:Wood*2|Loader_OreProcessing.java:194",
  "treated/small_gear|shaped|nomirror|P / s|P=material:plate:WoodTreated;s=tool:saw|material:gearGtSmall:WoodTreated*1|Loader_Recipes_Woods.java:217",
  "treated/gear|shaped|nomirror|BPB/PsP/BPB|B=material:bolt:WoodTreated;P=material:plate:WoodTreated;s=tool:saw|material:gearGt:WoodTreated*1|Loader_Recipes_Woods.java:218"
 };
}
