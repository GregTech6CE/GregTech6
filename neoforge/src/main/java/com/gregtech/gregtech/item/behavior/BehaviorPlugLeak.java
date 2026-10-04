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

package com.gregtech.gregtech.item.behavior;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
/** Original Behavior_Plug_Leak: place expendable mined blocks adjacent to a liquid. */
public final class BehaviorPlugLeak {
 private BehaviorPlugLeak(){}
 public static InteractionResult use(UseOnContext context){
  var player=context.getPlayer();var level=context.getLevel();var target=context.getClickedPos().relative(context.getClickedFace());
  if(level.isClientSide||player==null||!level.hasChunkAt(target)||!ManualToolBehaviorAccess.mayEdit(level,player,target))return InteractionResult.PASS;
  boolean liquid=!level.getFluidState(target).isEmpty();for(var side:Direction.values())liquid|=!level.getFluidState(target.relative(side)).isEmpty();if(!liquid)return InteractionResult.PASS;
  for(int slot=player.getInventory().items.size()-1;slot>=0;slot--){
   var stock=player.getInventory().items.get(slot);if(!(stock.getItem() instanceof BlockItem blockItem)||stock.isEmpty()||stock.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem)continue;
   var block=blockItem.getBlock();var state=block.defaultBlockState();var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
   if(state.hasBlockEntity()||block instanceof InfestedBlock||!state.canOcclude()||state.getDestroySpeed(level,target)<0||state.is(BlockTags.NEEDS_DIAMOND_TOOL)||id.getNamespace().equals("thaumcraft")||!(state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.is(BlockTags.MINEABLE_WITH_SHOVEL)))continue;
   boolean valuable=false;for(String tag:new String[]{"forge:ores","forge:storage_blocks","c:ores","c:storage_blocks"})valuable|=state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.ResourceLocation.parse(tag)));if(valuable)continue;
   var copy=stock.copy();var hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
   var result=copy.useOn(new UseOnContext(level,player,context.getHand(),copy,hit));
   if(result.consumesAction()&&!player.getAbilities().instabuild)stock.shrink(Math.max(0,stock.getCount()-copy.getCount()));player.getInventory().setChanged();return result;
  }
  return InteractionResult.PASS;
 }
}
