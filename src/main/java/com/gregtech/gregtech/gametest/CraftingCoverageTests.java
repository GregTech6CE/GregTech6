package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.registry.GTTechnological;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Finds obtainable-in-survival items that have no recipe anywhere: neither a datapack recipe
 * (crafting/smelting/…) nor a row in any {@link RecipeMap}.
 *
 * <p>This turns the user's "X has no recipe" reports into a precise, reproducible list. GT6 gives
 * every one of these items a recipe ({@code Loader_Tools} for tools, {@code Loader_MultiTileEntities}
 * for the tool blocks, {@code Loader_Recipes_Other}/{@code _Chem} for the machine components), so an
 * entry here is a real gap rather than a design choice — except for the deliberate exclusions listed
 * in {@link #NO_RECIPE_BY_DESIGN}.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CraftingCoverageTests {

    /**
     * Items that are intentionally craftless: they are only ever produced by a machine, found in
     * worldgen, or are display/debug entries ({@code docs/PORTING_REMAINING_2026-09-14.md} §7).
     *
     * <p>The selector circuits are <em>not</em> in this set: GT6 crafts them
     * ({@code ItemIntegratedCircuit:58-85}), so they carry recipes of their own
     * ({@code HandCraftingTests}).</p>
     */
    private static final java.util.Set<String> NO_RECIPE_BY_DESIGN = java.util.Set.of(
            "circuit_part_enderpearl",      // ender pearl circuit parts come from the assembler only
            "circuit_part_endereye");

    /**
     * GT6's {@code CS.VN} lists 16 voltage names but the component crafting loops
     * ({@code MultiItemTechnological:405-423}) stop at index 9 (PUV1), so every component above PUV1
     * has no recipe in the original either — those tiers exist as items only.
     */
    private static boolean aboveGt6RecipeTiers(String id) {
        if (!id.startsWith("compact_")) return false;
        for (String tier : new String[]{"puv2", "puv3", "puv4", "puv5", "xv"}) {
            if (id.endsWith("_" + tier)) return true;
        }
        return false;
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void itemsWithoutAnyRecipeAreReported(GameTestHelper helper) {
        java.util.Set<Item> produced = new java.util.HashSet<>();
        int datapack = 0;
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            ItemStack result;
            try {
                result = recipe.getResultItem(helper.getLevel().registryAccess());
            } catch (RuntimeException e) {
                continue;
            }
            if (result != null && !result.isEmpty()) {
                produced.add(result.getItem());
                datapack++;
            }
            for (var ingredient : recipe.getIngredients()) {
                // ingredients are inputs, not results - recorded separately below
                if (ingredient.isEmpty()) continue;
            }
        }
        int machineRows = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            for (Recipe recipe : map.mRecipeList) {
                for (ItemStack out : recipe.mOutputs) {
                    if (out != null && !out.isEmpty()) {
                        produced.add(out.getItem());
                        machineRows++;
                    }
                }
            }
        }

        var missing = new TreeMap<String, java.util.List<String>>();
        int checked = 0;
        for (var entry : GTTechnological.all()) {
            if (!entry.isPresent()) continue;
            Item item = entry.get();
            checked++;
            if (produced.contains(item) || byDesign(id(item))) continue;
            missing.computeIfAbsent("technological", k -> new java.util.ArrayList<>()).add(id(item));
        }
        for (var entry : GTToolItems.all().values()) {
            if (!entry.isPresent()) continue;
            Item item = entry.get();
            checked++;
            if (produced.contains(item) || byDesign(id(item))) continue;
            missing.computeIfAbsent("tools", k -> new java.util.ArrayList<>()).add(id(item));
        }
        int total = missing.values().stream().mapToInt(java.util.List::size).sum();

        var json = new TreeMap<String, Object>();
        json.put("checked", checked);
        json.put("datapackResults", datapack);
        json.put("machineRows", machineRows);
        json.put("withoutRecipe", total);
        json.put("byFamily", new TreeMap<>(missing));
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("../../docs/items-without-recipes.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/items-without-recipes.json: " + e);
            return;
        }
        helper.assertTrue(checked > 300, "items checked for a recipe: " + checked);
        helper.succeed();
    }

    private static boolean byDesign(String id) {
        for (String prefix : NO_RECIPE_BY_DESIGN) if (id.startsWith(prefix)) return true;
        return aboveGt6RecipeTiers(id);
    }

    private static String id(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "unknown" : key.getPath();
    }

    /** Every checked family must resolve to a registered item, so the report is never empty by accident. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void reportFamiliesArePopulated(GameTestHelper helper) {
        var families = new LinkedHashMap<String, Integer>();
        families.put("technological", GTTechnological.all().size());
        families.put("tools", GTToolItems.all().size());
        for (Map.Entry<String, Integer> entry : families.entrySet()) {
            helper.assertTrue(entry.getValue() > 0, entry.getKey() + " family is registered");
        }
        helper.assertTrue(!net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(ResourceLocation.fromNamespaceAndPath("gregtech", "extruder_shape_plate")).equals(
                        net.minecraft.world.item.Items.AIR), "a known tech item resolves by id");
        helper.succeed();
    }
}
