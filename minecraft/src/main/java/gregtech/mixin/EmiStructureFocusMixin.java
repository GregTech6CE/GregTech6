package gregtech.mixin;

import dev.emi.emi.api.stack.EmiIngredient;
import gregtech.compat.emi.StructureEmiRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EMI has no uses-navigation callback; focus through its public API after it creates the pages. */
@Pseudo
@Mixin(targets = "dev.emi.emi.api.EmiApi", remap = false)
public abstract class EmiStructureFocusMixin {
    @Inject(
            method = "displayUses(Ldev/emi/emi/api/stack/EmiIngredient;)V",
            at = @At("TAIL"),
            require = 1,
            remap = false)
    private static void gregtech$focusControllerStructure(EmiIngredient ingredient, CallbackInfo callback) {
        StructureEmiRecipe.focusControllerUses(ingredient);
    }
}
