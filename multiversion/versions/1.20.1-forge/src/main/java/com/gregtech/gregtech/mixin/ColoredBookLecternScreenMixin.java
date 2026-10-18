package com.gregtech.gregtech.mixin;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge's lectern reader still dispatches by vanilla item identity; use its native written reader. */
@Mixin(LecternScreen.class)
public abstract class ColoredBookLecternScreenMixin {
    @Redirect(method="bookChanged",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;fromItem(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;"),require=1)
    private BookViewScreen.BookAccess gregtech$writtenCover(ItemStack stack) {
        return stack.getItem() instanceof com.gregtech.gregtech.item.ColoredBookItem
                ? new BookViewScreen.WrittenBookAccess(stack) : BookViewScreen.BookAccess.fromItem(stack);
    }
}
