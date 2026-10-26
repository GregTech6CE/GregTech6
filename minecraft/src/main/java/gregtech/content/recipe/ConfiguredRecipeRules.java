package gregtech.content.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import gregtech.GregTechConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.Map;

/** Apply recipe switches before parsing, without modifying datapack files. */
public final class ConfiguredRecipeRules {
    private ConfiguredRecipeRules() {}

    public static void apply(Map<ResourceLocation, JsonElement> recipes, ResourceManager resources) {
        recipes.replaceAll((id, json) -> restore(id, json, resources));
        recipes.entrySet().removeIf(entry -> !GregTechConfig.recipeEnabled(entry.getKey().toString())
                || disabled(entry.getValue()));
    }

    private static JsonElement restore(ResourceLocation id, JsonElement current, ResourceManager resources) {
        boolean vanilla = id.getNamespace().equals("minecraft");
        if (id.getNamespace().equals("gt6ce") || (vanilla ? GregTechConfig.Rule.REPLACE_VANILLA_RECIPES
                : GregTechConfig.Rule.REPLACE_MOD_RECIPES).enabled()) return current;
//? if >=1.21.1 {
        /*String directory = "recipe/";
*///?} else {
        String directory = "recipes/";
//?}
        var path = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), directory + id.getPath() + ".json");
        var stack = resources.getResourceStack(path);
        if (stack.isEmpty() || !stack.get(stack.size() - 1).sourcePackId().toLowerCase(java.util.Locale.ROOT).contains("gt6ce"))
            return current; // A user datapack remains authoritative.
        for (int i = stack.size() - 2; i >= 0; i--) {
            var resource = stack.get(i);
            if (resource.sourcePackId().toLowerCase(java.util.Locale.ROOT).contains("gt6ce")) continue;
            try (var reader = resource.openAsReader()) {
                return JsonParser.parseReader(reader);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException("Cannot restore recipe " + id, failure);
            }
        }
        return current;
    }

    private static boolean disabled(JsonElement json) {
        if (!json.isJsonObject()) return false;
        var object = json.getAsJsonObject();
        if (!object.has("result")) return false;
        var result = object.get("result");
        String output = "";
        if (result.isJsonPrimitive()) output = result.getAsString();
        else if (result.isJsonObject()) {
            var value = result.getAsJsonObject();
            if (value.has("id")) output = value.get("id").getAsString();
            else if (value.has("item")) output = value.get("item").getAsString();
        }
        if (output.equals("minecraft:enchanting_table")) return GregTechConfig.Rule.DISABLE_ENCHANTMENT_TABLE.enabled();
        if (output.equals("minecraft:ender_chest")) return GregTechConfig.Rule.DISABLE_ENDER_CHEST.enabled();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        if (output.equals("minecraft:charcoal") && type.equals("minecraft:smelting"))
            return GregTechConfig.Rule.DISABLE_CHARCOAL.enabled();
        if (output.equals("minecraft:torch") && GregTechConfig.Rule.DISABLE_COAL_TORCHES.enabled()) {
            String ingredients = object.has("ingredients") ? object.get("ingredients").toString()
                    : object.has("key") ? object.get("key").toString() : "";
            return ingredients.contains("minecraft:coal") || ingredients.contains("minecraft:charcoal")
                    || ingredients.contains("minecraft:coals");
        }
        return false;
    }
}
