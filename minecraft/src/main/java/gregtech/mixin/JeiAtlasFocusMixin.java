package gregtech.mixin;

import gregtech.compat.common.MaterialAtlas;
import gregtech.compat.jei.AtlasCategory;
import java.util.List;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.gui.recipes.layouts.IRecipeLayoutList;
import mezz.jei.gui.recipes.lookups.ILookupState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Select the initial tab of an ingredient lookup without changing the user's category order. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.lookups.IngredientLookupState", remap = false)
public abstract class JeiAtlasFocusMixin implements ILookupState {
    @Shadow
    private int recipeCategoryIndex;

    @Shadow
    private int recipeIndex;

    @Unique
    private Object gregtech$previewRecipe;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void gregtech$selectAtlas(
            IRecipeManager manager, IFocusGroup focus, List<IRecipeCategory<?>> categories, CallbackInfo ci) {
        if (focus.getAllFocuses().size() != 1) return;
        var preview = AtlasCategory.openingPreview();
        if (preview != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (preview.type().equals(categories.get(i).getRecipeType())) {
                    recipeCategoryIndex = i;
                    gregtech$previewRecipe = preview.recipe();
                    return;
                }
            }
        }
        gregtech$toolHead = focus.getFocuses(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)
                .map(f -> f.getTypedValue().getIngredient())
                .map(MaterialAtlas::toolMaterial)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Unique
    private gregtech.api.material.GTMaterial gregtech$toolHead;

    // JEI sorts bookmarks and craftable recipes before pagination. Locate the preview in that order once.
    @Override
//? if >=1.21.1 {
    /*public List<mezz.jei.gui.recipes.IRecipeLayoutWithButtons<?>> getVisible(IRecipeLayoutList layouts) {
*///?} else {
    public List<mezz.jei.gui.recipes.RecipeLayoutWithButtons<?>> getVisible(IRecipeLayoutList layouts) {
//?}
        if (gregtech$toolHead != null && getFocusedRecipes().getRecipeCategory() instanceof AtlasCategory atlas
                && atlas.getRecipeType().getUid().getPath().equals(MaterialAtlas.Kind.MATERIAL.id)) {
            for (var candidate : getFocusedRecipes().getRecipes()) {
                if (candidate instanceof MaterialAtlas.Page page && page.entry().material == gregtech$toolHead) {
                    gregtech$previewRecipe = page;
                    break;
                }
            }
            gregtech$toolHead = null;
        }
        if (gregtech$previewRecipe != null) {
            var target = gregtech$previewRecipe;
            gregtech$previewRecipe = null;
//? if >=1.21.1 {
            /*var recipes = getFocusedRecipes().getRecipes();
            if (recipes.contains(target) && layouts instanceof gregtech.compat.jei.AtlasLayoutLookup lookup) {
                int currentIndex = recipeIndex;
                lookup.locate(target, recipes, index -> {
                    if (recipeIndex == currentIndex) recipeIndex = index - index % getRecipesPerPage();
                });
            }
*///?} else {
            int perPage = getRecipesPerPage();
            for (int start = 0; start < layouts.size(); start += perPage) {
                var page = layouts.subList(start, Math.min(start + perPage, layouts.size()));
                if (page.stream().anyMatch(layout -> target.equals(layout.recipeLayout().getRecipe()))) {
                    recipeIndex = start;
                    return page;
                }
            }
//?}
        }
        return ILookupState.super.getVisible(layouts);
    }
}
