package gregtech.content.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Shared JSON serialization helpers for dynamic {@link RecipePublishing} packs across loaders. */
public final class GeneratedRecipeJson {
    private static final Gson GSON = new Gson();

    private GeneratedRecipeJson() {}

    /** Datapack directory holding recipe JSON; the 1.21 recipe format renamed it from {@code recipes}. */
    public static String recipeRoot() {
//? if >=1.21.1 {
        /*return "recipe";
*///?} else {
        return "recipes";
//?}
    }

    /** Key of the item id inside a recipe result object; the 1.21 result format renamed it from {@code item}. */
    public static String resultKey() {
//? if >=1.21.1 {
        /*return "id";
*///?} else {
        return "item";
//?}
    }

    /** Resource path of the generated recipe {@code path}, including the version-specific root. */
    public static String recipePath(String path) {
        return recipeRoot() + "/" + path + ".json";
    }

    /** {@code gt6ce} resource id of the generated recipe {@code path}. */
    public static ResourceLocation recipeId(String path) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", recipePath(path));
    }

    /** Emits a recipe JSON under {@code gt6ce:<recipe root>/<path>.json}. */
    public static void put(Map<ResourceLocation, byte[]> generated, String path, Map<String, Object> recipe) {
        generated.put(recipeId(path), GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, Object> item(String id) {
        return Map.of("item", id);
    }

    public static Map<String, Object> tag(String id) {
        return Map.of("tag", id);
    }

    /** Parses {@code item:<id>} or {@code tag:<id>} into a recipe ingredient map. */
    public static Map<String, Object> spec(String spec) {
        if (spec.startsWith("item:")) return item(spec.substring(5));
        if (spec.startsWith("tag:")) return tag(spec.substring(4));
        throw new IllegalArgumentException("Unsupported ingredient spec: " + spec);
    }

    /** A result object carrying only the item id, for the rows that leave the count at its default. */
    public static Map<String, Object> result(String itemId) {
        return Map.of(resultKey(), itemId);
    }

    public static Map<String, Object> result(String itemId, int count) {
        var out = new LinkedHashMap<String, Object>();
        out.put(resultKey(), itemId);
        out.put("count", count);
        return out;
    }

    /** {@link #result} as the JSON tree that hand-built recipe rows carry in their {@code result} field. */
    public static JsonObject resultJson(String itemId, int count) {
        return GSON.toJsonTree(result(itemId, count)).getAsJsonObject();
    }

    public static Map<String, Object> shaped(
            String type,
            List<String> pattern,
            Map<String, Object> key,
            String resultId,
            int count,
            Boolean allowMirror) {
        return shaped(type, pattern, key, resultId, count, allowMirror, null, null, null, null);
    }

    public static Map<String, Object> shaped(
            String type,
            List<String> pattern,
            Map<String, Object> key,
            String resultId,
            int count,
            Boolean allowMirror,
            String group,
            String category,
            String constructionColor,
            Boolean autocraftable) {
        var json = new LinkedHashMap<String, Object>();
        json.put("type", type);
        if (category != null) json.put("category", category);
        if (group != null) json.put("group", group);
        if (allowMirror != null) json.put("allow_mirror", allowMirror);
        if (constructionColor != null) json.put("construction_color", constructionColor);
        if (autocraftable != null) json.put("gregtech_autocraftable", autocraftable);
        json.put("pattern", pattern);
        json.put("key", key);
        json.put("result", result(resultId, count));
        return json;
    }

    public static Map<String, Object> shapeless(
            String type, List<Object> ingredients, String resultId, int count) {
        return shapeless(type, ingredients, resultId, count, null, null, null);
    }

    public static Map<String, Object> shapeless(
            String type,
            List<Object> ingredients,
            String resultId,
            int count,
            String group,
            String category,
            Boolean autocraftable) {
        var json = new LinkedHashMap<String, Object>();
        json.put("type", type);
        if (category != null) json.put("category", category);
        if (group != null) json.put("group", group);
        if (autocraftable != null) json.put("gregtech_autocraftable", autocraftable);
        json.put("ingredients", ingredients);
        json.put("result", result(resultId, count));
        return json;
    }

    public static Map<String, Object> smelting(String ingredientSpec, String resultId, float experience, int cookingTime) {
        var json = new LinkedHashMap<String, Object>();
        json.put("type", "minecraft:smelting");
        json.put("ingredient", spec(ingredientSpec));
        json.put("result", result(resultId));
        json.put("experience", experience);
        json.put("cookingtime", cookingTime);
        return json;
    }
}
