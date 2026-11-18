package gregtech.content.data;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Shared JSON serialization helpers for dynamic {@link RecipePublishing} packs across loaders. */
public final class GeneratedRecipeJson {
    private static final Gson GSON = new Gson();

    private GeneratedRecipeJson() {}

    /** Emits a recipe JSON under {@code gt6ce:recipes/<path>.json} (1.20.1) or {@code gt6ce:recipe/<path>.json} (1.21.1). */
    public static void put(Map<ResourceLocation, byte[]> generated, String path, Map<String, Object> recipe) {
        generated.put(
                ResourceLocation.fromNamespaceAndPath(
                        "gt6ce",
//? if >=1.21.1 {
                        /*"recipe/" + path + ".json"),
*///?} else {
                        "recipes/" + path + ".json"),
//?}
                GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
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

    public static Map<String, Object> result(String itemId, int count) {
        var out = new LinkedHashMap<String, Object>();
//? if >=1.21.1 {
        /*out.put("id", itemId);
*///?} else {
        out.put("item", itemId);
//?}
        out.put("count", count);
        return out;
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
//? if >=1.21.1 {
        /*json.put("result", Map.of("id", resultId));
*///?} else {
        json.put("result", resultId);
//?}
        json.put("experience", experience);
        json.put("cookingtime", cookingTime);
        return json;
    }
}
