package gregtech.content.recipe;

import gregtech.content.recipe.WoodCraftingRow.IngredientSpec;
import gregtech.content.recipe.WoodCraftingRow.ItemSpec;
import gregtech.content.recipe.WoodCraftingRow.Shaped;
import gregtech.content.recipe.WoodCraftingRow.Shapeless;
import gregtech.content.recipe.WoodCraftingRow.Tag;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

/** Forge 1.20.1 runtime injector for wood dictionary crafting rows. */
final class WoodCraftingRuntimeInjector {
    private WoodCraftingRuntimeInjector() {}

    static void apply(RecipeManager manager, RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        WoodCraftingRecipeInjector.clearDiagnostics();
        for (var row : GTWoodRecipes.buildingRows()) addRow(recipes, row, CraftingBookCategory.BUILDING);
        for (var row : TreatedWoodCraftingRows.ROWS) addRow(recipes, row, CraftingBookCategory.MISC);
        int added = WoodCraftingRecipeInjector.totalRegisteredCount();
        if (added > 0) RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        var skipped = WoodCraftingRecipeInjector.skipped();
        Mc.logger()
                .info(
                        "Registered {} GT6 wood dictionary crafting rows for {} species ({} skipped)",
                        added,
                        WoodCraftingRecipeInjector.speciesIds().size(),
                        skipped.size());
        for (String reason : skipped) Mc.logger().debug("[gregtech] wood row skipped: {}", reason);
    }

    private static void addRow(List<Recipe<?>> recipes, WoodCraftingRow row, CraftingBookCategory category) {
        boolean treated = category == CraftingBookCategory.MISC;
        var id = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, "wood/" + row.path());
        var output = WoodCraftingRecipeInjector.resolve(row.output());
        if (output.isEmpty()) {
            unresolved(row, "result " + row.output(), treated);
            return;
        }
        output.setCount(row.count());
        if (row.shape() instanceof Shaped shaped) {
            var keys = new LinkedHashMap<Character, Ingredient>();
            for (var key : shaped.keys()) {
                var ingredient = ingredient(key.ingredient());
                if (ingredient == null) {
                    unresolved(row, "key '" + key.symbol() + "' (" + key.ingredient() + ")", treated);
                    return;
                }
                keys.put(key.symbol(), ingredient);
            }
            int width = shaped.pattern().get(0).length();
            int height = shaped.pattern().size();
            var grid = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int y = 0; y < height; y++) {
                var patternRow = shaped.pattern().get(y);
                for (int x = 0; x < width; x++) {
                    char symbol = patternRow.charAt(x);
                    if (symbol != ' ') grid.set(x + y * width, keys.get(symbol));
                }
            }
            var base = new ShapedRecipe(id, "gt.wood", category, width, height, grid, output);
            recipes.add(new ToolShapedRecipe(base, shaped.allowMirror()));
        } else {
            var shapeless = (Shapeless) row.shape();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (var spec : shapeless.ingredients()) {
                var ingredient = ingredient(spec);
                if (ingredient == null) {
                    unresolved(row, "ingredient " + spec, treated);
                    return;
                }
                ingredients.add(ingredient);
            }
            recipes.add(new ShapelessRecipe(id, "gt.wood", category, output, ingredients));
        }
        WoodCraftingRecipeInjector.recordRegistered(id.toString(), treated);
    }

    private static @Nullable Ingredient ingredient(IngredientSpec spec) {
        if (spec instanceof Tag tag) return Ingredient.of(ItemTags.create(ResourceLocation.parse(tag.id())));
        var stack = WoodCraftingRecipeInjector.resolve((ItemSpec) spec);
        return stack.isEmpty() ? null : Ingredient.of(stack);
    }

    private static void unresolved(WoodCraftingRow row, String spec, boolean required) {
        String message = row.path() + " (" + row.source() + "): unresolved " + spec;
        if (required) throw new IllegalStateException(message);
        WoodCraftingRecipeInjector.recordSkipped(message);
    }
}
