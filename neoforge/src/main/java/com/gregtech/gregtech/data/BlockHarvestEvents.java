package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Forge hook for GT utility tools, which have no vanilla TieredItem equivalent. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class BlockHarvestEvents {
 @SubscribeEvent public static void harvest(PlayerEvent.HarvestCheck event) {
  var original=BlockHarvestPolicy.source(event.getTargetBlock().getBlock());
  if(original.isPresent() && original.get().handHarvestable()){event.setCanHarvest(true);return;}
  var stack=event.getEntity().getMainHandItem();
  // Some port constructors lacked requiresCorrectToolForDrops. Source groups still require
  // their matching tool; a permissive native constructor must not bypass that contract.
  if(original.isPresent()) event.setCanHarvest(stack.isCorrectToolForDrops(event.getTargetBlock()));
  if(GTToolHelper.isSpecialHarvestTool(stack,event.getTargetBlock())) event.setCanHarvest(true);
  if(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric
      && com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.target(event.getTargetBlock()))
   event.setCanHarvest(com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.canHarvest(electric,stack,event.getTargetBlock()));
  if(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric
      && electric.toolName().equals("Chainsaw")
      && com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.target(event.getTargetBlock()))
   event.setCanHarvest(com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.canHarvest(electric,stack,event.getTargetBlock()));
  int level=BlockHarvestPolicy.level(event.getTargetBlock().getBlock());
  // Vanilla only supplies three tier tags; GT materials can require a higher tier.
  if(level>3 || original.isPresent()) {
   int actual=stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric?electric.headMaterial(stack).getToolQuality()+(electric.toolName().equals("Chainsaw")?1:0):
     GTToolHelper.isTool(stack)?GTToolHelper.getHarvestLevel(stack):
     stack.getItem() instanceof net.minecraft.world.item.TieredItem tool?vanillaLevel(tool.getTier()):-1;
   if(actual<level && (actual>=0 || level>3)) event.setCanHarvest(false);
  }
 }
 private static int vanillaLevel(net.minecraft.world.item.Tier tier){return tier==net.minecraft.world.item.Tiers.NETHERITE?4:tier==net.minecraft.world.item.Tiers.DIAMOND?3:tier==net.minecraft.world.item.Tiers.IRON?2:tier==net.minecraft.world.item.Tiers.STONE?1:tier==net.minecraft.world.item.Tiers.WOOD||tier==net.minecraft.world.item.Tiers.GOLD?0:-1;}

}
