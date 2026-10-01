package com.gregtech.gregtech.worldgen;
import com.gregtech.gregtech.block.wood.WoodSpecies;
/** Thin native world/sapling entry point; canopy generation is shared with Forge. */
public final class GTTreeGrower {
 private final WoodSpecies species;
 public GTTreeGrower(WoodSpecies species){this.species=species;}
 public WoodSpecies species(){return species;}
 public boolean grow(net.minecraft.world.level.LevelAccessor level,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource random){return GTTreeShapes.grow(level,pos,species,random);}
}
