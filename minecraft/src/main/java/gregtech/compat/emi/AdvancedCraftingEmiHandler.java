package gregtech.compat.emi;

import gregtech.client.gui.AdvancedCraftingMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;
import java.util.List;

/** Standard EMI fill and instant-craft integration for both advanced and charging worktables. */
public final class AdvancedCraftingEmiHandler implements StandardRecipeHandler<AdvancedCraftingMenu> {
    @Override public List<Slot> getInputSources(AdvancedCraftingMenu menu){return menu.transferInputSources();}
    @Override public List<Slot> getCraftingSlots(AdvancedCraftingMenu menu){return menu.craftingSlots();}
    @Override public Slot getOutputSlot(AdvancedCraftingMenu menu){return menu.getSlot(AdvancedCraftingMenu.RESULT);}
    @Override public boolean supportsRecipe(EmiRecipe recipe){
        return recipe.getCategory()==VanillaEmiRecipeCategories.CRAFTING&&recipe.supportsRecipeTree()&&recipe.getInputs().size()<=9;
    }
}
