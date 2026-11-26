package gregtech.content.recipe;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=1.21.1 {
/*import gregtech.content.data.OriginalCraftingJson;
*///?} else {
import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
//?}

/**
 * Shared output sink for generated crafting recipes.
 *
 * <p>On 1.20.1 this buffers concrete {@link Recipe} instances and publishes them through {@link
 * RuntimeRecipeLifecycle#replaceGenerated}; on 1.21.1 it emits JSON entries directly through {@code
 * OriginalCraftingJson}.</p>
 */
public final class GeneratedCraftingSink {
//? if >=1.21.1 {
    /*private final Map<ResourceLocation, byte[]> data;
    private final int initialSize;

    private GeneratedCraftingSink(Map<ResourceLocation, byte[]> data) {
        this.data = data;
        this.initialSize = data.size();
    }

    public static GeneratedCraftingSink datapack(Map<ResourceLocation, byte[]> data) {
        return new GeneratedCraftingSink(data);
    }

    public void shaped(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            String[] pattern,
            Map<Character, Ingredient> key,
            boolean allowMirror) {
        OriginalCraftingJson.shaped(data, id, group, category, output, pattern, key, allowMirror);
    }

    public void shapeless(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            List<Ingredient> ingredients) {
        OriginalCraftingJson.shapeless(data, id, group, category, output, ingredients);
    }

    public void toolShapeless(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            List<Ingredient> ingredients,
            boolean autocraftable,
            FormConversionSelector selector) {
        OriginalCraftingJson.shapeless(data, id, group, category, output, ingredients, autocraftable);
    }

    public void orderedFormConversion(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            List<Ingredient> ingredients,
            FormConversionSelector selector,
            String inputPrefix,
            String materialName) {
        OriginalCraftingJson.orderedFormConversion(
                data, id, group, category, output, ingredients, selector, inputPrefix, materialName);
    }

    public int writtenCount() {
        return data.size() - initialSize;
    }

    public boolean isDatapack() {
        return true;
    }

    public void commit() {}
*///?} else {
    private final RecipeManager manager;
    private final List<Recipe<?>> recipes;
    private final int initialSize;

    private GeneratedCraftingSink(RecipeManager manager, List<Recipe<?>> recipes) {
        this.manager = manager;
        this.recipes = recipes;
        this.initialSize = recipes.size();
    }

    public static GeneratedCraftingSink runtime(RecipeManager manager) {
        return new GeneratedCraftingSink(manager, new ArrayList<>(manager.getRecipes()));
    }

    public static GeneratedCraftingSink runtime(RecipeManager manager, List<Recipe<?>> recipes) {
        return new GeneratedCraftingSink(manager, recipes);
    }

    public void shaped(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            String[] pattern,
            Map<Character, Ingredient> key,
            boolean allowMirror) {
        int width = pattern[0].length();
        int height = pattern.length;
        NonNullList<Ingredient> grid = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char symbol = pattern[y].charAt(x);
                if (symbol == ' ') continue;
                grid.set(x + y * width, key.get(symbol));
            }
        }
        ShapedRecipe base = new ShapedRecipe(id, group, category, width, height, grid, output);
        recipes.add(new ToolShapedRecipe(base, allowMirror));
    }

    public void shapeless(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            List<Ingredient> ingredients) {
        recipes.add(new ShapelessRecipe(id, group, category, output, toNonNullList(ingredients)));
    }

    public void toolShapeless(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            List<Ingredient> ingredients,
            boolean autocraftable,
            FormConversionSelector selector) {
        recipes.add(new ToolShapelessRecipe(
                new ShapelessRecipe(id, group, category, output, toNonNullList(ingredients)),
                autocraftable,
                selector,
                "",
                "",
                false,
                FormConversionPlacement.FIXED));
    }

    public void orderedFormConversion(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack output,
            List<Ingredient> ingredients,
            FormConversionSelector selector,
            String inputPrefix,
            String materialName) {
        recipes.add(new ToolShapelessRecipe(
                new ShapelessRecipe(id, group, category, output, toNonNullList(ingredients)),
                false,
                selector,
                inputPrefix,
                materialName,
                false,
                FormConversionPlacement.FIXED));
    }

    public int writtenCount() {
        return recipes.size() - initialSize;
    }

    public boolean isDatapack() {
        return false;
    }

    public void commit() {
        RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
    }

    private static NonNullList<Ingredient> toNonNullList(List<Ingredient> ingredients) {
        if (ingredients instanceof NonNullList<Ingredient> existing) return existing;
        NonNullList<Ingredient> list = NonNullList.createWithCapacity(ingredients.size());
        list.addAll(ingredients);
        return list;
    }
//?}
}
