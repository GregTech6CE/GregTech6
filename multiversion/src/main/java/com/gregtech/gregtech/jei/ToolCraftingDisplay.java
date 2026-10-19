package com.gregtech.gregtech.jei;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.api.fluid.FluidContainerDisplayBinding;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import java.util.List;
/** Native crafting layout plus full-bucket lookup of the actual empty shell recipe. */
final class ToolCraftingDisplay {
    static void register(IVanillaCategoryExtensionRegistration registration) {
//? if >=1.21.1 {
        /*registration.getCraftingCategory().addExtension(ToolShapedRecipe.class, new ICraftingCategoryExtension<ToolShapedRecipe>() {
            @Override public void setRecipe(net.minecraft.world.item.crafting.RecipeHolder<ToolShapedRecipe> holder,
                    IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) { layout(holder.value(), builder, grid); }
        });
*///?} else {
        registration.getCraftingCategory().addCategoryExtension(ToolShapedRecipe.class, recipe -> new ICraftingCategoryExtension() {
            @Override public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) { layout(recipe, builder, grid); }
            @Override public net.minecraft.resources.ResourceLocation getRegistryName() { return recipe.getId(); }
        });
//?}
    }
    private static void layout(ToolShapedRecipe recipe, IRecipeLayoutBuilder builder, ICraftingGridHelper grid) {
        int width = 1, height = 1;
        while (width < 3 && !recipe.canCraftInDimensions(width, 3)) width++;
        while (height < 3 && !recipe.canCraftInDimensions(3, height)) height++;
        grid.createAndSetInputs(builder, recipe.getIngredients().stream().map(i -> List.of(i.getItems())).toList(), width, height);
        var output = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        grid.createAndSetOutputs(builder, List.of(output));
        var variants = FluidContainerDisplayBinding.bucketVariants(output);
        if (!variants.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(variants);
    }
}
