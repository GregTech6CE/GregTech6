package gregtech.mixin;

import gregtech.content.tool.VanillaToolDurability;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The loader's default item bars bypass ItemStack.getMaxDamage and its GT6 adjustment. */
@Mixin(Item.class)
public abstract class VanillaToolDurabilityBarMixin {
    @Unique
    private static int gt6ce$barMaximum(ItemStack stack) {
        int original = stack.getItem().getMaxDamage(stack);
        return VanillaToolDurability.maximum(stack, original) == original ? 0 : stack.getMaxDamage();
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true, require = 1)
    private void gt6ce$originalVanillaBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        int maximum = gt6ce$barMaximum(stack);
        if (maximum > 0)
            result.setReturnValue(Mth.clamp(Math.round(13.0F - stack.getDamageValue() * 13.0F / maximum), 0, 13));
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true, require = 1)
    private void gt6ce$originalVanillaBarColor(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        int maximum = gt6ce$barMaximum(stack);
        if (maximum > 0) {
            float remaining = Mth.clamp((maximum - (float) stack.getDamageValue()) / maximum, 0.0F, 1.0F);
            result.setReturnValue(Mth.hsvToRgb(remaining / 3.0F, 1.0F, 1.0F));
        }
    }
}
