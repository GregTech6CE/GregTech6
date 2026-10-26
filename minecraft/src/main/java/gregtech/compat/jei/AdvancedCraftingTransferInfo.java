package gregtech.compat.jei;

import gregtech.client.gui.AdvancedCraftingMenu;
import gregtech.content.registry.GTMenuTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}
import java.util.List;
import java.util.Optional;

/** Uses JEI's validated transfer protocol and the actual 3×3 grid, rather than a ghost recipe. */
//? if >=1.21.1 {
/*public final class AdvancedCraftingTransferInfo implements IRecipeTransferInfo<AdvancedCraftingMenu,RecipeHolder<CraftingRecipe>> {
    @Override public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType(){return RecipeTypes.CRAFTING;}
    @Override public boolean canHandle(AdvancedCraftingMenu menu,RecipeHolder<CraftingRecipe> recipe){return recipe.value().canCraftInDimensions(3,3);}
    @Override public List<Slot> getRecipeSlots(AdvancedCraftingMenu menu,RecipeHolder<CraftingRecipe> recipe){return menu.craftingSlots();}
    @Override public List<Slot> getInventorySlots(AdvancedCraftingMenu menu,RecipeHolder<CraftingRecipe> recipe){return menu.transferInventorySlots();}
*///?} else {
public final class AdvancedCraftingTransferInfo implements IRecipeTransferInfo<AdvancedCraftingMenu,CraftingRecipe> {
    @Override public RecipeType<CraftingRecipe> getRecipeType(){return RecipeTypes.CRAFTING;}
    @Override public boolean canHandle(AdvancedCraftingMenu menu,CraftingRecipe recipe){return recipe.canCraftInDimensions(3,3);}
    @Override public List<Slot> getRecipeSlots(AdvancedCraftingMenu menu,CraftingRecipe recipe){return menu.craftingSlots();}
    @Override public List<Slot> getInventorySlots(AdvancedCraftingMenu menu,CraftingRecipe recipe){return menu.transferInventorySlots();}
//?}
    @Override public Class<AdvancedCraftingMenu> getContainerClass(){return AdvancedCraftingMenu.class;}
    @Override public Optional<MenuType<AdvancedCraftingMenu>> getMenuType(){return Optional.of(GTMenuTypes.ADVANCED_CRAFTING.get());}
}
