package gregtech.mixin;

import gregtech.compat.jei.RecipeBorderPreference;
import mezz.jei.api.gui.drawable.IScalableDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** JEI 15 predates the per-category border option available in JEI 19. */
@Pseudo
@Mixin(targets = "mezz.jei.library.gui.recipes.RecipeLayout", remap = false)
public abstract class JeiRecipeBorderMixin {
    @Shadow
    @Final
    private IRecipeCategory<?> recipeCategory;

    @Redirect(
            method = "drawRecipe",
            require = 0,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lmezz/jei/api/gui/drawable/IScalableDrawable;draw(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/Rect2i;)V"))
    private void gregtech$drawRecipeBorder(IScalableDrawable border, GuiGraphics graphics, Rect2i area) {
        if (recipeCategory instanceof RecipeBorderPreference category && !category.needsRecipeBorder()) return;
        border.draw(graphics, area);
    }
}
