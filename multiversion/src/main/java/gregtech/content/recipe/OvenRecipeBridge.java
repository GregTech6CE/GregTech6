package gregtech.content.recipe;

//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else {
import gregtech.GregTech;
//?}
import gregtech.data.MachineRecipeMaps;
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
public final class OvenRecipeBridge {
    private OvenRecipeBridge() {}
    private static final java.util.Set<gregtech.internal.recipe.Recipe> MIRRORED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final java.util.Set<gregtech.internal.recipe.Recipe> DISPLACED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Undo only this loader's previous mirror, preserving independently registered machine recipes. */
    public static void clearMirrors() {
        gregtech.recipe.MachineRecipeOverlay.restoreFurnaceBeforeMirror();
        var map=MachineRecipeMaps.Furnace;
        map.mRecipeList.removeAll(MIRRORED);
        map.mRecipeItemMap.values().forEach(rows->rows.removeAll(MIRRORED));
        map.mRecipeFluidMap.values().forEach(rows->rows.removeAll(MIRRORED));
        for(var row:DISPLACED) if(!map.mRecipeList.contains(row)) map.add(row);
//? if >=1.21.1 {
        /*MIRRORED.clear(); DISPLACED.clear(); ADDED_TO_VANILLA.clear();
*///?} else {
        MIRRORED.clear(); DISPLACED.clear();
//?}
    }


    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        clearMirrors();
        var originalRows=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<gregtech.internal.recipe.Recipe,Boolean>());
        originalRows.addAll(MachineRecipeMaps.Furnace.mRecipeList);

//? if >=1.21.1 {
        /*List<RecipeHolder<?>> rewritten = new ArrayList<>();
*///?} else {
        List<Recipe<?>> rewritten = new ArrayList<>();
//?}
        int mirrored = 0;
        int unsmelted = 0;

//? if >=1.21.1 {
        /*for (RecipeHolder<?> holder : new ArrayList<>(manager.getRecipes())) {
            Recipe<?> recipe = holder.value();
*///?} else {
        for (Recipe<?> recipe : new ArrayList<>(manager.getRecipes())) {
//?}
            // GT6 imprint listeners only create a row when its optional form actually exists.
            // NeoForge represents unresolved tags as a barrier stack; length alone is insufficient.
//? if >=1.21.1 {
            /*var recipeId = holder.id();
*///?} else {
            var recipeId = recipe.getId();
//?}
            if (recipeId.getNamespace().equals("gt6ce") && recipeId.getPath().startsWith("clay_molds/imprint_")
                    && recipe.getIngredients().stream().anyMatch(input -> input != Ingredient.EMPTY
                    && java.util.Arrays.stream(input.getItems()).noneMatch(stack -> !stack.isEmpty()
                    && !stack.is(net.minecraft.world.item.Items.BARRIER)))) continue;
            ItemStack result = resultOf(recipe, access);
            boolean cooking = recipe.getType() == RecipeType.SMELTING || recipe.getType() == RecipeType.BLASTING;
            boolean neverFurnace = cooking && MachineRecipeMaps.isNeverFurnace(result);

            if (neverFurnace) {
                ItemStack scrap = MachineRecipeMaps.neverFurnaceOutput(result);
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
//? if >=1.21.1 {
                    /*rewritten.add(new RecipeHolder<>(holder.id(), unSmelt(cookingRecipe, scrap)));
*///?} else {
                    rewritten.add(unSmelt(cookingRecipe, scrap));
//?}
                    unsmelted++;
                }
                if (recipe.getType() == RecipeType.SMELTING) {
                    ItemStack input = firstInput(recipe);
                    if (!input.isEmpty() && gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem()).isEmpty() && MachineRecipeMaps.add_smelting(input, scrap)) mirrored++;
                }
                continue;
            }

//? if >=1.21.1 {
            /*rewritten.add(holder);
*///?} else {
            rewritten.add(recipe);
//?}
            if (recipe.getType() == RecipeType.SMELTING) {
                ItemStack input = firstInput(recipe);
                if (!input.isEmpty() && !result.isEmpty() && gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem()).isEmpty() && MachineRecipeMaps.add_smelting(input, result)) {
                    mirrored++;
                }
            }
        }

        MIRRORED.addAll(MachineRecipeMaps.Furnace.mRecipeList); MIRRORED.removeAll(originalRows);
        DISPLACED.addAll(originalRows); DISPLACED.removeAll(MachineRecipeMaps.Furnace.mRecipeList);
//? if >=1.21.1 {
        /*int fuelled = mirrorIntoFurnace(rewritten, access);
        var unique = new java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, RecipeHolder<?>>();
        for (var holder : rewritten) unique.putIfAbsent(holder.id(), holder);
        manager.replaceRecipes(unique.values());
        com.mojang.logging.LogUtils.getLogger().info("Converted {} vanilla smelting recipes to GT6 oven recipes; {} un-smelted"
*///?} else {
        int fuelled = mirrorIntoFurnace(rewritten);
        gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, rewritten);
        GregTech.LOGGER.info("Converted {} vanilla smelting recipes to GT6 oven recipes; {} un-smelted"
//?}
                + " (NEVER_FURNACE materials yield scrap instead); {} GT furnace rows added to the"
                + " vanilla furnace.", mirrored, unsmelted, fuelled);
        gregtech.recipe.MachineRecipeOverlay.reapplyFurnace(manager);
    }

    /**
     * Adds the GT furnace table's rows to the vanilla smelting list. GT6 {@code RM.add_smelting}
     * writes straight into {@code FurnaceRecipes}, a map keyed by input, so a GT row replaces a
     * vanilla one there. Source material listeners keep that priority, quantity, experience and
     * smoker/blast selection; other GT rows keep existing datapack choices.
     *
     * @return how many recipes were added
     */
//? if >=1.21.1 {
    /*private static int mirrorIntoFurnace(List<RecipeHolder<?>> recipes, RegistryAccess access) {
*///?} else {
    private static int mirrorIntoFurnace(List<Recipe<?>> recipes) {
//?}
        preferMaterialRows(recipes);
        // The mirror runs again on every server start, i.e. once per world load, so this report has to
        // be replaced instead of appended to. Without the clear it kept one ItemStack per mirrored row
        // of every world this JVM had ever loaded (about 2314 stacks per world load, monotonically),
        // and addedToVanillaFurnace() handed out all of them. Sibling loaders clear their accumulators
        // the same way; see Loader_StoneCraftingRecipes/HandTool/Wood/Track.
        ADDED_TO_VANILLA.clear();
        java.util.Set<Item> covered = new java.util.HashSet<>();
//? if >=1.21.1 {
        /*for (RecipeHolder<?> holder : recipes) {
            Recipe<?> recipe = holder.value();
*///?} else {
        for (Recipe<?> recipe : recipes) {
//?}
            if (recipe.getType() != RecipeType.SMELTING) continue;
            for (var ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) covered.add(stack.getItem());
            }
        }
        int added = 0;
        var ordered=new ArrayList<>(MachineRecipeMaps.Furnace.mRecipeList);
//? if >=1.21.1 {
        /*ordered.sort(java.util.Comparator.comparing((gregtech.internal.recipe.Recipe row)->sortKey(firstStack(row.mInputs), access))
                .thenComparing(row->sortKey(firstStack(row.mOutputs), access)));
*///?} else {
        ordered.sort(java.util.Comparator.comparing((gregtech.internal.recipe.Recipe row)->firstStack(row.mInputs).save(new net.minecraft.nbt.CompoundTag()).toString())
                .thenComparing(row->firstStack(row.mOutputs).save(new net.minecraft.nbt.CompoundTag()).toString()));
//?}
        for (gregtech.internal.recipe.Recipe gt : ordered) {
            if (!gt.mEnabled || gt.mHidden) continue;
            ItemStack input = firstStack(gt.mInputs);
            ItemStack output = firstStack(gt.mOutputs);
            if (input.isEmpty() || output.isEmpty() || input.getCount() != 1) continue;
            if (!covered.add(input.getItem())) continue;   // the vanilla list already covers this input
            var cooking = gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem());
            float experience = cooking.map(data -> data.experience()).orElse(0.0F);
            var category = cooking.map(data -> data.food()).orElse(false) ? CookingBookCategory.FOOD : CookingBookCategory.MISC;
//? if >=1.21.1 {
            /*var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",
                    "furnace/" + BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace()
                            + "/" + BuiltInRegistries.ITEM.getKey(input.getItem()).getPath());
            recipes.add(new RecipeHolder<>(id, new SmeltingRecipe(
*///?} else {
            recipes.add(new SmeltingRecipe(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE,
                            "furnace/" + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(input.getItem()).getNamespace()
                                    + "/" + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(input.getItem()).getPath()),
//?}
                    "", category, Ingredient.of(input), output.copy(), experience,
//? if >=1.21.1 {
                    /*200)));   // vanilla furnace cooking time, as in GT6's FurnaceRecipes registration
*///?} else {
                    200));   // vanilla furnace cooking time, as in GT6's FurnaceRecipes registration
//?}
            ADDED_TO_VANILLA.add(input);
            added++;
        }
        mirrorClayMoldBlasting(recipes);
        mirrorMaterialCooking(recipes);
        return added;
    }

    /** Original MTE firing enables blast furnace as well as ordinary furnace (F,F,T). */
//? if >=1.21.1 {
    /*private static void mirrorClayMoldBlasting(List<RecipeHolder<?>> recipes) {
*///?} else {
    private static void mirrorClayMoldBlasting(List<Recipe<?>> recipes) {
//?}
        var covered=new java.util.HashSet<Item>();
        for(var holder:recipes) {
//? if >=1.21.1 {
            /*var recipe=holder.value();
*///?} else {
            var recipe=holder;
//?}
            if(recipe.getType()!=RecipeType.BLASTING)continue;
            for(var ingredient:recipe.getIngredients())for(var stack:ingredient.getItems())covered.add(stack.getItem());
        }
        int added=0;
        for(var raw:gregtech.content.recipe.ClayMoldCatalog.RAW) {
            var input=gregtech.content.recipe.ClayMoldRecipes.input(raw);
            if(!covered.add(input.getItem()))continue;
            var output=gregtech.content.recipe.ClayMoldRecipes.output(raw);
            var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","clay_molds/blast_"+raw.id());
//? if >=1.21.1 {
            /*recipes.add(new RecipeHolder<>(id,new net.minecraft.world.item.crafting.BlastingRecipe(
                    "",CookingBookCategory.MISC,Ingredient.of(input),output,0.0F,100)));
*///?} else {
            recipes.add(new net.minecraft.world.item.crafting.BlastingRecipe(id,
                    "",CookingBookCategory.MISC,Ingredient.of(input),output,0.0F,100));
//?}
            added++;
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original clay mold blast firing: {} rows",added);
    }

    /** RM.add_smelting overwrites the same source input; split mixed ingredients so unrelated choices survive. */
//? if >=1.21.1 {
    /*private static void preferMaterialRows(List<RecipeHolder<?>> recipes) {
*///?} else {
    private static void preferMaterialRows(List<Recipe<?>> recipes) {
//?}
        var source = new java.util.HashSet<Item>();
        for (var row : MachineRecipeMaps.Furnace.mRecipeList) {
            if (!row.mEnabled || row.mHidden) continue;
            var input = firstStack(row.mInputs);
            if (!input.isEmpty() && input.getCount() == 1
                    && gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem()).isPresent()) source.add(input.getItem());
        }
//? if >=1.21.1 {
        /*List<RecipeHolder<?>> filtered = new ArrayList<>();
*///?} else {
        List<Recipe<?>> filtered = new ArrayList<>();
//?}
        for (var holder : recipes) {
//? if >=1.21.1 {
            /*var recipe = holder.value();
*///?} else {
            var recipe = holder;
//?}
            if (!(recipe instanceof AbstractCookingRecipe row) ||
                    (recipe.getType() != RecipeType.SMELTING && recipe.getType() != RecipeType.SMOKING && recipe.getType() != RecipeType.BLASTING)) {
                filtered.add(holder); continue;
            }
            var choices = row.getIngredients().isEmpty() ? new ItemStack[0] : row.getIngredients().get(0).getItems();
            var remaining = java.util.Arrays.stream(choices).filter(stack -> !source.contains(stack.getItem())).toArray(ItemStack[]::new);
            if (remaining.length == choices.length) { filtered.add(holder); continue; }
            if (remaining.length == 0) continue;
//? if >=1.21.1 {
            /*var id = holder.id();
*///?} else {
            var id = row.getId();
//?}
            var excluded = java.util.Arrays.stream(choices).filter(stack -> source.contains(stack.getItem())).toArray(ItemStack[]::new);
//? if >=1.21.1 {
            /*var ingredient = net.neoforged.neoforge.common.crafting.DifferenceIngredient.of(
*///?} else {
            var ingredient = net.minecraftforge.common.crafting.DifferenceIngredient.of(
//?}
                    row.getIngredients().get(0), Ingredient.of(excluded));
            var result = row.getResultItem(RegistryAccess.EMPTY).copy();
            Recipe<?> replacement = recipe.getType() == RecipeType.SMOKING
//? if >=1.21.1 {
/*// Version-specific branch.
                    ? new net.minecraft.world.item.crafting.SmokingRecipe(row.getGroup(), row.category(), ingredient, result, row.getExperience(), row.getCookingTime())
*///?} else {
// Version-specific branch.
                    ? new net.minecraft.world.item.crafting.SmokingRecipe(id, row.getGroup(), row.category(), ingredient, result, row.getExperience(), row.getCookingTime())
//?}
                    : recipe.getType() == RecipeType.BLASTING
//? if >=1.21.1 {
/*// Version-specific branch.
                    ? new BlastingRecipe(row.getGroup(), row.category(), ingredient, result, row.getExperience(), row.getCookingTime())
                    : new SmeltingRecipe(row.getGroup(), row.category(), ingredient, result, row.getExperience(), row.getCookingTime());
            filtered.add(new RecipeHolder<>(id, replacement));
*///?} else {
// Version-specific branch.
                    ? new BlastingRecipe(id, row.getGroup(), row.category(), ingredient, result, row.getExperience(), row.getCookingTime())
                    : new SmeltingRecipe(id, row.getGroup(), row.category(), ingredient, result, row.getExperience(), row.getCookingTime());
            filtered.add(replacement);
//?}
        }
        recipes.clear(); recipes.addAll(filtered);
    }

    /** Source Listener_Furnace_Smelting sends food to smokers and other source materials to blast furnaces. */
//? if >=1.21.1 {
    /*private static void mirrorMaterialCooking(List<RecipeHolder<?>> recipes) {
*///?} else {
    private static void mirrorMaterialCooking(List<Recipe<?>> recipes) {
//?}
        var smoked = new java.util.HashSet<Item>();
        var blasted = new java.util.HashSet<Item>();
        for (var holder : recipes) {
//? if >=1.21.1 {
            /*var recipe = holder.value();
*///?} else {
            var recipe = holder;
//?}
            if (recipe.getType() != RecipeType.SMOKING && recipe.getType() != RecipeType.BLASTING) continue;
            var covered = recipe.getType() == RecipeType.SMOKING ? smoked : blasted;
            for (var ingredient : recipe.getIngredients()) for (var stack : ingredient.getItems()) covered.add(stack.getItem());
        }
        int added = 0;
        for (var row : MachineRecipeMaps.Furnace.mRecipeList) {
            if (!row.mEnabled || row.mHidden) continue;
            var input = firstStack(row.mInputs); var output = firstStack(row.mOutputs);
            if (input.isEmpty() || output.isEmpty() || input.getCount() != 1) continue;
            var metadata = gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem());
            if (metadata.isEmpty()) continue;
            var data = metadata.get(); var covered = data.food() ? smoked : blasted;
            if (!covered.add(input.getItem())) continue;
//? if >=1.21.1 {
            /*var itemId = BuiltInRegistries.ITEM.getKey(input.getItem());
*///?} else {
            var itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(input.getItem());
//?}
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",
                    "material_cooking/" + (data.food() ? "smoking/" : "blasting/") + itemId.getNamespace() + "/" + itemId.getPath());
            Recipe<?> recipe = data.food()
//? if >=1.21.1 {
/*// Version-specific branch.
                    ? new net.minecraft.world.item.crafting.SmokingRecipe("", CookingBookCategory.FOOD, Ingredient.of(input), output.copy(), data.experience(), 100)
                    : new BlastingRecipe("", CookingBookCategory.MISC, Ingredient.of(input), output.copy(), data.experience(), 100);
            recipes.add(new RecipeHolder<>(id, recipe));
*///?} else {
// Version-specific branch.
                    ? new net.minecraft.world.item.crafting.SmokingRecipe(id, "", CookingBookCategory.FOOD, Ingredient.of(input), output.copy(), data.experience(), 100)
                    : new BlastingRecipe(id, "", CookingBookCategory.MISC, Ingredient.of(input), output.copy(), data.experience(), 100);
            recipes.add(recipe);
//?}
            added++;
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original material smoker/blast rows: {}", added);
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
//? if >=1.21.1 {
    /*public static int mirrorIntoFurnaceForTests(List<RecipeHolder<?>> recipes, RegistryAccess access) {
        return mirrorIntoFurnace(recipes, access);
    }

    private static String sortKey(ItemStack stack, RegistryAccess access) {
        return stack.isEmpty() ? "" : stack.save(access).toString();
*///?} else {
    public static int mirrorIntoFurnaceForTests(List<Recipe<?>> recipes) {
        return mirrorIntoFurnace(recipes);
//?}
    }

    private static ItemStack firstStack(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /** Replaces a cooking recipe's result, keeping id, ingredient, experience and time. */
    private static Recipe<?> unSmelt(AbstractCookingRecipe recipe, ItemStack result) {
        Ingredient ingredient = recipe.getIngredients().isEmpty()
                ? Ingredient.EMPTY : recipe.getIngredients().get(0);
        if (recipe instanceof BlastingRecipe) {
//? if >=1.21.1 {
            /*return new BlastingRecipe(recipe.getGroup(), recipe.category(), ingredient,
*///?} else {
            return new BlastingRecipe(recipe.getId(), recipe.getGroup(), recipe.category(), ingredient,
//?}
                    result, recipe.getExperience(), recipe.getCookingTime());
        }
//? if >=1.21.1 {
        /*return new SmeltingRecipe(recipe.getGroup(), recipe.category(), ingredient,
*///?} else {
        return new SmeltingRecipe(recipe.getId(), recipe.getGroup(), recipe.category(), ingredient,
//?}
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
