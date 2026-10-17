package com.gregtech.gregtech.mixin;

import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use ordinary item drawing for the gearbox folder's overlapping preview icons.
 * Its entries render correctly individually, but the REMI batch preview can be blank.
 * Optional integration: no effect on other folders, models or recipe ingredients.
 */
@Pseudo
@Mixin(targets = "com.evandev.remi.feature.stackgroup.EmiGroupStack", remap = false)
public abstract class ReliableEmiGearboxIconMixin {
    @Inject(method = "isUnbatchable", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void gregtech$drawGearboxFolderNormally(CallbackInfoReturnable<Boolean> cir) {
        var id = ((EmiStack) (Object) this).getId();
        if (id.getNamespace().equals("gregtech") && id.getPath().equals("material_forms/functional_gearbox"))
            cir.setReturnValue(true);
    }
}
