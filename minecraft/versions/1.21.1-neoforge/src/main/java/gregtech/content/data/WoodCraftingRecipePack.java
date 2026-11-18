package gregtech.content.data;

import com.google.gson.Gson;
import gregtech.content.recipe.GTWoodRecipes;
import gregtech.content.recipe.TreatedWoodCraftingRows;
import gregtech.content.recipe.WoodCraftingRecipeInjector;
import gregtech.content.recipe.WoodCraftingRow;
import gregtech.content.recipe.WoodCraftingRow.IngredientSpec;
import gregtech.content.recipe.WoodCraftingRow.ItemSpec;
import gregtech.content.recipe.WoodCraftingRow.Shaped;
import gregtech.content.recipe.WoodCraftingRow.Shapeless;
import gregtech.content.recipe.WoodCraftingRow.Tag;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Original wood dictionary patterns over shared catalogs; the pack itself is owned by {@link RecipePublishing}. */
public final class WoodCraftingRecipePack {
    private static final Gson GSON = new Gson();

    private WoodCraftingRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        int before = generated.size();
        for (var row : GTWoodRecipes.buildingRows()) addRow(generated, row, "building");
        for (var row : TreatedWoodCraftingRows.ROWS) addRow(generated, row, "misc");
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Original wood crafting datapack: {} rows; source-explicit skipped tree rod rows={}",
                        generated.size() - before,
                        GTWoodRecipes.SKIPPED_CRAFT_ROWS.size());
    }

    private static String itemId(WoodCraftingRow row, ItemSpec spec) {
        var stack = WoodCraftingRecipeInjector.resolve(spec);
        if (stack.isEmpty())
            throw new IllegalStateException(row.path() + " (" + row.source() + "): unresolved wood item " + spec);
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static Map<String, Object> ingredient(WoodCraftingRow row, IngredientSpec spec) {
        if (spec instanceof Tag tag)
            return Map.of("tag", ResourceLocation.parse(tag.id()).toString());
        return Map.of("item", itemId(row, (ItemSpec) spec));
    }

    private static void addRow(Map<ResourceLocation, byte[]> data, WoodCraftingRow row, String category) {
        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("type", row.shape() instanceof Shaped ? "gt6ce:tool_shaped" : "minecraft:crafting_shapeless");
        recipe.put("group", "gt.wood");
        recipe.put("category", category);
        if (row.shape() instanceof Shaped shaped) {
            Map<String, Object> keys = new LinkedHashMap<>();
            for (var key : shaped.keys()) keys.put(Character.toString(key.symbol()), ingredient(row, key.ingredient()));
            recipe.put("pattern", shaped.pattern());
            recipe.put("key", keys);
            recipe.put("allow_mirror", shaped.allowMirror());
        } else {
            var shapeless = (Shapeless) row.shape();
            recipe.put(
                    "ingredients",
                    shapeless.ingredients().stream()
                            .map(spec -> ingredient(row, spec))
                            .toList());
        }
        recipe.put("result", Map.of("id", itemId(row, row.output()), "count", row.count()));
        var key = ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/wood/" + row.path() + ".json");
        if (data.putIfAbsent(key, GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8)) != null)
            throw new IllegalStateException("Duplicate wood recipe " + key);
    }
}
