package com.gregtech.gregtech.loaders;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts vanilla furnace/blast furnace smelting recipes into GT6 oven recipes on server start, and
 * pushes the GT furnace rows back into the vanilla furnace.
 * <p>
 * GT6 {@code Loader_Recipes_Furnace:103-131} also rewrites the vanilla recipes themselves: any
 * recipe whose result carries an ore-dict material flagged {@code TD.Processing.NEVER_FURNACE}
 * (Iron, Wrought Iron, Steel, Titanium, Tungsten) is <em>un-smelted</em> 鈥?the result is replaced by
 * {@code scrapGt} of that material, because those metals have to be melted in a crucible instead.
 * Without that rewrite a vanilla furnace smelts iron ore straight into iron ingots, which is exactly
 * the step GT6 removed; this loader performs the same substitution through
 * {@link RecipeManager#replaceRecipes(Iterable)}.
 * </p>
 * <p>
 * The other direction is GT6 {@code RM.add_smelting} ({@code RM.java:805-821}): it writes into the
 * <em>vanilla</em> furnace recipe list, which is how dust, piles, gems, rocks and crushed ore melt
 * back into their metal. The port keeps those rows in {@link MachineRecipeMaps#Furnace} (the Oven's
 * table), so this loader mirrors them into the vanilla recipe manager as well 鈥?otherwise the
 * vanilla furnace would refuse the port's own ore forms. Rows are inserted with the vanilla cooking
 * time and replace any existing recipe for the same input, matching the 1.7.10 map semantics of
 * {@code FurnaceRecipes.func_151394_a}.
 * </p>
 */
public final class Loader_OvenRecipes {
    private Loader_OvenRecipes() {}
    private static final java.util.Set<com.gregtech.gregtech.api.recipe.Recipe> MIRRORED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final java.util.Set<com.gregtech.gregtech.api.recipe.Recipe> DISPLACED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Undo only this loader's previous mirror, preserving independently registered machine recipes. */
    public static void clearMirrors() {
        var map=MachineRecipeMaps.Furnace;
        map.mRecipeList.removeAll(MIRRORED);
        map.mRecipeItemMap.values().forEach(rows->rows.removeAll(MIRRORED));
        map.mRecipeFluidMap.values().forEach(rows->rows.removeAll(MIRRORED));
        for(var row:DISPLACED) if(!map.mRecipeList.contains(row)) map.add(row);
        MIRRORED.clear(); DISPLACED.clear(); ADDED_TO_VANILLA.clear();
    }


    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        clearMirrors();
        var originalRows=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<com.gregtech.gregtech.api.recipe.Recipe,Boolean>());
        originalRows.addAll(MachineRecipeMaps.Furnace.mRecipeList);

        List<RecipeHolder<?>> rewritten = new ArrayList<>();
        int mirrored = 0;
        int unsmelted = 0;

        for (RecipeHolder<?> holder : new ArrayList<>(manager.getRecipes())) {
            Recipe<?> recipe = holder.value();
            ItemStack result = resultOf(recipe, access);
            boolean cooking = recipe.getType() == RecipeType.SMELTING || recipe.getType() == RecipeType.BLASTING;
            boolean neverFurnace = cooking && MachineRecipeMaps.isNeverFurnace(result);

            if (neverFurnace) {
                ItemStack scrap = MachineRecipeMaps.neverFurnaceOutput(result);
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                    rewritten.add(new RecipeHolder<>(holder.id(), unSmelt(cookingRecipe, scrap)));
                    unsmelted++;
                }
                if (recipe.getType() == RecipeType.SMELTING) {
                    ItemStack input = firstInput(recipe);
                    if (!input.isEmpty() && MachineRecipeMaps.add_smelting(input, scrap)) mirrored++;
                }
                continue;
            }

            rewritten.add(holder);
            if (recipe.getType() == RecipeType.SMELTING) {
                ItemStack input = firstInput(recipe);
                if (!input.isEmpty() && !result.isEmpty() && MachineRecipeMaps.add_smelting(input, result)) {
                    mirrored++;
                }
            }
        }

        MIRRORED.addAll(MachineRecipeMaps.Furnace.mRecipeList); MIRRORED.removeAll(originalRows);
        DISPLACED.addAll(originalRows); DISPLACED.removeAll(MachineRecipeMaps.Furnace.mRecipeList);
        int fuelled = mirrorIntoFurnace(rewritten, access);
        var unique = new java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, RecipeHolder<?>>();
        for (var holder : rewritten) unique.putIfAbsent(holder.id(), holder);
        manager.replaceRecipes(unique.values());
        com.mojang.logging.LogUtils.getLogger().info("Converted {} vanilla smelting recipes to GT6 oven recipes; {} un-smelted"
                + " (NEVER_FURNACE materials yield scrap instead); {} GT furnace rows added to the"
                + " vanilla furnace.", mirrored, unsmelted, fuelled);
    }

    /**
     * Adds the GT furnace table's rows to the vanilla smelting list. GT6 {@code RM.add_smelting}
     * writes straight into {@code FurnaceRecipes}, a map keyed by input, so a GT row replaces a
     * vanilla one there; in this port the datapack recipes ({@code data/gregtech/recipes/**}) are the
     * hand-authored equivalents, so an input that already has a recipe keeps it and only the missing
     * rows are added.
     *
     * @return how many recipes were added
     */
    private static int mirrorIntoFurnace(List<RecipeHolder<?>> recipes, RegistryAccess access) {
        // The mirror runs again on every server start, i.e. once per world load, so this report has to
        // be replaced instead of appended to. Without the clear it kept one ItemStack per mirrored row
        // of every world this JVM had ever loaded (about 2314 stacks per world load, monotonically),
        // and addedToVanillaFurnace() handed out all of them. Sibling loaders clear their accumulators
        // the same way; see Loader_StoneCraftingRecipes/HandTool/Wood/Track.
        ADDED_TO_VANILLA.clear();
        java.util.Set<Item> covered = new java.util.HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> recipe = holder.value();
            if (recipe.getType() != RecipeType.SMELTING) continue;
            for (var ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) covered.add(stack.getItem());
            }
        }
        int added = 0;
        var ordered=new ArrayList<>(MachineRecipeMaps.Furnace.mRecipeList);
        ordered.sort(java.util.Comparator.comparing((com.gregtech.gregtech.api.recipe.Recipe row)->sortKey(firstStack(row.mInputs), access))
                .thenComparing(row->sortKey(firstStack(row.mOutputs), access)));
        for (com.gregtech.gregtech.api.recipe.Recipe gt : ordered) {
            if (!gt.mEnabled || gt.mHidden) continue;
            ItemStack input = firstStack(gt.mInputs);
            ItemStack output = firstStack(gt.mOutputs);
            if (input.isEmpty() || output.isEmpty()) continue;
            if (!covered.add(input.getItem())) continue;   // the vanilla list already covers this input
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",
                    "furnace/" + BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace()
                            + "/" + BuiltInRegistries.ITEM.getKey(input.getItem()).getPath());
            recipes.add(new RecipeHolder<>(id, new SmeltingRecipe(
                    "", CookingBookCategory.MISC, Ingredient.of(input), output.copy(), 0.0F,
                    200)));   // vanilla furnace cooking time, as in GT6's FurnaceRecipes registration
            ADDED_TO_VANILLA.add(input);
            added++;
        }
        mirrorClayMoldBlasting(recipes);
        return added;
    }

    /** Original MTE firing enables blast furnace as well as ordinary furnace (F,F,T). */
    private static void mirrorClayMoldBlasting(List<RecipeHolder<?>> recipes) {
        var covered=new java.util.HashSet<Item>();
        for(var holder:recipes) {
            var recipe=holder.value();
            if(recipe.getType()!=RecipeType.BLASTING)continue;
            for(var ingredient:recipe.getIngredients())for(var stack:ingredient.getItems())covered.add(stack.getItem());
        }
        int added=0;
        for(var raw:com.gregtech.gregtech.content.recipe.ClayMoldCatalog.RAW) {
            var input=com.gregtech.gregtech.content.recipe.ClayMoldRecipes.input(raw);
            if(!covered.add(input.getItem()))continue;
            var output=com.gregtech.gregtech.content.recipe.ClayMoldRecipes.output(raw);
            var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","clay_molds/blast_"+raw.id());
            recipes.add(new RecipeHolder<>(id,new net.minecraft.world.item.crafting.BlastingRecipe(
                    "",CookingBookCategory.MISC,Ingredient.of(input),output,0.0F,100)));
            added++;
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original clay mold blast firing: {} rows",added);
    }

    private static final List<ItemStack> ADDED_TO_VANILLA = new ArrayList<>();

    /** The inputs whose GT furnace row was mirrored into the vanilla furnace, for tests and reports. */
    public static List<ItemStack> addedToVanillaFurnace() {
        return java.util.Collections.unmodifiableList(ADDED_TO_VANILLA);
    }

    /**
     * Runs one mirroring pass over {@code recipes} and returns how many rows it added.
     *
     * <p>Production runs this once per server start from {@link #apply}; it is public as a
     * deterministic seam for the row-mirroring regression test, which drives two passes to check that
     * {@link #addedToVanillaFurnace()} describes one pass rather than accumulating across world
     * loads.</p>
     */
    public static int mirrorIntoFurnaceForTests(List<RecipeHolder<?>> recipes, RegistryAccess access) {
        return mirrorIntoFurnace(recipes, access);
    }

    private static String sortKey(ItemStack stack, RegistryAccess access) {
        return stack.isEmpty() ? "" : stack.save(access).toString();
    }

    private static ItemStack firstStack(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Replaces a cooking recipe's result, keeping id, ingredient, experience and time. */
    private static Recipe<?> unSmelt(AbstractCookingRecipe recipe, ItemStack result) {
        Ingredient ingredient = recipe.getIngredients().isEmpty()
                ? Ingredient.EMPTY : recipe.getIngredients().get(0);
        if (recipe instanceof BlastingRecipe) {
            return new BlastingRecipe(recipe.getGroup(), recipe.category(), ingredient,
                    result, recipe.getExperience(), recipe.getCookingTime());
        }
        return new SmeltingRecipe(recipe.getGroup(), recipe.category(), ingredient,
                result, recipe.getExperience(), recipe.getCookingTime());
    }

    private static ItemStack resultOf(Recipe<?> recipe, RegistryAccess access) {
        try {
            return recipe.getResultItem(access);
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack firstInput(Recipe<?> recipe) {
        var ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) return ItemStack.EMPTY;
        var items = ingredients.get(0).getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        ItemStack stack = items[0].copy();
        stack.setCount(1);
        return stack;
    }
}
