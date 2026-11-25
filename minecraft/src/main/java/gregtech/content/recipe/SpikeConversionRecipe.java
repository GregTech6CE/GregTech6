package gregtech.content.recipe;

import gregtech.content.block.misc.SpikeBlock;
import gregtech.content.item.SpikeBlockItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Convert between fixed and falling balls of the same material and family.
 * A dedicated serializer avoids changing the established tool_shapeless wire contract. */
public final class SpikeConversionRecipe extends GTShapelessCraftingRecipe
        implements gregtech.engine.recipe.AutocraftableCraftingRecipe {
    public SpikeConversionRecipe(ShapelessRecipe base) {
        super(base);
    }

    @Override
    public boolean isAutocraftableByGT() {
        return true;
    }

    private boolean accepts(ItemStack input) {
        var output = getResultItem(RegistryAccess.EMPTY);
        if (!(input.getItem() instanceof SpikeBlockItem fromItem)
                || !(output.getItem() instanceof SpikeBlockItem toItem)
                || getIngredients().size() != 1
                || output.getCount() != 1) return false;
        if (((SpikeBlock) fromItem.getBlock()).family() != ((SpikeBlock) toItem.getBlock()).family()) return false;
        var from = SpikeBlock.itemMode(input);
        var to = SpikeBlock.itemMode(output);
        return SpikeBlock.itemSecondary(input) == SpikeBlock.itemSecondary(output)
                && ((from == SpikeBlock.Mode.OMNI && to == SpikeBlock.Mode.FALLING)
                        || (from == SpikeBlock.Mode.FALLING && to == SpikeBlock.Mode.OMNI));
    }

    @Override
    protected boolean onMatches(CraftingGridView grid, Level level) {
        if (!super.onMatches(grid, level)) return false;
        int occupied = 0;
        for (var stack : grid.items()) {
            if (stack.isEmpty()) continue;
            occupied++;
            if (!accepts(stack)) return false;
        }
        return occupied == 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static final RecipeSerializer<SpikeConversionRecipe> SERIALIZER =
            CraftingRecipeCompat.wrapShapelessSerializer(SpikeConversionRecipe::new, r -> r);
}
