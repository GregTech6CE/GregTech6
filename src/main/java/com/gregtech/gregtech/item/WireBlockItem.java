package com.gregtech.gregtech.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Uses each registered wire's full source name, preserving material and size grammar. */
public class WireBlockItem extends BlockItem {
    public WireBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
}
