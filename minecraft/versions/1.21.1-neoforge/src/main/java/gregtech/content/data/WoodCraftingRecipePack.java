package gregtech.content.data;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Original wood dictionary patterns over shared catalogs; the pack itself is owned by {@link RecipePublishing}. */
public final class WoodCraftingRecipePack {
    private static final Gson GSON = new Gson();

    private WoodCraftingRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        int before = generated.size();
        for (String row : gregtech.content.recipe.GTWoodRecipes.CRAFT_ROWS) addRow(generated, row);
        for (String row : gregtech.content.recipe.NaturalWoodCraftingRows.ROWS) addRow(generated, row);
        for (String row : gregtech.content.recipe.VanillaWoodCraftingRows.ROWS) addRow(generated, row);
        for (String row : gregtech.content.recipe.TreatedWoodCraftingRows.ROWS) addRow(generated, row);
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Original wood crafting datapack: {} rows; source-explicit skipped tree rod rows={}",
                        generated.size() - before,
                        gregtech.content.recipe.GTWoodRecipes.SKIPPED_CRAFT_ROWS.length);
    }

    private static net.minecraft.world.item.ItemStack resolve(String spec) {
        if (spec.startsWith("item:"))
            return new net.minecraft.world.item.ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.substring(5))));
        if (spec.startsWith("material:")) {
            String[] parts = spec.split(":", 3);
            return gregtech.content.registry.GTItems.getStack(
                    prefix(parts[1]), gregtech.engine.material.GTMaterialRegistry.get(parts[2]), 1);
        }
        return gregtech.content.recipe.WoodCraftingRecipeInjector.resolve(spec);
    }

    private static Map<String, Object> ingredient(String spec) {
        if (spec.startsWith("tag:"))
            return Map.of("tag", ResourceLocation.parse(spec.substring(4)).toString());
        var stack = resolve(spec);
        if (stack.isEmpty()) throw new IllegalStateException("Unresolved original wood ingredient " + spec);
        return Map.of("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static void addRow(Map<ResourceLocation, byte[]> data, String encoded) {
        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 7) throw new IllegalArgumentException("Malformed wood row " + encoded);
        Map<String, Object> keys = new java.util.LinkedHashMap<>();
        for (String key : parts[4].split(";")) {
            if (key.length() < 3 || key.charAt(1) != '=') throw new IllegalArgumentException("Bad wood key " + key);
            keys.put(key.substring(0, 1), ingredient(key.substring(2)));
        }
        int star = parts[5].lastIndexOf('*');
        String outputSpec = star < 0 ? parts[5] : parts[5].substring(0, star);
        int count = star < 0 ? 1 : Integer.parseInt(parts[5].substring(star + 1));
        var output = resolve(outputSpec);
        if (output.isEmpty()) throw new IllegalStateException("Unresolved original wood output " + outputSpec);
        Map<String, Object> recipe = new java.util.LinkedHashMap<>();
        boolean shaped = parts[1].equals("shaped");
        recipe.put("type", shaped ? "gt6ce:tool_shaped" : "minecraft:crafting_shapeless");
        recipe.put("group", "gt.wood");
        recipe.put("category", parts[0].startsWith("treated/") ? "misc" : "building");
        if (shaped) {
            recipe.put("pattern", parts[3].split("/", -1));
            recipe.put("key", keys);
            recipe.put("allow_mirror", parts[2].equals("mirror"));
        } else recipe.put("ingredients", new ArrayList<>(keys.values()));
        recipe.put(
                "result",
                Map.of("id", BuiltInRegistries.ITEM.getKey(output.getItem()).toString(), "count", count));
        var key = ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/wood/" + parts[0] + ".json");
        if (data.putIfAbsent(key, GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8)) != null)
            throw new IllegalStateException("Duplicate wood recipe " + key);
    }

    private static gregtech.content.data.MaterialPrefix prefix(String id) {
        return switch (id) {
            case "bolt" -> gregtech.content.data.MaterialPrefix.bolt;
            case "plate" -> gregtech.content.data.MaterialPrefix.plate;
            case "gearGtSmall" -> gregtech.content.data.MaterialPrefix.gearGtSmall;
            case "gearGt" -> gregtech.content.data.MaterialPrefix.gearGt;
            default -> throw new IllegalArgumentException("Unknown wood prefix " + id);
        };
    }
}
