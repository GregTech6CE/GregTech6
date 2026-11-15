package gregtech.mixin;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import gregtech.compat.emi.GregTechEMIPlugin;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** EMI's deferred registration API runs before recipe filters and exposes no final row list. */
@Pseudo
@Mixin(targets = "dev.emi.emi.registry.EmiRecipes", remap = false)
public abstract class EmiMaterialProcessRecipesMixin {
    @Shadow
    private static Map<EmiRecipeCategory, List<EmiIngredient>> workstations;

    @ModifyVariable(method = "bake", at = @At("STORE"), ordinal = 0, require = 1, allow = 1, remap = false)
    private static List<EmiRecipe> gregtech$includeMaterialProcesses(List<EmiRecipe> filtered) {
        return GregTechEMIPlugin.addMaterialProcesses(filtered, workstations);
    }
}
