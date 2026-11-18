package gregtech.content.data;

import gregtech.content.recipe.ExtruderShapeRecipeRules;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/** Emits the extruder, food and slicer mold tool-shaped recipes from {@link ExtruderShapeRecipeRules}. */
public final class ExtruderShapeRecipePack {
    private ExtruderShapeRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (var recipe : ExtruderShapeRecipeRules.recipes()) {
            var key = new TreeMap<String, Object>();
            for (var entry : recipe.key()) {
                key.put(String.valueOf(entry.symbol()), GeneratedRecipeJson.spec(entry.spec()));
            }
            var json = GeneratedRecipeJson.shaped(
                    ExtruderShapeRecipeRules.TYPE, recipe.pattern(), key, "gt6ce:" + recipe.path(), 1, false);
            GeneratedRecipeJson.put(generated, ExtruderShapeRecipeRules.DIRECTORY + "/" + recipe.path(), json);
        }
    }
}
