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

package com.gregtech.gregtech.item;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
/** Original eight pocket identities; switching preserves material, durability and player enchantments. */
public final class PocketToolItem extends GTToolItem {
 public PocketToolItem(Properties properties,GTToolType type){super(properties,type);}
 public static final GTToolType[] MODES={GTToolType.POCKET_MULTITOOL,GTToolType.POCKET_KNIFE,GTToolType.POCKET_SAW,GTToolType.POCKET_FILE,GTToolType.POCKET_SCREWDRIVER,GTToolType.POCKET_WIRE_CUTTER,GTToolType.POCKET_SCISSORS,GTToolType.POCKET_CHISEL};
 public GTToolType activeType(){return switch(toolType()){case POCKET_KNIFE->GTToolType.KNIFE;case POCKET_SAW->GTToolType.SAW;case POCKET_FILE->GTToolType.FILE;case POCKET_SCREWDRIVER->GTToolType.SCREWDRIVER;case POCKET_WIRE_CUTTER->GTToolType.WIRE_CUTTER;case POCKET_SCISSORS->GTToolType.SCISSORS;case POCKET_CHISEL->GTToolType.CHISEL;default->GTToolType.POCKET_MULTITOOL;};}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
  var player=context.getPlayer();var level=context.getLevel();
  if(player!=null&&player.isShiftKeyDown()&&GTToolHelper.isUsable(stack)&&player.mayBuild()){
   var pos=context.getClickedPos();var key=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
   if(!level.mayInteract(player,pos)||level.getBlockEntity(pos)!=null&&!key.getNamespace().equals("gregtech"))return InteractionResult.PASS;
   if(!level.isClientSide){
    int index=java.util.Arrays.asList(MODES).indexOf(toolType());var next=new ItemStack(GTToolItems.get(MODES[(index+1)%MODES.length]));
    next.applyComponents(stack.getComponents());com.gregtech.gregtech.platform.neoforge.StackCustomData.update(next,t->t.remove("gt.material_enchantments.v1"));GTToolEnchantments.apply(next,level.registryAccess());next.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,((GTToolItem)next.getItem()).getDefaultAttributeModifiers(next).withTooltip(false));
    player.setItemInHand(context.getHand(),next);player.getInventory().setChanged();
   }
   return InteractionResult.sidedSuccess(level.isClientSide);
  }
  return super.onItemUseFirst(stack,context);
 }
 @Override public InteractionResult useOn(UseOnContext context){if(activeType()==GTToolType.SAW)return com.gregtech.gregtech.item.behavior.BehaviorPlaceWoodworkingSupplies.use(context);if(activeType()==GTToolType.SCREWDRIVER){var state=context.getLevel().getBlockState(context.getClickedPos());
    if(state.getBlock() instanceof com.gregtech.gregtech.block.machine.HopperBlock||state.getBlock() instanceof com.gregtech.gregtech.block.machine.QueueHopperBlock){var hit=new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());return state.useWithoutItem(context.getLevel(),context.getPlayer(),hit);}}
   return super.useOn(context);}
 @Override public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack,net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.InteractionHand hand){
  if(activeType()!=GTToolType.SCISSORS||!GTToolHelper.isUsable(stack)||player.isSpectator())return net.minecraft.world.InteractionResult.PASS;
  var result=net.minecraft.world.item.Items.SHEARS.interactLivingEntity(stack.copy(),player,entity,hand);
  if(result.consumesAction()&&!player.level().isClientSide)GTToolHelper.damageForUse(stack,20,player);return result;
 }
 public boolean onBlockStartBreak(ItemStack stack,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player){
  var level=player.level();var state=level.getBlockState(pos);
  if(activeType()!=GTToolType.SAW||!GTToolHelper.isUsable(stack)||level.isClientSide||player.getAbilities().instabuild||!player.mayBuild()||!level.mayInteract(player,pos)||state.hasBlockEntity()||state.getDestroySpeed(level,pos)<0)return false;
  if(!state.is(net.minecraft.tags.BlockTags.LEAVES)&&!(state.getBlock() instanceof net.minecraft.world.level.block.IceBlock)&&state.getBlock()!=net.minecraft.world.level.block.Blocks.PACKED_ICE)return false;
  if(!state.onDestroyedByPlayer(level,pos,player,true,state.getFluidState()))return true;
  state.getBlock().destroy(level,pos,state);mineBlock(stack,level,state,pos,player);
  net.minecraft.world.level.block.Block.popResource(level,pos,new ItemStack(state.getBlock()));player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(state.getBlock()));player.causeFoodExhaustion(.005F);level.levelEvent(player,2001,pos,net.minecraft.world.level.block.Block.getId(state));return true;
 }
 private boolean specialHarvest(net.minecraft.world.level.block.state.BlockState state){return switch(activeType()){
  case FILE -> state.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock||state.getBlock() instanceof com.gregtech.gregtech.block.misc.BarsBlock;
  case SCISSORS -> state.is(net.minecraft.tags.BlockTags.WOOL)||state.is(net.minecraft.tags.BlockTags.WOOL_CARPETS)||state.getBlock()==net.minecraft.world.level.block.Blocks.COBWEB||state.getBlock() instanceof net.minecraft.world.level.block.VineBlock;
  case SAW -> state.is(net.minecraft.tags.BlockTags.LEAVES)||state.getBlock() instanceof net.minecraft.world.level.block.VineBlock||state.getBlock() instanceof net.minecraft.world.level.block.IceBlock||state.getBlock()==net.minecraft.world.level.block.Blocks.PACKED_ICE||state.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock;
  default -> false;};}
 @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return GTToolHelper.isUsable(stack)&&specialHarvest(state)?Math.max(Float.MIN_NORMAL,GTToolHelper.getHead(stack).getToolSpeed()*toolType().speedMultiplier()*(activeType()==GTToolType.FILE?3:1)):super.getDestroySpeed(stack,state);}
 @Override public boolean isCorrectToolForDrops(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return GTToolHelper.isUsable(stack)&&specialHarvest(state)||super.isCorrectToolForDrops(stack,state);}
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.pocket.switch"));}
}
