package gregtech.mixin;

import gregtech.content.recipe.FormConversionRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Iterator;

/** The recipe-book ghost uses exactly the same cell as its server placement. Client-only mixin. */
@Mixin(RecipeBookComponent.class)
public abstract class FormConversionGhostPlaceMixin {
    @Redirect(method = "setupGhostRecipe", at = @At(value = "INVOKE",
//? if >=1.21.1 {
            /*target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;placeRecipe(IIILnet/minecraft/world/item/crafting/RecipeHolder;Ljava/util/Iterator;I)V"), require = 1)
    private void gregtech$placeFormGhost(RecipeBookComponent target, int width, int height, int resultSlot,
            net.minecraft.world.item.crafting.RecipeHolder<?> recipe, Iterator<Ingredient> inputs, int count) {
*///?} else {
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;placeRecipe(IIILnet/minecraft/world/item/crafting/Recipe;Ljava/util/Iterator;I)V"), require = 1)
    private void gregtech$placeFormGhost(RecipeBookComponent target, int width, int height, int resultSlot,
            net.minecraft.world.item.crafting.Recipe<?> recipe, Iterator<Ingredient> inputs, int count) {
//?}
        FormConversionRecipeBook.place(target, width, height, resultSlot, recipe, inputs, count);
    }
}
