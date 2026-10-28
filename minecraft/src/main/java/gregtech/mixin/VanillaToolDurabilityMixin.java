package gregtech.mixin;

import gregtech.content.tool.VanillaToolDurability;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shared stack query covers durability use, repair and numeric tooltips on both modern versions. */
@Mixin(ItemStack.class)
public abstract class VanillaToolDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true, require = 1)
    private void gt6ce$originalVanillaDurability(CallbackInfoReturnable<Integer> result) {
        int original = result.getReturnValueI();
        int maximum = VanillaToolDurability.maximum((ItemStack) (Object) this, original);
        if (maximum != original) result.setReturnValue(maximum);
    }
}
