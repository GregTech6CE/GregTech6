package com.gregtech.gregtech.block.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Original BlockColored names use the whole source phrase, including its dye. */
public final class ColoredConstructionBlockItem extends BlockItem {
    public ColoredConstructionBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId() + "." + ColoredConstructionBlock.itemColor(stack).getName());
    }
}
