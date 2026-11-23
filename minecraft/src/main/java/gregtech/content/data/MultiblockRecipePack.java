package gregtech.content.data;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Crafting recipes for the multiblock machines and their structural parts, generated from the
 * original GT6 "Multiblock Machines" registrations (see {@link MultiblockCraftingRecipes}).
 * <p>
 * Before this pack the multiblock family had no recipe at all: {@link BasicMachineRecipePack}
 * skips the multiblock machine types and no resource-pack recipe existed for the controller
 * blocks, so every one of them was unobtainable in survival.
 * </p>
 */
public final class MultiblockRecipePack {
    /** Material used when a recipe is not bound to a machine spec. */
    private static final String DEFAULT_MATERIAL = "StainlessSteel";

    // Loader_MultiTileEntities' Umber/Livingrock/Holystone/Betweenstone utilities use external
    // OP.stone entries. Without those mods the original ore ingredient cannot match anything;
    // omit that unavailable row instead of letting the machine-ingredient fallback use iron.
    private static final Set<String> OPTIONAL_STONE_INPUTS = Set.of(
            "gt6ce:stone_umber_stone", "gt6ce:stone_livingrock_stone",
            "gt6ce:stone_holystone_stone", "gt6ce:stone_betweenstone_stone");

    private static final Set<String> EMITTED = new HashSet<>();

    private MultiblockRecipePack() {}

    /** Port block ids that received a recipe in the last generated pack. */
    public static Set<String> emittedBlockIds() {
        return Collections.unmodifiableSet(EMITTED);
    }

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        EMITTED.clear();

        for (MultiblockCraftingRecipes.Entry entry : MultiblockCraftingRecipes.ENTRIES) {
            // The native wall family owns these rows, including its complete tool tags.
            if (OriginalHandCraftingRows.hasMetalWallRecipe(entry.blockId())) continue;
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("gt6ce", entry.blockId());
            Block block = Mc.block(blockId);
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) continue;
            Item item = block.asItem();
            if (item == net.minecraft.world.item.Items.AIR) continue;

            GTMaterial material = materialOf(block);
            Map<Character, Object> ingredients = new TreeMap<>();
            entry.recipe()
                    .keys()
                    .forEach((symbol, ingredient) ->
                            ingredients.put(symbol, MachineRecipeIngredients.resolve(ingredient, material)));
            Map<String, Object> recipe = buildRecipe(entry.recipe().rows(), ingredients, item);
            if (recipe == null) continue;

            GeneratedRecipeJson.put(generated, "machines/multiblock/" + entry.blockId(), recipe);
            EMITTED.add(entry.blockId());
        }

        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Original multiblock crafting datapack: {} emitted, {} not registered or unresolved",
                        EMITTED.size(),
                        MultiblockCraftingRecipes.ENTRIES.size() - EMITTED.size());
        for (var row : java.util.stream.Stream.concat(
                        OriginalSurvivalRecipes.allRows().stream(), UtilityCraftingRows.ROWS.stream())
                .toList()) {
            var blockId = ResourceLocation.fromNamespaceAndPath("gt6ce", row.id());
            Item item = Mc.item(blockId);
            if (item == null || item == net.minecraft.world.item.Items.AIR)
                throw new IllegalStateException("Unregistered original recipe output " + row.id());
            if (!concreteInputsAvailable(row)) continue;
            var pattern = new ArrayList<>(List.of(row.rows()));
            while (!pattern.isEmpty() && pattern.get(pattern.size() - 1).isBlank()) {
                pattern.remove(pattern.size() - 1);
            }
            var ingredients = MachineRecipeIngredients.resolveAll(row.keys(), row.material(), 1);
            var recipe = buildRecipe(pattern, ingredients, item);
            if (recipe == null) throw new IllegalStateException("Unresolved original recipe " + row.id());
            recipe.put(
                    "result",
                    GeneratedRecipeJson.result(row.id().contains(":") ? row.id() : "gt6ce:" + row.id(), row.count()));
            GeneratedRecipeJson.put(generated, "survival_repairs/" + row.id(), recipe);
            EMITTED.add(row.id());
        }

        addClayUtilityRecipes(generated);
    }

    private static boolean concreteInputsAvailable(OriginalSurvivalRecipes.Row row) {
        boolean available = true;
        for (String spec : row.keys().values()) {
            if (!spec.startsWith("item:")) continue;
            String id = spec.substring(5);
            Item input = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (input != null && input != net.minecraft.world.item.Items.AIR) continue;
            if (!OPTIONAL_STONE_INPUTS.contains(id))
                throw new IllegalStateException("Unregistered original recipe input " + id + " for " + row.id());
            available = false;
        }
        return available;
    }

    /** Original raw utilities return their clay before firing; only firing produces ceramic parts. */
    private static void addClayUtilityRecipes(Map<ResourceLocation, byte[]> generated) {
        for (var row : gregtech.content.recipe.ClayUtilityCatalog.ROWS) {
            for (String type : List.of("smelting", "blasting")) {
                var firing = new HashMap<String, Object>();
                firing.put("type", "minecraft:" + type);
                firing.put("ingredient", Map.of("item", "gt6ce:" + row.raw()));
                firing.put("result", GeneratedRecipeJson.result("gt6ce:" + row.fired()));
                firing.put("experience", 0);
                firing.put("cookingtime", type.equals("smelting") ? 200 : 100);
                clayRecipe(generated, type + "_" + row.raw(), firing);
            }
            var reclaim = new HashMap<String, Object>();
            reclaim.put("type", "minecraft:crafting_shapeless");
            reclaim.put("ingredients", List.of(Map.of("item", "gt6ce:" + row.raw())));
            reclaim.put("result", GeneratedRecipeJson.result("minecraft:clay_ball", row.clayBalls()));
            clayRecipe(generated, "reclaim_" + row.raw(), reclaim);
        }
        // MultiItemRandomTools also permits the vertically reversed crossing layout.
        var crossing = new HashMap<String, Object>();
        crossing.put("type", "gt6ce:tool_shaped");
        crossing.put("pattern", List.of(" C ", "CCC", "kCR"));
        crossing.put(
                "key",
                Map.of(
                        "C",
                        Map.of("tag", "gt6ce:clay_balls"),
                        "k",
                        Map.of("item", "gt6ce:tool_knife"),
                        "R",
                        Map.of("item", "gt6ce:tool_rolling_pin")));
        crossing.put("allow_mirror", false);
        crossing.put("result", GeneratedRecipeJson.result("gt6ce:clay_crossing"));
        clayRecipe(generated, "form_clay_crossing_flipped", crossing);
    }

    private static void clayRecipe(Map<ResourceLocation, byte[]> generated, String name, Map<String, Object> recipe) {
        GeneratedRecipeJson.put(generated, "ceramic_utilities/" + name, recipe);
    }

    private static GTMaterial materialOf(Block block) {
        if (block instanceof BasicMachineBlock machine) {
            return machine.basicSpec().material();
        }
        return GTMaterialRegistry.get(DEFAULT_MATERIAL);
    }

    private static Map<String, Object> buildRecipe(
            List<String> pattern, Map<Character, Object> ingredients, Item resultItem) {
        if (ingredients.isEmpty() || pattern.isEmpty()) return null;
        Map<String, Object> key = new HashMap<>();
        for (var pair : ingredients.entrySet()) {
            key.put(String.valueOf(pair.getKey()), pair.getValue());
        }

        ResourceLocation resultId = Mc.itemId(resultItem);
        if (resultId == null) return null;

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gt6ce:tool_shaped");
        recipe.put("pattern", pattern);
        recipe.put("key", key);
        // Multiblock recipes come from GT6's aRegistry.add(...) rows as well: CR.DEF_REV, no MIR.
        recipe.put("allow_mirror", false);
        recipe.put("result", GeneratedRecipeJson.result(resultId.toString()));
        recipe.put("group", "gregtech_multiblocks");
        return recipe;
    }
}
