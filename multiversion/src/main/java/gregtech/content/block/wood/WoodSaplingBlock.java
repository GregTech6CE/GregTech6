package gregtech.content.block.wood;

//? if >=1.21.1 {
/*import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
*///?} else {
//?}
import net.minecraft.world.level.block.SaplingBlock;
//? if >=1.21.1 {
/*import net.minecraft.world.level.block.state.*;
/^* Native two-stage sapling and bonemeal mechanics, growing the same shared GT6 canopy. ^/
*///?} else {
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** GT6 tree sapling. */
//?}
public class WoodSaplingBlock extends SaplingBlock {
//? if >=1.21.1 {
 /*private final WoodSpecies species;
 public WoodSaplingBlock(WoodSpecies species,gregtech.content.worldgen.GTTreeGrower grower,BlockBehaviour.Properties properties){super(new net.minecraft.world.level.block.grower.TreeGrower("gregtech_"+species.id(),java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty()),properties);this.species=species;}
 @Override public com.mojang.serialization.MapCodec<WoodSaplingBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
 public WoodSpecies species(){return species;}
 @Override public void advanceTree(ServerLevel level,BlockPos pos,BlockState state,RandomSource random){
  if(state.getValue(STAGE)==0)level.setBlock(pos,state.cycle(STAGE),4);
  else gregtech.content.worldgen.GTTreeShapes.grow(level,pos,species,random);
 }
*///?} else {
    private final WoodSpecies species;

    public WoodSaplingBlock(WoodSpecies species, AbstractTreeGrower grower, BlockBehaviour.Properties properties) {
        super(grower, properties);
        this.species = species;
    }

    public WoodSpecies species() { return species; }
//?}
    @Override protected boolean mayPlaceOn(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos){
        return super.mayPlaceOn(state,level,pos)||gregtech.content.worldgen.TreeSpeciesRules.allowsSand(species)&&state.is(net.minecraft.tags.BlockTags.SAND);
    }
}
