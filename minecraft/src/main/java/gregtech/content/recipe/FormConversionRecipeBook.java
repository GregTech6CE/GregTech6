package gregtech.content.recipe;

import net.minecraft.recipebook.PlaceRecipe;
import java.util.Iterator;

/** Choose only the fixed cell; vanilla callbacks still own inventory movement and ghost rendering. */
public final class FormConversionRecipeBook {
    private FormConversionRecipeBook() {}

    public static <T> void place(PlaceRecipe<T> target, int width, int height, int resultSlot,
//? if >=1.21.1 {
            /*net.minecraft.world.item.crafting.RecipeHolder<?> holder, Iterator<T> inputs, int count) {
        var recipe = holder.value();
*///?} else {
            net.minecraft.world.item.crafting.Recipe<?> recipe, Iterator<T> inputs, int count) {
//?}
        if (recipe instanceof ToolShapelessRecipe conversion && conversion.formSelector().variants() > 1
                && conversion.getIngredients().size() == 1) {
            int offset = conversion.formSelector().offset();
            // An undersized grid or missing ingredient must never fall back to the wrong first slot.
            if (width <= 0 || height <= 0 || offset >= width * height || !inputs.hasNext()) return;
            int slot = offset + (resultSlot >= 0 && resultSlot <= offset ? 1 : 0);
//? if >=1.21.1 {
            /*target.addItemToSlot(inputs.next(), slot, count, offset % width, offset / width);
*///?} else {
            target.addItemToSlot(inputs, slot, count, offset / width, offset % width);
//?}
            return;
        }
//? if >=1.21.1 {
        /*target.placeRecipe(width, height, resultSlot, holder, inputs, count);
*///?} else {
        target.placeRecipe(width, height, resultSlot, recipe, inputs, count);
//?}
    }
}
