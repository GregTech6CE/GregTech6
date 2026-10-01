package com.gregtech.gregtech.mixin;
import net.minecraft.client.color.item.ItemColors;import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.BlockItem;import net.minecraft.nbt.CompoundTag;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Packed original block entity items use the same persisted primary paint tint. */
@Mixin(ItemColors.class)public abstract class ItemPaintColorMixin {
@Inject(method="getColor",at=@At("RETURN"),cancellable=true,require=1)
private void gregtech$paint(ItemStack stack,int tint,CallbackInfoReturnable<Integer> cir){if(tint!=0||!(stack.getItem() instanceof BlockItem))return;CompoundTag data=stack.getTagElement("BlockEntityTag");if(data!=null&&data.getBoolean("gt.painted"))cir.setReturnValue(data.getInt("gt.color")&0xFFFFFF);}
}
