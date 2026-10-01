package com.gregtech.gregtech.block.wood;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** GT6 tree sapling. */
public class WoodSaplingBlock extends SaplingBlock {
    private final WoodSpecies species;

    public WoodSaplingBlock(WoodSpecies species, AbstractTreeGrower grower, BlockBehaviour.Properties properties) {
        super(grower, properties);
        this.species = species;
    }

    public WoodSpecies species() { return species; }
}
