package com.gregtech.gregtech.item;

import com.gregtech.gregtech.block.BookShelfBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Uses the registered shelf's GT6 plank/metal material name in the inventory and JEI. */
public final class BookShelfBlockItem extends BlockItem {
    public BookShelfBlockItem(BookShelfBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ((BookShelfBlock) getBlock()).variant().displayName();
    }
}
