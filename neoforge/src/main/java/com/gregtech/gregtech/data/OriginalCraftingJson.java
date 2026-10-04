package com.gregtech.gregtech.data;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** JSON boundary for original grid recipes, owned by the existing Native reloadable pack. */
public final class OriginalCraftingJson {
    private OriginalCraftingJson() {}
    private static JsonObject recipe(String type, String group, CraftingBookCategory category, ItemStack output) {
        if (output.isEmpty() || !output.getComponentsPatch().isEmpty())
            throw new IllegalArgumentException("Original grid output must be a plain registered stack");
        var json = new JsonObject(); json.addProperty("type", type); json.addProperty("group", group);
        json.addProperty("category", category.name().toLowerCase(Locale.ROOT));
        var result = new JsonObject(); result.addProperty("id", BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
        result.addProperty("count", output.getCount()); json.add("result", result); return json;
    }
    public static void shapeless(Map<ResourceLocation, byte[]> data, ResourceLocation id, String group,
                                 CraftingBookCategory category, ItemStack output, List<Ingredient> ingredients) {
        shapeless(data, id, group, category, output, ingredients, true, false);
    }
    /** Source AdvancedCrafting1ToY/XToY permissions survive the reloadable data boundary. */
    public static void shapeless(Map<ResourceLocation, byte[]> data, ResourceLocation id, String group,
                                 CraftingBookCategory category, ItemStack output, List<Ingredient> ingredients,
                                 boolean autocraftable) {
        shapeless(data, id, group, category, output, ingredients, autocraftable, true);
    }
    /** Original single-input variants retain their grid position through JSON and network sync. */
    public static void formConversion(Map<ResourceLocation, byte[]> data, ResourceLocation id, String group,
                                     CraftingBookCategory category, ItemStack output, List<Ingredient> ingredients,
                                     com.gregtech.gregtech.content.recipe.FormConversionSelector selector) {
        shapeless(data, id, group, category, output, ingredients, false, true, selector);
    }
    private static void shapeless(Map<ResourceLocation, byte[]> data, ResourceLocation id, String group,
                                 CraftingBookCategory category, ItemStack output, List<Ingredient> ingredients,
                                 boolean autocraftable, boolean sourcePermission) {
        shapeless(data, id, group, category, output, ingredients, autocraftable, sourcePermission,
                com.gregtech.gregtech.content.recipe.FormConversionSelector.NONE);
    }
    private static void shapeless(Map<ResourceLocation, byte[]> data, ResourceLocation id, String group,
                                 CraftingBookCategory category, ItemStack output, List<Ingredient> ingredients,
                                 boolean autocraftable, boolean sourcePermission,
                                 com.gregtech.gregtech.content.recipe.FormConversionSelector selector) {
        var json = recipe(sourcePermission ? "gregtech:tool_shapeless" : "minecraft:crafting_shapeless", group, category, output); var values = new JsonArray();
        if (sourcePermission) json.addProperty("gregtech_autocraftable", autocraftable);
        if (selector.variants() > 0) {
            json.addProperty("gregtech_form_variants", selector.variants());
            json.addProperty("gregtech_form_offset", selector.offset());
        }
        for (var ingredient : ingredients) values.add(Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow());
        json.add("ingredients", values); put(data, id, json);
    }
    public static void shaped(Map<ResourceLocation, byte[]> data, ResourceLocation id, String group,
                              CraftingBookCategory category, ItemStack output, String[] pattern,
                              Map<Character, Ingredient> ingredients, boolean mirror) {
        var json = recipe("gregtech:tool_shaped", group, category, output); var rows = new JsonArray();
        var keys = new JsonObject(); var used = new LinkedHashSet<Character>();
        for (String row : pattern) { rows.add(row); for (char c : row.toCharArray()) if (c != ' ') used.add(c); }
        for (char c : used) keys.add(String.valueOf(c), Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredients.get(c)).getOrThrow());
        json.add("pattern", rows); json.add("key", keys); json.addProperty("allow_mirror", mirror); put(data, id, json);
    }
    private static void put(Map<ResourceLocation, byte[]> data, ResourceLocation recipeId, JsonObject json) {
        var location = ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(), "recipe/" + recipeId.getPath() + ".json");
        // Original RuntimeRecipeLifecycle replaces generated ids with first-entry precedence.
        data.putIfAbsent(location, json.toString().getBytes(StandardCharsets.UTF_8));
    }
}
