package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.recipe.CraftingFormPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** Preserve the source grid before 1.21 trims its empty borders. Vanilla inputs stay cropped. */
@Mixin(CraftingInput.class)
public abstract class CraftingFormPositionMixin implements CraftingFormPosition {
    @Unique private int gregtech$formSize;
    @Unique private int gregtech$formFirst;

    @Override public void gregtech$setFormPosition(int width, int height, int left, int top) {
        gregtech$formSize = width * height;
        gregtech$formFirst = left + top * width;
    }
    @Override public int gregtech$formGridSize() { return gregtech$formSize; }
    @Override public int gregtech$formFirstSlot() { return gregtech$formFirst; }

    @Inject(method = "ofPositioned", at = @At("RETURN"))
    private static void gregtech$rememberFormPosition(int width, int height, List<ItemStack> items,
            CallbackInfoReturnable<CraftingInput.Positioned> callback) {
        var positioned = callback.getReturnValue();
        // EMPTY is a shared singleton and must never carry another grid's position.
        if (!positioned.input().isEmpty())
            ((CraftingFormPosition) positioned.input()).gregtech$setFormPosition(
                    width, height, positioned.left(), positioned.top());
    }
}
