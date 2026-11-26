package gregtech.content.recipe;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
*///?} else {
import net.minecraft.world.inventory.CraftingContainer;
//?}

/**
 * Thin base class for shaped crafting recipes, shielding subclasses from 1.20.1/1.21.1
 * constructor {@code id}/{@code ShapedRecipePattern}, grid type, and registry-lookup differences.
 */
public abstract class GTShapedCraftingRecipe extends ShapedRecipe {
//? if >=1.21.1 {
    /*protected GTShapedCraftingRecipe(ShapedRecipe base) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY),
                base.showNotification());
    }

    public static ShapedRecipe shaped(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            int width,
            int height,
            NonNullList<Ingredient> ingredients,
            ItemStack result) {
        return new ShapedRecipe(
                group, category, new ShapedRecipePattern(width, height, ingredients, Optional.empty()), result);
    }
*///?} else {
    protected GTShapedCraftingRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY));
    }

    public static ShapedRecipe shaped(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            int width,
            int height,
            NonNullList<Ingredient> ingredients,
            ItemStack result) {
        return new ShapedRecipe(id, group, category, width, height, ingredients, result);
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
