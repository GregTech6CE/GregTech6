package gregtech.mixin;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import gregtech.client.MaterialProcessScreen;
import gregtech.compat.emi.MaterialProcessEmiRecipe;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EMI does not expose the lookup ingredient to recipe widgets; retain it before constructing their pages. */
@Pseudo
@Mixin(targets = "dev.emi.emi.api.EmiApi", remap = false)
public abstract class EmiMaterialProcessFocusMixin {
    @Inject(
            method = "setPages(Ljava/util/Map;Ldev/emi/emi/api/stack/EmiIngredient;)V",
            at = @At("HEAD"),
            require = 1,
            remap = false)
    private static void gregtech$focusMaterialProcess(
            Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient ingredient, CallbackInfo callback) {
        // EMI opens lookups only from its recipe/tree screens or a container screen.
        if (recipes.values().stream().anyMatch(rows -> !rows.isEmpty())
                && Minecraft.getInstance().screen instanceof MaterialProcessScreen screen) screen.onClose();
        if (ingredient.isEmpty()) return;
        for (var recipe : recipes.getOrDefault(MaterialProcessEmiRecipe.category(), List.of()))
            if (recipe instanceof MaterialProcessEmiRecipe process) process.focus(ingredient);
    }
}
