package gregtech.mixin;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.compat.emi.MaterialUsesIndex;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** EMI exposes no API for compressed uses indices. Keep the adaptation local to each manager. */
@Pseudo
@Mixin(targets = "dev.emi.emi.registry.EmiRecipes$Manager", remap = false)
public abstract class EmiMaterialUsesMixin {
    @Shadow
    private Map<EmiStack, List<EmiRecipe>> byInput;

    @Shadow
    @Final
    private List<EmiRecipe> recipes;

    @Unique
    private MaterialUsesIndex gregtech$materialUses;

    @Redirect(
            method = "<init>(Ljava/util/List;Ljava/util/Map;Ljava/util/List;Z)V",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/recipe/EmiRecipe;getInputs()Ljava/util/List;"),
            require = 1)
    private List<EmiIngredient> gregtech$compactMaterialInputs(EmiRecipe recipe) {
        return MaterialUsesIndex.compactInputs(recipe.getInputs());
    }

    @Redirect(
            method = "<init>(Ljava/util/List;Ljava/util/Map;Ljava/util/List;Z)V",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/recipe/EmiRecipe;getCatalysts()Ljava/util/List;"),
            require = 1)
    private List<EmiIngredient> gregtech$compactMaterialCatalysts(EmiRecipe recipe) {
        return MaterialUsesIndex.compactInputs(recipe.getCatalysts());
    }

    @Inject(method = "<init>(Ljava/util/List;Ljava/util/Map;Ljava/util/List;Z)V", at = @At("RETURN"), require = 1)
    private void gregtech$buildMaterialUses(CallbackInfo callback) {
        var comparison = ((Object2ObjectOpenCustomHashMap<EmiStack, List<EmiRecipe>>) byInput).strategy();
        gregtech$materialUses = new MaterialUsesIndex(recipes, comparison);
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gt6ce] EMI material uses: {} expanded relations -> {} shared relations",
                        gregtech$materialUses.expandedEdges(),
                        gregtech$materialUses.sharedEdges());
    }

    @Inject(method = "getRecipesByInput", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$materialUses(EmiStack stack, CallbackInfoReturnable<List<EmiRecipe>> callback) {
        if (gregtech$materialUses != null)
            callback.setReturnValue(gregtech$materialUses.uses(stack, callback.getReturnValue()));
    }
}
