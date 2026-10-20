package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.block.misc.LegacyConstructionBlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Upgrade saved palette items at the native decode boundary, including items in other containers. */
@Mixin(ItemStack.class)
public abstract class LegacyConstructionStackMixin {
//? if >=1.21.1 {
    /*@Inject(method = "parseOptional", at = @At("RETURN"), cancellable = true, require = 1)
*///?} else {
    @Inject(method = "of", at = @At("RETURN"), cancellable = true, require = 1)
//?}
    private static void gt6ce$constructionColor(CallbackInfoReturnable<ItemStack> result) {
        ItemStack stack = result.getReturnValue();
        if (stack != null) result.setReturnValue(LegacyConstructionBlockItem.canonical(stack));
    }
}
