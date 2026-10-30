package gregtech.mixin;

import gregtech.content.recipe.CraftingFormPosition;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve the source grid before 1.21 trims its empty borders. Vanilla inputs stay cropped. */
@Mixin(CraftingInput.class)
public abstract class CraftingFormPositionMixin implements CraftingFormPosition {
    @Unique
    private int gregtech$formSize;

    @Unique
    private int gregtech$formFirst;

    @Override
    public void gregtech$setFormPosition(int width, int height, int left, int top) {
        gregtech$formSize = width * height;
        gregtech$formFirst = left + top * width;
    }

    @Override
    public int gregtech$formGridSize() {
        return gregtech$formSize;
    }

    @Override
    public int gregtech$formFirstSlot() {
        return gregtech$formFirst;
    }

    // Single-input conversions depend on leading empty slots. Cache keys must not
    // equate two trimmed 1x1 inputs that came from different positions in the grid.
    @Inject(method = "equals", at = @At("RETURN"), cancellable = true)
    private void gregtech$compareFormPosition(Object other, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()
                && ((CraftingInput) (Object) this).ingredientCount() == 1
                && other instanceof CraftingFormPosition position) {
            callback.setReturnValue(gregtech$formSize == position.gregtech$formGridSize()
                    && gregtech$formFirst == position.gregtech$formFirstSlot());
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true)
    private void gregtech$hashFormPosition(CallbackInfoReturnable<Integer> callback) {
        if (((CraftingInput) (Object) this).ingredientCount() == 1)
            callback.setReturnValue(31 * (31 * callback.getReturnValue() + gregtech$formSize) + gregtech$formFirst);
    }

    @Inject(method = "ofPositioned", at = @At("RETURN"))
    private static void gregtech$rememberFormPosition(
            int width, int height, List<ItemStack> items, CallbackInfoReturnable<CraftingInput.Positioned> callback) {
        var positioned = callback.getReturnValue();
        // EMPTY is a shared singleton and must never carry another grid's position.
        if (!positioned.input().isEmpty())
            ((CraftingFormPosition) positioned.input())
                    .gregtech$setFormPosition(width, height, positioned.left(), positioned.top());
    }
}
