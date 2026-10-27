package gregtech.mixin;

import gregtech.content.recipe.GTToolAssemblyRecipe;
import gregtech.content.recipe.GTToolPatternRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use the actual grid's material, not a representative steel tool, in Polymorph's choices. */
@Pseudo
@Mixin(targets = "com.illusivesoulworks.polymorph.common.capability.AbstractRecipeData", remap = false)
public abstract class PolymorphToolResultMixin {
    @Redirect(method = "getRecipe", at = @At(value = "INVOKE",
//? if >=1.21.1 {
            /*target = "Lnet/minecraft/world/item/crafting/Recipe;getResultItem(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
*///?} else {
            target = "Lnet/minecraft/world/item/crafting/Recipe;getResultItem(Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
//?}
            remap = true), require = 0, remap = false)
//? if >=1.21.1 {
    /*private ItemStack gt6ce$dynamicToolResult(Recipe<?> recipe, net.minecraft.core.HolderLookup.Provider access) {
*///?} else {
    private ItemStack gt6ce$dynamicToolResult(Recipe<?> recipe, net.minecraft.core.RegistryAccess access) {
//?}
        // Polymorph already calls assemble(input, access) when the display result is empty.
        // Leave the recipe's own display result intact for recipe books and JEI/EMI.
        if (recipe instanceof GTToolPatternRecipe || recipe instanceof GTToolAssemblyRecipe)
            return ItemStack.EMPTY;
        return recipe.getResultItem(access);
    }
}
