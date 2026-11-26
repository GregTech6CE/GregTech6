package gregtech.mixin;

import gregtech.compat.jei.AtlasCategory;
import mezz.jei.api.recipe.category.IRecipeCategory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** JEI caches a recipe rectangle without its available height; resize only the atlas layouts. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipeGuiLogic", remap = false)
public abstract class JeiAtlasSizeMixin {
    @Shadow
    private IRecipeCategory<?> cachedRecipeCategory;

    @Shadow
    public abstract IRecipeCategory<?> getSelectedRecipeCategory();

    @ModifyVariable(
            method = "getVisibleRecipeLayoutsWithButtons",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 1)
    private int gregtech$fitAtlas(int availableHeight) {
        if (getSelectedRecipeCategory() instanceof AtlasCategory atlas && atlas.fitHeight(availableHeight))
            cachedRecipeCategory = null;
        return availableHeight;
    }
}
