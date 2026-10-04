/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** Original MiningDrill, BuzzSaw and Trimmer target/quality rules. */
public final class ElectricUtilityHarvest {
 private ElectricUtilityHarvest(){}
 public static boolean target(ElectricToolItem tool,BlockState state){return switch(tool.toolName()){
  case "Mining Drill" -> state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.is(BlockTags.MINEABLE_WITH_SHOVEL)||state.getBlock() instanceof HalfTransparentBlock||state.getBlock() instanceof IceBlock||state.getBlock()==Blocks.PACKED_ICE||state.getBlock() instanceof FlowerPotBlock;
  case "BuzzSaw" -> state.getBlock()==Blocks.IRON_BARS||state.getBlock() instanceof com.gregtech.gregtech.block.misc.BarsBlock||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("carpentersblocks");
  case "Trimmer" -> state.is(BlockTags.LEAVES)||state.getBlock() instanceof VineBlock;
  default -> false;};}
 public static boolean canHarvest(ElectricToolItem tool,ItemStack stack,BlockState state){return tool.isPoweredUsable(stack)&&target(tool,state)&&tool.headMaterial(stack).getToolQuality()+tool.definition().quality()>=ElectricWrenchHarvest.requiredQuality(state);}
 public static float speed(ElectricToolItem tool,ItemStack stack){return Math.max(Float.MIN_NORMAL,tool.definition().speed()*tool.headMaterial(stack).getToolSpeed());}
 public static boolean trim(ElectricToolItem tool,ItemStack stack,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player){
  if(!tool.toolName().equals("Trimmer")||!canHarvest(tool,stack,player.level().getBlockState(pos))||player.getAbilities().instabuild||!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return false;
  var state=level.getBlockState(pos);if(state.hasBlockEntity()||state.getDestroySpeed(level,pos)<0||!player.mayBuild()||!level.mayInteract(player,pos))return false;
  var key=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());String path=key.getPath();
  String sapling=path.endsWith("_leaves")?path.substring(0,path.length()-7)+"_sapling":path.startsWith("leaves_")?"sapling_"+path.substring(7):null;
  var item=sapling==null?net.minecraft.world.item.Items.AIR:net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(key.getNamespace(),sapling));
  ItemStack drop=state.getBlock() instanceof VineBlock?new ItemStack(Blocks.VINE):new ItemStack(item);
  if(drop.isEmpty())return false; // Unknown external leaves retain their own drop event.
  if(state.is(Blocks.OAK_LEAVES)&&level.random.nextInt(9)==0)drop=new ItemStack(net.minecraft.world.item.Items.APPLE);
  if(!state.onDestroyedByPlayer(level,pos,player,true,state.getFluidState()))return true;
  state.getBlock().destroy(level,pos,state);tool.mineBlock(stack,level,state,pos,player);Block.popResource(level,pos,drop);
  player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(state.getBlock()));player.causeFoodExhaustion(.005F);level.levelEvent(player,2001,pos,Block.getId(state));return true;
 }
}
