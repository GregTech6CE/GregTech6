package gregtech.content.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
*///?} else {
import net.minecraft.world.inventory.CraftingContainer;
//?}

/**
 * Thin base class for shapeless crafting recipes, shielding subclasses from 1.20.1/1.21.1
 * constructor {@code id}, grid type, and registry-lookup differences.
 */
public abstract class GTShapelessCraftingRecipe extends ShapelessRecipe {
//? if >=1.21.1 {
    /*protected GTShapelessCraftingRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    protected GTShapelessCraftingRecipe(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack result,
            NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }
*///?} else {
    protected GTShapelessCraftingRecipe(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY),
                base.getIngredients());
    }

    protected GTShapelessCraftingRecipe(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            ItemStack result,
            NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }
//?}

//? if >=1.21.1 {
    /*@Override
    public final boolean matches(CraftingInput grid, Level level) {
        return onMatches(CraftingGridView.of(grid), level);
    }

    @Override
    public final ItemStack assemble(CraftingInput grid, HolderLookup.Provider access) {
        return onAssemble(CraftingGridView.of(grid), access);
    }

    @Override
    public final NonNullList<ItemStack> getRemainingItems(CraftingInput grid) {
        return onGetRemainingItems(CraftingGridView.of(grid));
    }

    protected ItemStack onAssemble(CraftingGridView grid, HolderLookup.Provider access) {
        return super.assemble(grid.raw(), access);
    }
*///?} else {
    @Override
    public final boolean matches(CraftingContainer grid, Level level) {
        return onMatches(CraftingGridView.of(grid), level);
    }

    @Override
    public final ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return onAssemble(CraftingGridView.of(grid), access);
    }

    @Override
    public final NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        return onGetRemainingItems(CraftingGridView.of(grid));
    }

    protected ItemStack onAssemble(CraftingGridView grid, HolderLookup.Provider access) {
        return super.assemble(
                grid.raw(), access instanceof RegistryAccess registryAccess ? registryAccess : RegistryAccess.EMPTY);
    }
//?}

    protected boolean onMatches(CraftingGridView grid, Level level) {
        return super.matches(grid.raw(), level);
    }

    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView grid) {
        return super.getRemainingItems(grid.raw());
    }
}
