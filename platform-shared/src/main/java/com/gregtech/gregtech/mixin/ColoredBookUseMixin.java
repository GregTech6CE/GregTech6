package com.gregtech.gregtech.mixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** Preserve native written-book resolution, packets and lectern behavior for GT6 cover items. */
@Mixin(ServerPlayer.class)
public abstract class ColoredBookUseMixin {
    @Redirect(method="openItemGui",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),require=1)
    private boolean gregtech$writtenCover(ItemStack stack,Item item){return stack.is(item)||(item==Items.WRITTEN_BOOK&&stack.getItem() instanceof com.gregtech.gregtech.item.ColoredBookItem);}
}
