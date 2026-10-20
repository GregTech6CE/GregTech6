package com.gregtech.gregtech.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.registry.GTConstructionVariants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;

/** Keep existing data packs' palette ingredients usable with the new registered color items. */
public final class ConstructionRecipeMigration {
    private ConstructionRecipeMigration() {}

    public static void migrate(JsonElement json) {
        if (!json.isJsonObject()) return;
        JsonObject recipe = json.getAsJsonObject();
        String color = recipe.has("construction_color") ? recipe.get("construction_color").getAsString() : "";
        for (String key : java.util.List.of("key", "ingredients", "ingredient", "base", "addition", "template"))
            if (recipe.has(key)) ingredients(recipe.get(key), color);
        if (recipe.has("result")) result(recipe.get("result"), color);
    }

    private static String family(String id) {
        if (!id.startsWith("gt6ce:")) return null;
        String path = id.substring("gt6ce:".length());
//? if >=1.21.1 {
        /*var block = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(id));
*///?} else {
        var block = BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(id));
//?}
        return block != Blocks.AIR && GTConstructionVariants.legacy(block) ? path : null;
    }

    private static void ingredients(JsonElement element, String color) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> ingredients(child, color));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String family = object.has("item") && object.get("item").isJsonPrimitive()
                    ? family(object.get("item").getAsString()) : null;
            if (family != null) {
                if (color.isEmpty()) {
                    object.remove("item");
                    object.addProperty("tag", "gt6ce:construction/" + family);
                } else object.addProperty("item", "gt6ce:" + family + "_" + color);
            } else object.entrySet().forEach(entry -> ingredients(entry.getValue(), color));
        }
    }

    private static void result(JsonElement element, String color) {
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        String key = object.has("id") ? "id" : "item";
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return;
        String family = family(object.get(key).getAsString());
        if (family != null) object.addProperty(key, "gt6ce:" + (color.isEmpty()
                ? GTConstructionVariants.defaultId(family) : family + "_" + color));
    }
}
