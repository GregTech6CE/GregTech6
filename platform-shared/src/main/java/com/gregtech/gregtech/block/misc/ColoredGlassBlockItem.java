package com.gregtech.gregtech.block.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Item form for the sixteen coloured clear/glow glass variants. */
public final class ColoredGlassBlockItem extends BlockItem {
    public ColoredGlassBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override public Component getName(ItemStack stack) {
        var color = ColoredGlassBlock.itemColor(stack);
        return Component.translatable(getDescriptionId() + "." + color.getName());
    }
}
