package gregtech.compat.kubejs;

import com.google.gson.JsonObject;

/** Shared object conversions for the item input and output components. */
final class MachineItemJson {
    private MachineItemJson() {}

    static void writeIdentity(JsonObject json, String item, String tag, String material, String form) {
        if (item != null) json.addProperty("item", item);
        else if (tag != null) json.addProperty("tag", tag);
        else {
            json.addProperty("material", material);
            json.addProperty("form", form);
        }
    }

    static String text(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : fallback;
    }

    static JsonObject map(java.util.Map<?, ?> values) {
        var json = new JsonObject();
        values.forEach((key, value) -> {
            if (value instanceof Number number) json.addProperty(String.valueOf(key), number);
            else if (value instanceof Boolean flag) json.addProperty(String.valueOf(key), flag);
            else json.addProperty(String.valueOf(key), String.valueOf(value));
        });
        return json;
    }
}
