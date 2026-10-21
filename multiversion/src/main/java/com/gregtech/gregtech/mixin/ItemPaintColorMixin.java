package com.gregtech.gregtech.mixin;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads packed block entity paint; native item alpha remains a separate rendering contract. */
@Mixin(ItemColors.class)
public abstract class ItemPaintColorMixin {
    @Inject(method = "getColor", at = @At("RETURN"), cancellable = true, require = 1)
//? if >=1.21.1 {
    /*private void gregtech$paint(ItemStack stack, int tint, CallbackInfoReturnable<Integer> callback) {
        if (tint != 0 || !(stack.getItem() instanceof BlockItem)) return;
        CompoundTag data = stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (data != null && data.getBoolean("gt.painted")) callback.setReturnValue(data.getInt("gt.color") & 0xFFFFFF);
    }
*///?} else {
    private void gregtech$paint(ItemStack stack, int tint, CallbackInfoReturnable<Integer> callback) {
        if (tint != 0 || !(stack.getItem() instanceof BlockItem)) return;
        CompoundTag data = stack.getTagElement("BlockEntityTag");
        if (data != null && data.getBoolean("gt.painted")) callback.setReturnValue(data.getInt("gt.color") & 0xFFFFFF);
    }
//?}
}
