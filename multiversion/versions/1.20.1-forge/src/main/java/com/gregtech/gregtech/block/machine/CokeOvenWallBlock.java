package com.gregtech.gregtech.block.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Structural brick for the Coke Oven multiblock. Inert casing block. */
public class CokeOvenWallBlock extends Block {
    public CokeOvenWallBlock() {
        super(Properties.of().strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    }
}
