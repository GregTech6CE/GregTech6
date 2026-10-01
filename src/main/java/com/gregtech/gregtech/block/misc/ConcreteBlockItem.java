package com.gregtech.gregtech.block.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The original coloured metadata item, carried as a vanilla BlockStateTag in 1.20.1. */
public final class ConcreteBlockItem extends BlockItem {
    public ConcreteBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override
    public Component getName(ItemStack stack) {
        var color = ConcreteBlock.itemColor(stack);
        return Component.translatable("color.minecraft." + color.getName()).append(" ")
                .append(Component.translatable(getDescriptionId()));
    }
}
