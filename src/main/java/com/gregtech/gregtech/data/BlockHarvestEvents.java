package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge hook for GT utility tools, which have no vanilla TieredItem equivalent. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class BlockHarvestEvents {
 @SubscribeEvent public static void harvest(PlayerEvent.HarvestCheck event) {
  var stack=event.getEntity().getMainHandItem();
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
  if(level>3) {
   int actual=stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric?electric.headMaterial(stack).getToolQuality()+(electric.toolName().equals("Chainsaw")?1:0):
     GTToolHelper.isTool(stack)?GTToolHelper.getHarvestLevel(stack):
     stack.getItem() instanceof net.minecraft.world.item.TieredItem tool?tool.getTier().getLevel():-1;
   if(actual<level) event.setCanHarvest(false);
  }
 }
}
