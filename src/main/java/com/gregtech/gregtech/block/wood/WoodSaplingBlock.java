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
    @Override protected boolean mayPlaceOn(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos){
        return super.mayPlaceOn(state,level,pos)||com.gregtech.gregtech.worldgen.TreeSpeciesRules.allowsSand(species)&&state.is(net.minecraft.tags.BlockTags.SAND);
    }
}
