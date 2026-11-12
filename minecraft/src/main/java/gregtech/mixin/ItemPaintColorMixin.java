package gregtech.mixin;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads packed block entity paint; native item alpha remains a separate rendering contract. */
@Mixin(ItemColors.class)
public abstract class ItemPaintColorMixin {
    @Inject(method = "getColor", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$paint(ItemStack stack, int tint, CallbackInfoReturnable<Integer> callback) {
        if (tint != 0 || !(stack.getItem() instanceof BlockItem)) return;
        CompoundTag data = gregtech.engine.stack.BlockEntityData.read(stack);
        if (data != null && data.getBoolean("gt.painted")) callback.setReturnValue(data.getInt("gt.color") & 0xFFFFFF);
    }
}
