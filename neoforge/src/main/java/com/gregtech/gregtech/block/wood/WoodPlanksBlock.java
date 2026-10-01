package com.gregtech.gregtech.block.wood;

import net.minecraft.world.level.block.Block;

/** GT6-style tinted planks block. */
public class WoodPlanksBlock extends Block {
    @Override public com.mojang.serialization.MapCodec<WoodPlanksBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private final WoodSpecies species;

    public WoodPlanksBlock(WoodSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public WoodSpecies species() { return species; }
}
