package com.gregtech.gregtech.loaders;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GT6 {@code BlockStones:257-487} — the crafting-grid rows of every masonry variant of every rock type.
 *
 * <p>The rows are shaped ({@code CR.shaped}) and shapeless ({@code CR.shapeless}) hand recipes GT6
 * registers per equal-block group, i.e. once per rock type per variant:</p>
 *
 * <pre>
 * STONE group     4 rocks → cobble        "XX"/"XX"
 *                 stone + file → small gear          "X "/" f"
 *                 3 rocks → 1 stone stairs            " X"/"XX"   (mirrored)
 *                 2 rocks → 1 cobble slab             "  "/"XX"
 * COBBLE group    2 cobble → 4 cobble slabs           "  "/"XX"
 *                 3 cobble → 4 stairs                 " X"/"XX"   (mirrored)
 *                 6 cobble → 6 cobblestone walls      "XXX"/"XXX" (mirrored)
 * BRICKS group    bricks + chisel → cracked bricks    "y"/"X"
 *                 bricks + hammer → cracked bricks    "h"/"X"
 *                 bricks + iron stick → reinforced    "Se"/"X "   (mirrored)
 *                 bricks + redstone → redstone bricks "Dh"/"X "
 * SMOOTH group    smooth + chisel → chiseled bricks   "y"/"X"
 *                 4 smooth → 4 bricks                 "XX"/"XX"
 *                 2 smooth → 2 tiles                  "X"/"X"
 *                 2 smooth → 2 small tiles            "XX"
 *                 2 smooth → 2 small bricks           "X "/" X"
 *                 2 smooth → 2 windmill tiles A       " X"/"X "
 * tile family     tiles ↔ square bricks, windmill A ↔ B (shapeless)
 * moss            clean + vine → mossy (cobble, bricks, cracked bricks; shapeless)
 * </pre>
 *
 * <p>Two GT6 details are carried over: the tool keys are GT tools of the type GT6's
 * {@code craftingToolHammer/Chisel/File} ore dictionary names refer to ({@code CR.java:344-360}), and
 * rows with the {@code CR.DEF} flag are registered <em>without</em> mirroring, which is why
 * {@link ToolShapedRecipe} is used instead of a plain {@link ShapedRecipe}.</p>
 *
 * <p>GT6's {@code CR.shaped(BRICK ×4, "XX"/"XX", plain stone)} is not repeated here: the data pack
 * already ships it as {@code stone_<rock>_bricks.json}.</p>
 */
public final class Loader_StoneCraftingRecipes {

    private static final List<String> REGISTERED = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    private Loader_StoneCraftingRecipes() {}

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    /** Rows GT6 has but a rock of the port cannot express, with the reason. */
    public static List<String> skipped() { return List.copyOf(SKIPPED); }

    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();
        SKIPPED.clear();
        int added = 0;
        for (StoneType type : StoneType.values()) added += rock(recipes, type);
        if (added > 0) com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} GT6 masonry crafting rows for {} rock types ({} skipped)",
                added, StoneType.values().length, SKIPPED.size());
        for (String reason : SKIPPED) GregTech.LOGGER.debug("[gregtech] masonry row skipped: {}", reason);
    }

    /** Every row of one rock type, exactly in GT6's order. */
    private static int rock(List<Recipe<?>> recipes, StoneType type) {
        String path = type.registryId();
        ItemStack rock = item(type, StoneVariant.STONE, MaterialPrefix.rockGt);
        ItemStack stone = block(type, StoneVariant.STONE);
        ItemStack cobble = block(type, StoneVariant.COBBLE);
        ItemStack cobbleMossy = block(type, StoneVariant.COBBLE_MOSSY);
        ItemStack bricks = block(type, StoneVariant.BRICKS);
        ItemStack cracked = block(type, StoneVariant.BRICKS_CRACKED);
        ItemStack bricksMossy = block(type, StoneVariant.BRICKS_MOSSY);
        ItemStack chiseled = block(type, StoneVariant.BRICKS_CHISELED);
        ItemStack smooth = block(type, StoneVariant.SMOOTH);
        ItemStack redstoneBricks = block(type, StoneVariant.BRICKS_REDSTONE);
        ItemStack tiles = block(type, StoneVariant.TILES);
        ItemStack smallTiles = block(type, StoneVariant.SMALL_TILES);
        ItemStack smallBricks = block(type, StoneVariant.SMALL_BRICKS);
        ItemStack windmillA = block(type, StoneVariant.WINDMILL_TILES_A);
        ItemStack windmillB = block(type, StoneVariant.WINDMILL_TILES_B);
        ItemStack squareBricks = block(type, StoneVariant.SQUARE_BRICKS);
        ItemStack cobbleSlab = slab(type, StoneVariant.COBBLE);
        if (rock.isEmpty() || stone.isEmpty() || cobble.isEmpty()) return 0;

        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack vine = new ItemStack(Items.VINE);
        ItemStack smallGear = GTItems.getStack(MaterialPrefix.gearGtSmall, type.material(), 1);
        Ingredient file = Ingredient.of(tool(GTToolType.FILE));
        Ingredient chisel = Ingredient.of(tool(GTToolType.CHISEL));
        Ingredient hammer = Ingredient.of(tool(GTToolType.HARD_HAMMER));

        int added = 0;
        added += shaped(recipes, path + "/rocks_to_cobble", new String[]{"XX", "XX"},
                Map.of('X', Ingredient.of(rock)), cobble, false);
        added += shaped(recipes, path + "/stone_to_small_gear", new String[]{"X ", " f"},
                Map.of('X', Ingredient.of(stone), 'f', file), smallGear, false);
        added += shaped(recipes, path + "/rocks_to_stairs", new String[]{" X", "XX"},
                Map.of('X', Ingredient.of(rock)), new ItemStack(Items.STONE_STAIRS), true);
        added += shaped(recipes, path + "/rocks_to_cobble_slab", new String[]{"  ", "XX"},
                Map.of('X', Ingredient.of(rock)), cobbleSlab, false);

        added += shaped(recipes, path + "/cobble_to_slabs", new String[]{"  ", "XX"},
                Map.of('X', Ingredient.of(cobble)), copy(cobbleSlab, 4), false);
        added += shaped(recipes, path + "/cobble_to_stairs", new String[]{" X", "XX"},
                Map.of('X', Ingredient.of(cobble)), new ItemStack(Items.COBBLESTONE_STAIRS, 4), true);
        added += shaped(recipes, path + "/cobble_to_wall", new String[]{"XXX", "XXX"},
                Map.of('X', Ingredient.of(cobble)), new ItemStack(Items.COBBLESTONE_WALL, 6), true);

        added += shaped(recipes, path + "/bricks_cracked_by_chisel", new String[]{"y", "X"},
                Map.of('X', Ingredient.of(bricks), 'y', chisel), cracked, false);
        added += shaped(recipes, path + "/bricks_cracked_by_hammer", new String[]{"h", "X"},
                Map.of('X', Ingredient.of(bricks), 'h', hammer), cracked, false);
        // GT6's 'e' key is OreDictToolNames.drill (CR.java:341); the port registers no drill tool, so
        // this row cannot be built.
        SKIPPED.add(path + "/bricks_reinforced: GT6's key 'e' is the drill tool, which the port does not register");
        added += shaped(recipes, path + "/bricks_redstone", new String[]{"Dh", "X "},
                Map.of('X', Ingredient.of(bricks), 'D', Ingredient.of(redstone),
                        'h', hammer), redstoneBricks, false);

        added += shaped(recipes, path + "/smooth_chiseled", new String[]{"y", "X"},
                Map.of('X', Ingredient.of(smooth), 'y', chisel), chiseled, false);
        added += shaped(recipes, path + "/smooth_to_bricks", new String[]{"XX", "XX"},
                Map.of('X', Ingredient.of(smooth)), copy(bricks, 4), false);
        added += shaped(recipes, path + "/smooth_to_tiles", new String[]{"X", "X"},
                Map.of('X', Ingredient.of(smooth)), copy(tiles, 2), false);
        added += shaped(recipes, path + "/smooth_to_small_tiles", new String[]{"XX"},
                Map.of('X', Ingredient.of(smooth)), copy(smallTiles, 2), false);
        added += shaped(recipes, path + "/smooth_to_small_bricks", new String[]{"X ", " X"},
                Map.of('X', Ingredient.of(smooth)), copy(smallBricks, 2), false);
        added += shaped(recipes, path + "/smooth_to_windmill_tiles", new String[]{" X", "X "},
                Map.of('X', Ingredient.of(smooth)), copy(windmillA, 2), false);

        added += shapeless(recipes, path + "/tiles_to_square_bricks", tiles, squareBricks);
        added += shapeless(recipes, path + "/square_bricks_to_tiles", squareBricks, tiles);
        added += shapeless(recipes, path + "/windmill_tiles_a_to_b", windmillA, windmillB);
        added += shapeless(recipes, path + "/windmill_tiles_b_to_a", windmillB, windmillA);

        added += shapeless(recipes, path + "/mossy_cobblestone_from_vine", vine, cobble, cobbleMossy);
        added += shapeless(recipes, path + "/mossy_bricks_from_vine", vine, bricks, bricksMossy);
        added += shapeless(recipes, path + "/mossy_cracked_bricks_from_vine", vine, cracked, bricksMossy);
        // GT6 RM.growmoss (RM.java:305-310) writes a second shapeless row per group using its own moss
        // item (OD.itemMoss), which the port does not register.
        for (String clean : new String[]{"cobblestone", "bricks", "cracked_bricks"}) {
            SKIPPED.add(path + "/mossy_" + clean + "_from_moss: GT6's moss item is not registered");
        }
        return added;
    }

    // ── recipe helpers ────────────────────────────────────────────────────

    private static int shaped(List<Recipe<?>> recipes, String path, String[] pattern,
                              Map<Character, Ingredient> key, ItemStack output, boolean allowMirror) {
        if (output.isEmpty()) {
            SKIPPED.add(path + ": the rock has no form for this output");
            return 0;
        }
        int width = pattern[0].length();
        int height = pattern.length;
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) {
            if (pattern[y].length() != width) return 0;
            for (int x = 0; x < width; x++) {
                char symbol = pattern[y].charAt(x);
                if (symbol == ' ') continue;
                Ingredient ingredient = key.get(symbol);
                if (ingredient == null || ingredient.isEmpty()) {
                    SKIPPED.add(path + ": the rock has no form for key '" + symbol + "'");
                    return 0;
                }
                ingredients.set(x + y * width, ingredient);
            }
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "stone/" + path);
        ShapedRecipe base = new ShapedRecipe(id, "gt.stone", CraftingBookCategory.BUILDING,
                width, height, ingredients, output.copy());
        recipes.add(new ToolShapedRecipe(base, allowMirror));
        REGISTERED.add(id.toString());
        return 1;
    }

    private static int shapeless(List<Recipe<?>> recipes, String path, ItemStack... stacks) {
        if (stacks.length < 2) return 0;
        ItemStack output = stacks[stacks.length - 1];
        if (output.isEmpty()) {
            SKIPPED.add(path + ": the rock has no form for this output");
            return 0;
        }
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < stacks.length - 1; i++) {
            if (stacks[i].isEmpty()) {
                SKIPPED.add(path + ": the rock has no form for input " + i);
                return 0;
            }
            ingredients.add(Ingredient.of(stacks[i]));
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "stone/" + path);
        recipes.add(new ShapelessRecipe(id, "gt.stone", CraftingBookCategory.BUILDING, output.copy(), ingredients));
        REGISTERED.add(id.toString());
        return 1;
    }

    private static ItemStack block(StoneType type, StoneVariant variant) {
        Block block = GTBlocks.getStone(type, variant);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static ItemStack slab(StoneType type, StoneVariant variant) {
        Block block = GTBlocks.getStoneSlab(type, variant);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static ItemStack item(StoneType type, StoneVariant ignored, MaterialPrefix prefix) {
        return GTItems.getStack(prefix, type.material(), 1);
    }

    private static ItemStack copy(ItemStack stack, int count) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    /** GT6's {@code craftingTool<Type>} key: the tool item, matched whatever its material and wear. */
    private static ItemStack tool(GTToolType type) {
        var item = GTToolItems.get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
