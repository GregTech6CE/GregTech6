package gregtech.compat.jei;

import gregtech.compat.common.MaterialAtlas;
import java.util.List;
import java.util.function.Supplier;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;

/** Resolve tool heads for the atlas without changing how other JEI categories compare tools. */
record AtlasToolLookup(Supplier<MaterialAtlas> atlas) implements ISimpleRecipeManagerPlugin<MaterialAtlas.Page> {
    private MaterialAtlas.Page page(ITypedIngredient<?> ingredient) {
        var current = atlas.get();
        return current == null
                ? null
                : ingredient
                        .getIngredient(VanillaTypes.ITEM_STACK)
                        .map(current::toolPage)
                        .orElse(null);
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
        return page(input) != null;
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
        return page(output) != null;
    }

    @Override
    public List<MaterialAtlas.Page> getRecipesForInput(ITypedIngredient<?> input) {
        var page = page(input);
        return page == null ? List.of() : List.of(page);
    }

    @Override
    public List<MaterialAtlas.Page> getRecipesForOutput(ITypedIngredient<?> output) {
        return getRecipesForInput(output);
    }

    @Override
    public List<MaterialAtlas.Page> getAllRecipes() {
        return List.of();
    }
}
