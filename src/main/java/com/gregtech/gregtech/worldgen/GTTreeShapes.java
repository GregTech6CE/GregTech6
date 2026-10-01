package com.gregtech.gregtech.worldgen;
import com.gregtech.gregtech.block.wood.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** Minecraft placement boundary for the single shared GT6 canopy implementation. */
public final class GTTreeShapes {
 private GTTreeShapes(){}
 public enum Shape {
  RUBBER,MAPLE,WILLOW,BLUE_MAHOE,HAZEL,CINNAMON,COCONUT,RAINBOWOOD,BLUE_SPRUCE;
  public int minHeight(){return TreeShapeRules.Shape.valueOf(name()).minHeight();}
  public int minTrunk(){return TreeShapeRules.Shape.valueOf(name()).minTrunk();}
 }
 public static Shape shapeOf(WoodSpecies species){return Shape.valueOf(TreeShapeRules.shapeOf(species).name());}
 public static boolean grow(LevelAccessor level,BlockPos pos,WoodSpecies species,RandomSource random){return grow(level,pos,species,shapeOf(species),random);}
 public static boolean grow(LevelAccessor level,BlockPos pos,WoodSpecies species,Shape shape,RandomSource random){
  return TreeShapeRules.grow(world(level),position(pos),species,TreeShapeRules.Shape.valueOf(shape.name()),random::nextInt);
 }
 public static int maxHeight(LevelAccessor level,BlockPos pos,int limit){return TreeShapeRules.maxHeight(world(level),position(pos),limit);}
 public static boolean canPlace(LevelAccessor level,BlockPos pos){
  if(level.isOutsideBuildHeight(pos))return false;
  var state=level.getBlockState(pos);
  return state.isAir()||state.is(BlockTags.LEAVES)||state.getBlock() instanceof WoodSaplingBlock||state.canBeReplaced()&&!state.liquid();
 }
 private static TreeShapeRules.Position position(BlockPos p){return new TreeShapeRules.Position(p.getX(),p.getY(),p.getZ());}
 private static BlockPos block(TreeShapeRules.Position p){return new BlockPos(p.x(),p.y(),p.z());}
 private static void place(LevelAccessor level,BlockPos pos,BlockState state,boolean force){
  if(!level.isOutsideBuildHeight(pos)&&(force||canPlace(level,pos)))level.setBlock(pos,state,3);
 }
 private static TreeShapeRules.TreeWorld world(LevelAccessor level){return new TreeShapeRules.TreeWorld(){
  public boolean isOutsideBuildHeight(TreeShapeRules.Position p){return level.isOutsideBuildHeight(block(p));}
  public int getMaxBuildHeight(){return level.getMaxBuildHeight();}
  public boolean canPlace(TreeShapeRules.Position p){return GTTreeShapes.canPlace(level,block(p));}
  public void setLog(TreeShapeRules.Position p,WoodSpecies s,boolean force){place(level,block(p),GTWoods.log(s).defaultBlockState(),force);}
  public void setLeaves(TreeShapeRules.Position p,WoodSpecies s){place(level,block(p),GTWoods.leaves(s).defaultBlockState(),false);}
  public boolean setResinHole(TreeShapeRules.Position p,int face){
   var hole=GTTreeHoles.hole(WoodSpecies.RUBBER);if(hole==null)return false;
   var sides=Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
   return level.setBlock(block(p),hole.defaultBlockState().setValue(TreeHoleBlock.FACING,sides[face]),3);
  }
  public boolean isDirtOrGrass(TreeShapeRules.Position p){var state=level.getBlockState(block(p));return state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK);}
  public void setPodzol(TreeShapeRules.Position p){level.setBlock(block(p),Blocks.PODZOL.defaultBlockState(),3);}
 };}
 public static boolean isGtLeaves(Block block){return GTWoods.leavesOf(block)!=null;}
}
