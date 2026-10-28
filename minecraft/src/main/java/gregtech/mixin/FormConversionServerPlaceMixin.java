package gregtech.mixin;

import gregtech.content.recipe.FormConversionRecipeBook;
import net.minecraft.recipebook.ServerPlaceRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Iterator;

/** Keep vanilla inventory accounting while placing position-sensitive form recipes. */
@Mixin(ServerPlaceRecipe.class)
public abstract class FormConversionServerPlaceMixin {
    @Redirect(method = "handleRecipeClicked", at = @At(value = "INVOKE",
//? if >=1.21.1 {
            /*target = "Lnet/minecraft/recipebook/ServerPlaceRecipe;placeRecipe(IIILnet/minecraft/world/item/crafting/RecipeHolder;Ljava/util/Iterator;I)V"), require = 1)
    private void gregtech$placeForm(ServerPlaceRecipe<?, ?> target, int width, int height, int resultSlot,
            net.minecraft.world.item.crafting.RecipeHolder<?> recipe, Iterator<Integer> inputs, int count) {
*///?} else {
            target = "Lnet/minecraft/recipebook/ServerPlaceRecipe;placeRecipe(IIILnet/minecraft/world/item/crafting/Recipe;Ljava/util/Iterator;I)V"), require = 1)
    private void gregtech$placeForm(ServerPlaceRecipe<?> target, int width, int height, int resultSlot,
            net.minecraft.world.item.crafting.Recipe<?> recipe, Iterator<Integer> inputs, int count) {
//?}
        FormConversionRecipeBook.place(target, width, height, resultSlot, recipe, inputs, count);
    }
}
