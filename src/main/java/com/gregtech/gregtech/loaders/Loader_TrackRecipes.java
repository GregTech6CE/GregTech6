package com.gregtech.gregtech.loaders;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTrackBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.core.NonNullList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GT6's track recipes ({@code Loader_Rails:108-156}, the branch without Railcraft, which this port
 * does not have):
 *
 * <ul>
 *   <li>plain track — {@code "RSR","RSR","RSR"} with the material's {@code railGt} item and a
 *       treated-wood stick, four tracks;</li>
 *   <li>booster track — {@code "RSR","GDG","RSR"} plus redstone and the booster material GT6 lists
 *       per rail ({@code Loader_Rails:119-128}), four tracks;</li>
 *   <li>detector track — {@code "RSR","RPR","RDR"} plus redstone and a stone pressure plate, four
 *       tracks;</li>
 *   <li>the four <b>vanilla</b> rail recipes ({@code Loader_Rails:141-156}), which GT6 does not add
 *       next to the vanilla ones but <em>replaces</em> ({@code DEL_OTHER_SHAPED_RECIPES}): they take
 *       GT6's {@code railGt} items instead of iron ingots, and the activator rail's yield climbs with
 *       the material (aluminium 1 … adamantium 64). Only conflicting shaped crafting rows are
 *       removed; shapeless crafting and machine recipes remain available.</li>
 * </ul>
 */
public final class Loader_TrackRecipes {

    /** The vanilla rails GT6 replaces ({@code Blocks.rail/golden_rail/detector_rail/activator_rail}). */
    private static final List<Item> REPLACED = List.of(Items.RAIL, Items.POWERED_RAIL,
            Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL);

    /** {@code Loader_Rails:145-156}: the activator rail's material and GT6's yield for it. */
    private static final List<String> REGISTERED = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    private Loader_TrackRecipes() {}

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    /** GT6 rows the port cannot express, with the reason. */
    public static List<String> skipped() { return List.copyOf(SKIPPED); }

    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();
        SKIPPED.clear();
        int added = 0;
        for (GTTrackBlocks.Track track : GTTrackBlocks.all()) {
            added += switch (track.family()) {
                case STRAIGHT -> plain(recipes, track);
                case BOOSTER -> booster(recipes, track);
                case DETECTOR -> detector(recipes, track);
            };
        }
        added += vanilla(recipes, access);
        if (added > 0) com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} GT6 track crafting rows", added);
        return;
    }

    /** GT6's {@code DEL_OTHER_SHAPED_RECIPES} applies to shaped crafting rows only. */
    public static boolean replacesVanillaRailRecipe(Recipe<?> recipe, net.minecraft.core.RegistryAccess access) {
        if (recipe.getType() != RecipeType.CRAFTING || !(recipe instanceof ShapedRecipe)) return false;
        // GT6 does not remove its own protected crafting rows.
        if (recipe.getId().getNamespace().equals(GregTech.NAMESPACE)) return false;
        ItemStack result = recipe.getResultItem(access);
        return !result.isEmpty() && REPLACED.contains(result.getItem());
    }

    /** GT6's replacement of the four vanilla rail recipes ({@code Loader_Rails:141-156}). */
    private static int vanilla(List<Recipe<?>> recipes, net.minecraft.core.RegistryAccess access) {
        recipes.removeIf(recipe -> replacesVanillaRailRecipe(recipe, access));
        int added = 0;
        // minecraft:rail — 4 from three iron tracks and three treated sticks.
        added += add(recipes, "tracks/vanilla/rail", new ItemStack(Items.RAIL, 4), com.gregtech.gregtech.content.transport.TrackRecipeCatalog.PLAIN,
                new char[]{'R', 'S'}, new ItemStack[]{rail("Iron"), stick()});
        // minecraft:golden_rail — 4, with gold tracks as the "G" ingredient.
        added += add(recipes, "tracks/vanilla/golden_rail", new ItemStack(Items.POWERED_RAIL, 4),
                com.gregtech.gregtech.content.transport.TrackRecipeCatalog.BOOSTER, new char[]{'R', 'S', 'G', 'D'},
                new ItemStack[]{rail("Iron"), stick(), rail("Gold"), new ItemStack(Items.REDSTONE)});
        // minecraft:detector_rail — 4, with a stone pressure plate.
        added += add(recipes, "tracks/vanilla/detector_rail", new ItemStack(Items.DETECTOR_RAIL, 4),
                com.gregtech.gregtech.content.transport.TrackRecipeCatalog.DETECTOR, new char[]{'R', 'S', 'P', 'D'},
                new ItemStack[]{rail("Iron"), stick(),
                        new ItemStack(Items.STONE_PRESSURE_PLATE), new ItemStack(Items.REDSTONE)});
        // minecraft:activator_rail — one row per material, the yield climbing to 64 adamantium tracks.
        for (var row : com.gregtech.gregtech.content.transport.TrackRecipeCatalog.ACTIVATORS) {
            added += add(recipes, "tracks/vanilla/activator_rail_" + row.material().toLowerCase(Locale.ROOT),
                    new ItemStack(Items.ACTIVATOR_RAIL, row.count()), com.gregtech.gregtech.content.transport.TrackRecipeCatalog.ACTIVATOR,
                    new char[]{'R', 'S', 'T'},
                    new ItemStack[]{rail(row.material()), stick(), new ItemStack(Items.REDSTONE_TORCH)});
        }
        return added;
    }

    /** {@code "RSR","RSR","RSR"} — four tracks from three rail items and three treated sticks. */
    private static int plain(List<Recipe<?>> recipes, GTTrackBlocks.Track track) {
        return add(recipes, track, com.gregtech.gregtech.content.transport.TrackRecipeCatalog.PLAIN, new char[]{'R', 'S'},
                new ItemStack[]{rail(track.material()), stick()}, 4);
    }

    /** {@code "RSR","GDG","RSR"} — four booster tracks, with GT6's per-rail "G" material. */
    private static int booster(List<Recipe<?>> recipes, GTTrackBlocks.Track track) {
        String g = GTTrackBlocks.boosterMaterial(track.material());
        return add(recipes, track, com.gregtech.gregtech.content.transport.TrackRecipeCatalog.BOOSTER, new char[]{'R', 'S', 'G', 'D'},
                new ItemStack[]{rail(track.material()), stick(), rail(g), new ItemStack(Items.REDSTONE)}, 4);
    }

    /** {@code "RSR","RPR","RDR"} — four detector tracks, with redstone and a stone pressure plate. */
    private static int detector(List<Recipe<?>> recipes, GTTrackBlocks.Track track) {
        return add(recipes, track, com.gregtech.gregtech.content.transport.TrackRecipeCatalog.DETECTOR, new char[]{'R', 'S', 'P', 'D'},
                new ItemStack[]{rail(track.material()), stick(),
                        new ItemStack(Items.STONE_PRESSURE_PLATE), new ItemStack(Items.REDSTONE)}, 4);
    }

    /** Builds GT6's 3×3 pattern for one of the port's tracks. */
    private static int add(List<Recipe<?>> recipes, GTTrackBlocks.Track track, String pattern,
                           char[] symbols, ItemStack[] ingredients, int count) {
        return add(recipes, "tracks/" + track.id(), new ItemStack(track.block().get(), count),
                pattern, symbols, ingredients);
    }

    /** Builds a 3×3 pattern; rows are separated by {@code |} and the key order matches. */
    private static int add(List<Recipe<?>> recipes, String path, ItemStack output, String pattern,
                           char[] symbols, ItemStack[] ingredients) {
        String id = path;
        for (ItemStack ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                SKIPPED.add(id + ": the port registers no such ingredient");
                return 0;
            }
        }
        String[] rows = pattern.split("\\|");
        int width = rows[0].length();
        NonNullList<Ingredient> grid = NonNullList.withSize(width * rows.length, Ingredient.EMPTY);
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != width) {
                SKIPPED.add(id + ": malformed pattern");
                return 0;
            }
            for (int x = 0; x < width; x++) {
                char symbol = rows[y].charAt(x);
                int index = indexOf(symbols, symbol);
                if (index < 0) {
                    SKIPPED.add(id + ": unknown key '" + symbol + "'");
                    return 0;
                }
                grid.set(x + y * width, Ingredient.of(ingredients[index]));
            }
        }
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, id);
        recipes.add(new ShapedRecipe(location, "gt.tracks", CraftingBookCategory.MISC,
                width, rows.length, grid, output));
        REGISTERED.add(location.toString());
        return 1;
    }

    private static int indexOf(char[] symbols, char symbol) {
        for (int i = 0; i < symbols.length; i++) if (symbols[i] == symbol) return i;
        return -1;
    }

    /** GT6's {@code OP.railGt.dat(material)}. */
    private static ItemStack rail(String material) {
        if (material == null) return ItemStack.EMPTY;
        GTMaterial resolved = GTMaterialRegistry.get(material);
        if (resolved == null || !resolved.isValid()) return ItemStack.EMPTY;
        return GTItems.getStack(MaterialPrefix.railGt, resolved.resolve(), 1);
    }

    /** GT6's {@code OP.stick.dat(ANY.WoodTreated)}. */
    private static ItemStack stick() {
        GTMaterial treated = GTMaterialRegistry.get("WoodTreated");
        if (treated == null || !treated.isValid()) return ItemStack.EMPTY;
        return GTItems.getStack(MaterialPrefix.stick, treated.resolve(), 1);
    }
}
