package gregtech.content.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
*///?} else {
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
//?}

/**
 * Thin base class for custom crafting recipes, shielding subclasses from the 1.20.1/1.21.1
 * {@code id}, {@code CraftingContainer}/{@code CraftingInput}, and lookup-type differences.
 */
public abstract class GTCustomCraftingRecipe extends CustomRecipe {
    protected GTCustomCraftingRecipe(@Nullable ResourceLocation id, CraftingBookCategory category) {
//? if >=1.21.1 {
        /*super(category);
*///?} else {
        super(id, category);
//?}
    }

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
//?}

    protected abstract boolean onMatches(CraftingGridView grid, Level level);

    protected abstract ItemStack onAssemble(CraftingGridView grid, HolderLookup.Provider access);

    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView grid) {
        return super.getRemainingItems(grid.raw());
    }
}
