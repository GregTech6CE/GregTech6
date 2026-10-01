package com.gregtech.gregtech.block.energy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Structural casing for reactor multiblocks. Simple inert block. */
public class ReactorCasingBlock extends Block {
    public ReactorCasingBlock() {
        super(Properties.of()
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
