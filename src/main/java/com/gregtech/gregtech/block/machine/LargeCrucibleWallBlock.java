package com.gregtech.gregtech.block.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Structural wall for the GT6 Large Crucible multiblock (3x3x3). */
public class LargeCrucibleWallBlock extends Block {
    public LargeCrucibleWallBlock() {
        super(Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    }
}
