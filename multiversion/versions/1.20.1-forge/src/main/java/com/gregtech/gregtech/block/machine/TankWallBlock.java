package com.gregtech.gregtech.block.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Structural wall for GT6 multiblock tanks (3x3x3 or 5x5x5). */
public class TankWallBlock extends Block {
    public TankWallBlock() {
        super(Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }
}
