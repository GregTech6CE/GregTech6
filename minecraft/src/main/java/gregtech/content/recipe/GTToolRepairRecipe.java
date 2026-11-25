package gregtech.content.recipe;

import gregtech.GregTechConfig;
import gregtech.content.item.GTToolItem;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Optional vanilla-style grid repair, preserving GT head/handle and intrinsic enchantments. */
public final class GTToolRepairRecipe extends GTCustomCraftingRecipe {
    public static final RecipeSerializer<GTToolRepairRecipe> SERIALIZER =
            CraftingRecipeCompat.simpleCustomSerializer(GTToolRepairRecipe::new);

    public GTToolRepairRecipe(@Nullable ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private static java.util.List<ItemStack> inputs(CraftingGridView grid) {
        int size = grid.size();
        if (!GregTechConfig.Rule.TOOL_MERGING.enabled()) return java.util.List.of();
        var tools = new java.util.ArrayList<ItemStack>(2);
        for (int i = 0; i < size; i++) {
            var stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (tools.size() == 2
                    || stack.getCount() != 1
                    || !(stack.getItem() instanceof GTToolItem)
                    || !stack.isDamageableItem()) return java.util.List.of();
            tools.add(stack);
        }
        if (tools.size() != 2) return java.util.List.of();
        var first = tools.get(0);
        var second = tools.get(1);
        if (first.getItem() != second.getItem()
                || first.getMaxDamage() != second.getMaxDamage()
                || GTToolHelper.getHead(first) != GTToolHelper.getHead(second)
                || GTToolHelper.getHandle(first) != GTToolHelper.getHandle(second)) return java.util.List.of();
        return tools;
    }

    @Override
    protected boolean onMatches(CraftingGridView grid, Level level) {
        return !inputs(grid).isEmpty();
    }

    @Override
    protected ItemStack onAssemble(CraftingGridView grid, HolderLookup.Provider lookup) {
        var tools = inputs(grid);
        if (tools.isEmpty()) return ItemStack.EMPTY;
        var first = tools.get(0);
        var second = tools.get(1);
        var result = GTToolItem.create(
                GTToolHelper.getType(first), GTToolHelper.getHead(first), GTToolHelper.getHandle(first));
        int max = first.getMaxDamage();
        long remaining = (long) max - first.getDamageValue() + max - second.getDamageValue() + max * 5L / 100;
        result.setDamageValue((int) Math.max(0, max - remaining));
        // Vanilla grid repair removes acquired enchantments, but retains curses. Material bonuses stay.
        gregtech.engine.tool.GTToolEnchantments.apply(result, lookup);
        for (var tool : tools) gregtech.engine.stack.EnchantmentCompat.copyCurses(tool, result);
        return result;
    }

    @Override
    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView grid) {
        return NonNullList.withSize(grid.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
