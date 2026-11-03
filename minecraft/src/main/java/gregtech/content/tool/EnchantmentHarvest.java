package gregtech.content.tool;

import gregtech.engine.tool.MaterialEnchantmentStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/** GT6 HarvestDropsEvent: Fire Aspect III smelts harvested drops without consuming furnace fuel. */
public final class EnchantmentHarvest {
    private EnchantmentHarvest() {}

    public static List<ItemStack> smelt(ServerLevel level, ItemStack tool, List<ItemStack> drops) {
        if (MaterialEnchantmentStacks.level(tool, "fire_aspect", level) < 3 || drops.isEmpty()) return drops;
        List<ItemStack> result = new ArrayList<>(drops.size());
        for (var drop : drops) {
//? if >=1.21.1 {
            /*var input = new net.minecraft.world.item.crafting.SingleRecipeInput(drop.copyWithCount(1));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            var output = recipe.map(value -> value.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
*///?} else {
            var input = new net.minecraft.world.SimpleContainer(drop.copyWithCount(1));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            var output = recipe.map(value -> value.assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
//?}
            if (output.isEmpty() && drop.is(net.minecraft.tags.ItemTags.LOGS_THAT_BURN)) output = new ItemStack(Items.CHARCOAL);
            if (output.isEmpty()) { result.add(drop); continue; }
            long amount = (long) drop.getCount() * output.getCount();
            while (amount > 0) {
                int count = (int) Math.min(amount, output.getMaxStackSize());
                result.add(output.copyWithCount(count));
                amount -= count;
            }
        }
        return result;
    }
}
