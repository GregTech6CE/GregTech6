package gregtech.mixin;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import gregtech.compat.emi.AtlasEmiRecipe;
import gregtech.compat.emi.MoldChiselEmiRecipe;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EMI adds the recipe background before the recipe supplies its widgets. */
@Pseudo
@Mixin(targets = "dev.emi.emi.screen.WidgetGroup", remap = false)
public abstract class EmiRecipeBorderMixin {
    @Shadow
    @Final
    public List<Widget> widgets;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void gregtech$omitBorder(EmiRecipe recipe, int x, int y, int width, int height, CallbackInfo ci) {
        if (recipe instanceof MoldChiselEmiRecipe || recipe instanceof AtlasEmiRecipe atlas && atlas.isOreChart())
            widgets.removeIf(widget -> widget.getClass().getName().equals("dev.emi.emi.widget.RecipeBackground"));
    }
}
