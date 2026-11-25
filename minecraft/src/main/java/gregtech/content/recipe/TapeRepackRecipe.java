package gregtech.content.recipe;

import gregtech.content.item.behavior.BehaviorDuctTape;
import gregtech.content.item.behavior.ItemBehaviors.Consumable;
import gregtech.engine.stack.StackData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Combines used rolls without increasing the total remaining tape. */
public final class TapeRepackRecipe extends GTShapelessCraftingRecipe {
    public TapeRepackRecipe(ShapelessRecipe base) {
        super(base);
    }

    @Override
    protected boolean onMatches(CraftingGridView grid, Level level) {
        return super.onMatches(grid, level) && !onAssemble(grid, level.registryAccess()).isEmpty();
    }

    @Override
    protected ItemStack onAssemble(CraftingGridView grid, HolderLookup.Provider access) {
        return repack(grid.items());
    }

    private ItemStack repack(Iterable<ItemStack> grid) {
        var output = getResultItem(RegistryAccess.EMPTY);
        Consumable roll = BehaviorDuctTape.consumableOf(output);
        if (roll == null || !roll.isFull(output) || output.getCount() != 1) return ItemStack.EMPTY;
        long remaining = 0;
        for (var input : grid) {
            if (input.isEmpty()) continue;
            if (!roll.isUsed(input)) return ItemStack.EMPTY;
            remaining += Math.max(0, Math.min(roll.uses(), roll.remaining(input)));
        }
        if (remaining == 0) return ItemStack.EMPTY;
        if (remaining >= roll.uses()) return output.copy();
        var result = roll.used().copy();
        var tag = StackData.readOrEmpty(result);
        tag.putLong(roll.key(), remaining);
        StackData.write(result, tag);
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static final RecipeSerializer<TapeRepackRecipe> SERIALIZER =
            CraftingRecipeCompat.wrapShapelessSerializer(TapeRepackRecipe::new, r -> r);
}
