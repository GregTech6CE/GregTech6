package gregtech.content.recipe;

//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else {
import gregtech.GregTech;
//?}
import gregtech.content.data.MachineRecipeMaps;
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
    private static final java.util.Set<gregtech.engine.recipe.Recipe> MIRRORED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final java.util.Set<gregtech.engine.recipe.Recipe> DISPLACED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Undo only this loader's previous mirror, preserving independently registered machine recipes. */
    public static void clearMirrors() {
        gregtech.content.recipe.MachineRecipeOverlay.restoreFurnaceBeforeMirror();
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
        var originalRows=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<gregtech.engine.recipe.Recipe,Boolean>());
        originalRows.addAll(MachineRecipeMaps.Furnace.mRecipeList);

//? if >=1.21.1 {
        /*List<RecipeHolder<?>> rewritten = new ArrayList<>();
*///?} else {
        List<Recipe<?>> rewritten = new ArrayList<>();
//?}
        int mirrored = 0;
        int unsmelted = 0;
        int dropped = 0;

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
            // GT6 Loader_Recipes_Furnace:103-112 removes every cooking row whose input and output both
            // carry GT material data; the listener rows add the proper ones back below. Without this a
            // vanilla furnace turns ore blocks straight into ingots, the step GT6 deliberately removed.
            // Recipes this mod authors itself already encode the proper conversions (stone firing,
            // ceramic and mold firing), so only foreign rows are subject to the removal.
            if (cooking && !recipeId.getNamespace().equals("gt6ce")
                    && oreDicted(firstInput(recipe)) && oreDicted(result)
                    && !isRetainedVanillaSmelting(recipeId, recipe, result)) {
                dropped++;
                continue;
            }
            boolean vanillaRawIron = cooking && isRetainedVanillaSmelting(recipeId, recipe, result)
                    && firstInput(recipe).is(net.minecraft.world.item.Items.RAW_IRON);
            boolean neverFurnace = cooking && (MachineRecipeMaps.isNeverFurnace(result) || vanillaRawIron);

            if (neverFurnace) {
                ItemStack scrap = vanillaRawIron ? vanillaRawIronOutput() : MachineRecipeMaps.neverFurnaceOutput(result);
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
                // Vanilla glass has one ingredient containing both sand variants. Mirroring only
                // its first stack restores the furnace recipe but still strands red sand in the GT oven.
                var inputs = isRetainedVanillaSmelting(recipeId, recipe, result)
                        ? recipe.getIngredients().get(0).getItems() : new ItemStack[]{firstInput(recipe)};
                var seen = new java.util.HashSet<Item>();
                for (var candidate : inputs) {
                    ItemStack input = candidate.copyWithCount(1);
                    if (!input.isEmpty() && seen.add(input.getItem()) && !result.isEmpty()
                            && gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem()).isEmpty()
                            && MachineRecipeMaps.add_smelting(input, result)) mirrored++;
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
        gregtech.content.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, rewritten);
        GregTech.LOGGER.info("Converted {} vanilla smelting recipes to GT6 oven recipes; {} un-smelted"
//?}
                + " (NEVER_FURNACE materials yield scrap instead); {} material rows dropped; {} GT"
                + " furnace rows added to the vanilla furnace.", mirrored, unsmelted, dropped, fuelled);
        gregtech.content.recipe.MachineRecipeOverlay.reapplyFurnace(manager);
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
        /*ordered.sort(java.util.Comparator.comparing((gregtech.engine.recipe.Recipe row)->sortKey(firstStack(row.mInputs), access))
                .thenComparing(row->sortKey(firstStack(row.mOutputs), access)));
*///?} else {
        ordered.sort(java.util.Comparator.comparing((gregtech.engine.recipe.Recipe row)->firstStack(row.mInputs).save(new net.minecraft.nbt.CompoundTag()).toString())
                .thenComparing(row->firstStack(row.mOutputs).save(new net.minecraft.nbt.CompoundTag()).toString()));
//?}
        for (gregtech.engine.recipe.Recipe gt : ordered) {
            if (!gt.mEnabled || gt.mHidden) continue;
            ItemStack input = firstStack(gt.mInputs);
            ItemStack output = firstStack(gt.mOutputs);
            if (input.isEmpty() || output.isEmpty() || input.getCount() != 1) continue;
            if (!covered.add(input.getItem())) continue;   // the vanilla list already covers this input
            var cooking = gregtech.content.recipe.FurnaceSmeltingRecipes.cooking(input.getItem());
            if (cooking.isEmpty()) cooking = SourceFurnaceNativeAdapter.cooking(gt);
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
            if (input.is(net.minecraft.world.item.Items.COBBLESTONE) || input.is(net.minecraft.world.item.Items.STONE)) continue;
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
            if (metadata.isEmpty()) metadata = SourceFurnaceNativeAdapter.cooking(row);
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

//? if >=1.21.1 {
    /*private static String sortKey(ItemStack stack, RegistryAccess access) {
        return stack.isEmpty() ? "" : stack.save(access).toString();
    }
*///?}

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

    /** Vanilla ingots are not MaterialFormItem, so apply the same iron material policy explicitly. */
    private static ItemStack vanillaRawIronOutput() {
        var iron = gregtech.content.material.Materials.Iron;
        if (!iron.has(gregtech.api.material.MaterialProperty.NEVER_FURNACE))
            return new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
        return gregtech.content.registry.GTItems.getStack(gregtech.content.data.MaterialPrefix.scrapGt, iron,
                (int) (gregtech.api.material.GTValues.U / gregtech.content.data.MaterialPrefix.scrapGt.getMaterialWeight()));
    }

    /** Modern composition data also covers sand and clay, but no material listener replaces these rows. */
    private static boolean isRetainedVanillaSmelting(net.minecraft.resources.ResourceLocation id,
                                                     Recipe<?> recipe, ItemStack output) {
        if (!id.getNamespace().equals("minecraft")
                || (recipe.getType() != RecipeType.SMELTING && recipe.getType() != RecipeType.BLASTING)
                || recipe.getIngredients().size() != 1) return false;
        var inputs = recipe.getIngredients().get(0).getItems();
        if (inputs.length == 0) return false;
        // Re-applying the bridge must also retain its own exact iron-scrap rewrite.
        var ironScrap = vanillaRawIronOutput();
        if (!ironScrap.isEmpty() && output.getCount() == ironScrap.getCount()
//? if >=1.21.1 {
                /*&& ItemStack.isSameItemSameComponents(output, ironScrap)
*///?} else {
                && ItemStack.isSameItemSameTags(output, ironScrap)
//?}
                && java.util.Arrays.stream(inputs).allMatch(stack -> stack.is(net.minecraft.world.item.Items.RAW_IRON))) return true;
        if (output.getCount() != 1) return false;
        // Modern raw-metal items are not GT oreRaw aliases. Keep their ordinary cooking entry;
        // the existing NEVER_FURNACE output policy below still turns iron into scrap.
        var raw = output.is(net.minecraft.world.item.Items.COPPER_INGOT) ? net.minecraft.world.item.Items.RAW_COPPER
                : output.is(net.minecraft.world.item.Items.GOLD_INGOT) ? net.minecraft.world.item.Items.RAW_GOLD
                : output.is(net.minecraft.world.item.Items.IRON_INGOT) ? net.minecraft.world.item.Items.RAW_IRON : null;
        if (raw != null) return java.util.Arrays.stream(inputs).allMatch(stack -> stack.is(raw));
        // Ancient debris has no GT material furnace row (MELTING without FURNACE). Dropping the
        // vanilla debris -> scrap rows would strand both the scrap and the netherite ingot recipe.
        if (output.is(net.minecraft.world.item.Items.NETHERITE_SCRAP))
            return java.util.Arrays.stream(inputs)
                    .allMatch(stack -> stack.is(net.minecraft.world.item.Items.ANCIENT_DEBRIS));
        if (recipe.getType() != RecipeType.SMELTING) return false;
        if (output.is(net.minecraft.world.item.Items.STONE))
            return java.util.Arrays.stream(inputs).allMatch(stack -> stack.is(net.minecraft.world.item.Items.COBBLESTONE));
        if (output.is(net.minecraft.world.item.Items.SMOOTH_STONE))
            return java.util.Arrays.stream(inputs).allMatch(stack -> stack.is(net.minecraft.world.item.Items.STONE));
        if (output.is(net.minecraft.world.item.Items.BRICK))
            return java.util.Arrays.stream(inputs).allMatch(stack -> stack.is(net.minecraft.world.item.Items.CLAY_BALL));
        if (output.is(net.minecraft.world.item.Items.GLASS))
            return java.util.Arrays.stream(inputs).allMatch(stack -> stack.is(net.minecraft.world.item.Items.SAND)
                    || stack.is(net.minecraft.world.item.Items.RED_SAND));
        return false;
    }

    /** GT6 {@code OM.anydata}: GT material items plus every unified vanilla item. */
    private static boolean oreDicted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof gregtech.engine.material.MaterialFormItem) return true;
        return gregtech.engine.material.ItemMaterialRegistry.get(stack).isPresent();
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
