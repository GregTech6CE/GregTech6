package gregtech.buildtools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;

/**
 * The single conversion from the shared 1.20.1 recipe documents to the 1.21.1 native shape.
 *
 * <p>Only the parts that actually differ are rewritten: the item stack result switches from
 * {@code item} to {@code id}. Ingredient value objects already are the 1.21.1 form — its codec
 * rejects a bare item string — so they pass through after validation. Custom serializers keep
 * their own fields and are decoded by the mod's codecs, but their result is a vanilla stack and
 * follows the same rename. Anything this class does not understand fails the build instead of
 * producing a silently wrong resource.</p>
 */
public final class RecipeResourceConverter {
    public static final Set<String> KNOWN_TYPES = Set.of(
            "minecraft:crafting_shaped",
            "minecraft:crafting_shapeless",
            "minecraft:smelting",
            "gt6ce:tool_shaped",
            "gt6ce:tool_shapeless",
            "gt6ce:book_binding",
            "gt6ce:spike_conversion",
            "gt6ce:tool_repair");

    private RecipeResourceConverter() {}

    /** Converts one recipe document; {@code resource} only appears in diagnostics. */
    public static JsonObject convertRecipe(String resource, JsonObject source) {
        String type = requiredString(source, "type", resource);
        if (!KNOWN_TYPES.contains(type)) throw new ConversionException(resource, "unsupported recipe type " + type);
        var out = new JsonObject();
        for (var entry : source.entrySet()) {
            switch (entry.getKey()) {
                case "result" -> out.add("result", convertResult(resource, entry.getValue()));
                case "ingredients" -> out.add("ingredients", convertList(resource, entry.getValue()));
                case "key" -> out.add("key", convertKey(resource, entry.getValue()));
                case "ingredient" -> out.add("ingredient", convertIngredient(resource, entry.getValue()));
                default -> out.add(entry.getKey(), entry.getValue());
            }
        }
        if (!"gt6ce:tool_repair".equals(type) && !out.has("result"))
            throw new ConversionException(resource, "missing result");
        return out;
    }

    /** A tag document keeps its values; only the directory changes. */
    public static JsonObject convertTag(String resource, JsonObject source) {
        if (!source.has("values") || !source.get("values").isJsonArray())
            throw new ConversionException(resource, "tag has no values array");
        return source;
    }

    private static JsonElement convertResult(String resource, JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            var out = new JsonObject();
            out.addProperty("id", value.getAsString());
            return out;
        }
        if (!value.isJsonObject()) throw new ConversionException(resource, "result is not an object or id");
        var source = value.getAsJsonObject();
        for (var key : source.keySet())
            if (!key.equals("item") && !key.equals("count") && !key.equals("id"))
                throw new ConversionException(resource, "unsupported result field " + key);
        var out = new JsonObject();
        if (source.has("id")) out.add("id", source.get("id"));
        else if (source.has("item")) out.addProperty("id", requiredString(source, "item", resource));
        else throw new ConversionException(resource, "result has neither item nor id");
        if (source.has("count")) {
            var count = source.get("count");
            if (!count.isJsonPrimitive() || !count.getAsJsonPrimitive().isNumber())
                throw new ConversionException(resource, "result count is not a number");
            int amount = count.getAsInt();
            // ItemStack.CODEC accepts 1..99 only.
            if (amount < 1 || amount > 99)
                throw new ConversionException(resource, "result count out of range " + amount);
            out.addProperty("count", amount);
        }
        return out;
    }

    private static JsonElement convertList(String resource, JsonElement value) {
        if (!value.isJsonArray()) throw new ConversionException(resource, "ingredients is not a list");
        var out = new JsonArray();
        for (var entry : value.getAsJsonArray()) out.add(convertIngredient(resource, entry));
        return out;
    }

    private static JsonElement convertKey(String resource, JsonElement value) {
        if (!value.isJsonObject()) throw new ConversionException(resource, "key is not an object");
        var out = new JsonObject();
        for (var entry : value.getAsJsonObject().entrySet())
            out.add(entry.getKey(), convertIngredient(resource, entry.getValue()));
        return out;
    }

    private static JsonElement convertIngredient(String resource, JsonElement value) {
        if (value.isJsonArray()) {
            var out = new JsonArray();
            for (var entry : value.getAsJsonArray()) out.add(convertIngredient(resource, entry));
            return out;
        }
        if (!value.isJsonObject()) throw new ConversionException(resource, "ingredient is not an object or list");
        var source = value.getAsJsonObject();
        // 1.21.1's Ingredient codec tries an array of values and then one value object; a bare
        // item string is rejected, so the object form is already the native expression here.
        if (source.has("type")) return source;
        if (source.size() == 1 && source.has("item")) {
            requiredString(source, "item", resource);
            return source;
        }
        if (source.size() == 1 && source.has("tag")) {
            requiredString(source, "tag", resource);
            return source;
        }
        throw new ConversionException(resource, "unsupported ingredient " + source.keySet());
    }

    private static String requiredString(JsonObject source, String key, String resource) {
        var value = source.get(key);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString())
            throw new ConversionException(resource, key + " must be a string");
        return value.getAsString();
    }

    /** A source document that cannot be converted; the message names the resource and the reason. */
    public static final class ConversionException extends RuntimeException {
        public ConversionException(String resource, String reason) {
            super(resource + ": " + reason);
        }
    }
}
