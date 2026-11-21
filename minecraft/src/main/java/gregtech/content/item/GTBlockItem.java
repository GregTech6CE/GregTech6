package gregtech.content.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thin {@link BlockItem} adapter that absorbs 1.20.1 / 1.21.1 tooltip signature differences
 * without holding gameplay fields or state.
 */
public class GTBlockItem extends BlockItem {

    public GTBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        appendTooltip(stack, context.level(), context.registries(), tooltip, flag);
    }
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        appendTooltip(stack, level, null, tooltip, flag);
    }
//?}

    /** Appends block-item tooltip lines when registry lookup access may also be required on 1.21.1. */
    protected void appendTooltip(ItemStack stack, @Nullable Level level,
            @Nullable HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag) {
        appendTooltip(stack, level, tooltip, flag);
    }

    /** Appends block-item tooltip lines across 1.20.1 / 1.21.1. */
    protected void appendTooltip(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
    }
}
