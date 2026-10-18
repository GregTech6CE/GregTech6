package com.gregtech.gregtech.mixin;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Keep GT entries in their own families, or unfolded if no GT family applies.
 * Optional Reliable EMI integration; never changes item/recipe tags.
 */
@Pseudo
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
public abstract class ReliableEmiNamespaceMixin {
    @Inject(method = "registerMatch", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gregtech$ownFamilies(@Coerce Object group, EmiStack stack,
            Map<?, ?> groups, Map<?, ?> items, Map<?, ?> stacks, CallbackInfo ci) {
        if (!"gt6ce".equals(stack.getId().getNamespace())) return;
        try {
            ResourceLocation id = (ResourceLocation) group.getClass().getMethod("getId").invoke(group);
            if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("c")) ci.cancel();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read Reliable EMI group identity", ex);
        }
    }
}
