package gregtech.content.data;

import gregtech.content.recipe.ClayMoldRecipeRules;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/** Emits the clay mold forming, imprint and recovery recipes from {@link ClayMoldRecipeRules}. */
public final class ClayMoldRecipePack {
    private ClayMoldRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (var recipe : ClayMoldRecipeRules.recipes()) {
            GeneratedRecipeJson.put(generated, recipe.path(), recipe.shaped() ? shaped(recipe) : shapeless(recipe));
        }
    }

    private static Map<String, Object> shaped(ClayMoldRecipeRules.Recipe recipe) {
        var key = new TreeMap<String, Object>();
        for (var entry : recipe.key()) {
            key.put(String.valueOf(entry.symbol()), GeneratedRecipeJson.spec(entry.spec()));
        }
        int count = recipe.count() == null ? 1 : recipe.count();
        return GeneratedRecipeJson.shaped(
                recipe.type(), recipe.pattern(), key, recipe.result(), count, recipe.allowMirror());
    }

    private static Map<String, Object> shapeless(ClayMoldRecipeRules.Recipe recipe) {
        var ingredients = new ArrayList<Object>(recipe.ingredients().size());
        for (var spec : recipe.ingredients()) ingredients.add(GeneratedRecipeJson.spec(spec));
        return GeneratedRecipeJson.shapeless(recipe.type(), ingredients, recipe.result(), recipe.count());
    }
}
