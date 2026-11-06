package gregtech.content.data;

import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Required vanilla overrides; {@link RecipePublishing} owns the pack that publishes them. */
public final class VanillaCraftingRecipePack {
    /** Replaced by {@code VanillaWoodCraftingRows}: GT6 NERFED_WOOD log/stripped-log counts. */
    private static final String[] REPLACED_PLANK_RECIPES = {
            "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
            "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
            "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:crimson_planks",
            "minecraft:warped_planks",
    };

    private VanillaCraftingRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        int before = generated.size();
        for (var row : gregtech.content.recipe.VanillaCraftingReplacements.rows()) {
            var keys = new LinkedHashMap<String, Object>();
            for (var entry : row.keys().entrySet())
                if (row.pattern() == null || row.pattern().indexOf(entry.getKey()) >= 0)
                    keys.put(entry.getKey().toString(), ingredient(entry.getValue()));
            var json = new LinkedHashMap<String, Object>();
            json.put("type", row.pattern() == null ? "minecraft:crafting_shapeless" : "gt6ce:tool_shaped");
            json.put("group", "gt.vanilla");
            json.put("category", "misc");
            if (row.pattern() == null) json.put("ingredients", new ArrayList<>(keys.values()));
            else {
                json.put("pattern", row.pattern().split("/", -1));
                json.put("key", keys);
                json.put("allow_mirror", true);
            }
            var result = (Map<?, ?>) ingredient(row.result().startsWith("material:") ? row.result() : "item:" + row.result());
//? if >=1.21.1 {
            /*json.put("result", Map.of("id", result.get("item"), "count", row.count()));
*///?} else {
            json.put("result", Map.of("item", result.get("item"), "count", row.count()));
//?}
            var id = ResourceLocation.parse(row.id());
            generated.put(
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), directory() + id.getPath() + ".json"),
                    new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
        }
        gregtech.content.recipe.TorchCraftingRecipes.recipes().forEach((id, json) -> generated.put(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), directory() + id.getPath() + ".json"),
                new Gson().toJson(json).getBytes(StandardCharsets.UTF_8)));
        gregtech.content.recipe.HandMaterialCraftingRecipes.recipes().forEach((id, json) -> generated.put(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), directory() + id.getPath() + ".json"),
                new Gson().toJson(json).getBytes(StandardCharsets.UTF_8)));
        for (var recipeId : REPLACED_PLANK_RECIPES) disablePlanks(generated, recipeId);
        com.mojang.logging.LogUtils.getLogger()
                .info("[gregtech] Original vanilla overrides: {} rows", generated.size() - before);
    }

    private static String directory() {
//? if >=1.21.1 {
        /*return "recipe/";
*///?} else {
        return "recipes/";
//?}
    }

    /** A failing condition disables the vanilla recipe; the override layer makes this file win by id. */
    private static void disablePlanks(Map<ResourceLocation, byte[]> generated, String recipeId) {
        var id = ResourceLocation.parse(recipeId);
        var json = new LinkedHashMap<String, Object>();
        json.put("type", "minecraft:crafting_shapeless");
//? if >=1.21.1 {
        /*json.put("neoforge:conditions", List.of(Map.of("type", "neoforge:false")));
*///?} else {
        json.put("conditions", List.of(Map.of("type", "forge:false")));
//?}
        json.put("ingredients", List.of(Map.of("item", "minecraft:barrier")));
//? if >=1.21.1 {
        /*json.put("result", Map.of("id", "minecraft:barrier", "count", 1));
*///?} else {
        json.put("result", Map.of("item", "minecraft:barrier"));
//?}
        generated.put(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), directory() + id.getPath() + ".json"),
                new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    private static Object ingredient(String spec) {
        if (spec.startsWith("tag:")) {
            // Vanilla has no wooden_chests tag; preserve GT6's ordinary chest ingredient.
            return spec.equals("tag:minecraft:wooden_chests")
                    ? Map.of("item", "minecraft:chest")
                    : Map.of("tag", spec.substring(4));
        }
        if (spec.startsWith("item:")) return Map.of("item", spec.substring(5));
        // GT6 uses OD.craftingFirestarter, including the two early wooden fire starters.
        if (spec.equals("firestarter")) return Map.of("tag", "gt6ce:crafting_firestarters");
        if (spec.startsWith("tool:"))
            return Map.of(
                    "item",
                    "gt6ce:tool_"
                            + gregtech.engine.tool.GTToolType.valueOf(spec.substring(5).toUpperCase(java.util.Locale.ROOT))
                                    .id());
        String[] parts = spec.split(":", 3);
        var stack = gregtech.content.registry.GTItems.getStack(
                gregtech.engine.prefix.PrefixRegistry.byName(parts[1]),
                gregtech.engine.material.GTMaterialRegistry.get(parts[2]),
                1);
        if (stack.isEmpty()) throw new IllegalStateException("Unbound original vanilla ingredient " + spec);
        stack = gregtech.engine.material.MaterialUnification.canonical(stack);
        return Map.of("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
