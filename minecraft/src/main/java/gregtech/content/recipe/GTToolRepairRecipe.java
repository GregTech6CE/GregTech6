package gregtech.content.recipe;

import gregtech.GregTechConfig;
import gregtech.content.item.GTToolItem;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.core.HolderLookup;
*///?} else {
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
//?}

/** Optional vanilla-style grid repair, preserving GT head/handle and intrinsic enchantments. */
public final class GTToolRepairRecipe extends CustomRecipe {
    public static final RecipeSerializer<GTToolRepairRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(GTToolRepairRecipe::new);

//? if >=1.21.1 {
    /*public GTToolRepairRecipe(CraftingBookCategory category) { super(category); }
    private static java.util.List<ItemStack> inputs(CraftingInput grid) {
        int size = grid.size();
*///?} else {
    public GTToolRepairRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    private static java.util.List<ItemStack> inputs(CraftingContainer grid) {
        int size = grid.getContainerSize();
//?}
        if (!GregTechConfig.Rule.TOOL_MERGING.enabled()) return java.util.List.of();
        var tools = new java.util.ArrayList<ItemStack>(2);
        for (int i = 0; i < size; i++) {
            var stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (tools.size() == 2 || stack.getCount() != 1 || !(stack.getItem() instanceof GTToolItem)
                    || !stack.isDamageableItem()) return java.util.List.of();
            tools.add(stack);
        }
        if (tools.size() != 2) return java.util.List.of();
        var first = tools.get(0);
        var second = tools.get(1);
        if (first.getItem() != second.getItem() || first.getMaxDamage() != second.getMaxDamage()
                || GTToolHelper.getHead(first) != GTToolHelper.getHead(second)
                || GTToolHelper.getHandle(first) != GTToolHelper.getHandle(second)) return java.util.List.of();
        return tools;
    }

    @Override
//? if >=1.21.1 {
    /*public boolean matches(CraftingInput grid, Level level) {
*///?} else {
    public boolean matches(CraftingContainer grid, Level level) {
//?}
        return !inputs(grid).isEmpty();
    }

    @Override
//? if >=1.21.1 {
    /*public ItemStack assemble(CraftingInput grid, HolderLookup.Provider lookup) {
*///?} else {
    public ItemStack assemble(CraftingContainer grid, RegistryAccess lookup) {
//?}
        var tools = inputs(grid);
        if (tools.isEmpty()) return ItemStack.EMPTY;
        var first = tools.get(0);
        var second = tools.get(1);
        var result = GTToolItem.create(GTToolHelper.getType(first), GTToolHelper.getHead(first), GTToolHelper.getHandle(first));
        int max = first.getMaxDamage();
        long remaining = (long) max - first.getDamageValue() + max - second.getDamageValue() + max * 5L / 100;
        result.setDamageValue((int) Math.max(0, max - remaining));
        // Vanilla grid repair removes acquired enchantments, but retains curses. Material bonuses stay.
//? if >=1.21.1 {
        /*gregtech.engine.tool.GTToolEnchantments.apply(result, lookup);
        var enchantments = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                result.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                        net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY));
        for (var tool : tools) for (var entry : tool.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).entrySet()) {
            if (entry.getKey().is(net.minecraft.tags.EnchantmentTags.CURSE))
                enchantments.upgrade(entry.getKey(), entry.getIntValue());
        }
        result.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchantments.toImmutable());
*///?} else {
        gregtech.engine.tool.GTToolEnchantments.apply(result);
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(result);
        for (var tool : tools) net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(tool)
                .forEach((enchantment, level) -> {
                    if (enchantment.isCurse()) enchantments.merge(enchantment, level, Math::max);
                });
        net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(enchantments, result);
//?}
        return result;
    }

    @Override
//? if >=1.21.1 {
    /*public NonNullList<ItemStack> getRemainingItems(CraftingInput grid) {
        return NonNullList.withSize(grid.size(), ItemStack.EMPTY);
*///?} else {
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        return NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
//?}
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
}
