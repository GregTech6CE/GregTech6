package com.gregtech.gregtech.api.tool;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Platform contract for actual powered tool items, preserving manual dispatch semantics. */
public interface PoweredToolItem {
 GTToolType interactionType(ItemStack stack);
 boolean canInteract(ItemStack stack);
 void consumeInteractionEnergy(ItemStack stack,long amount,LivingEntity user);
 boolean canHarvest(ItemStack stack,BlockState state);
 float harvestSpeed(ItemStack stack);
}
