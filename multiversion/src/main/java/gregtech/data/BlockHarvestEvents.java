package gregtech.data;

import gregtech.internal.tool.GTToolHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Forge hook for GT utility tools, which have no vanilla TieredItem equivalent. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
//?}
public final class BlockHarvestEvents {
 @SubscribeEvent public static void harvest(PlayerEvent.HarvestCheck event) {
  var oreDecision=OreHostHarvest.decision(event.getEntity().getMainHandItem(),event.getTargetBlock());
  if(oreDecision.isPresent()){event.setCanHarvest(oreDecision.get());return;}
  // A Spike family has two material tiers in native state; a single vanilla block tag cannot encode them.
  if (event.getTargetBlock().getBlock() instanceof gregtech.block.misc.SpikeBlock) {
   var stack = event.getEntity().getMainHandItem();
   Integer actual = null;
   // Concrete GT definitions expose material quality; interface-only addons do not.
   if (stack.getItem() instanceof gregtech.item.ElectricToolItem electric) {
    actual = electric.headMaterial(stack).getToolQuality() + electric.definition().quality();
   } else if (GTToolHelper.isTool(stack)) {
    actual = GTToolHelper.getHarvestLevel(stack);
   } else if (stack.getItem() instanceof net.minecraft.world.item.TieredItem tool
       && tool.getTier() instanceof net.minecraft.world.item.Tiers tier) {
    actual = switch (tier) {
     case WOOD, GOLD -> 0;
     case STONE -> 1;
     case IRON -> 2;
     case DIAMOND -> 3;
     case NETHERITE -> 4;
    };
   }
   if (actual != null) {
    event.setCanHarvest(stack.isCorrectToolForDrops(event.getTargetBlock())
        && actual >= SpikeHarvestPolicy.level(event.getTargetBlock()));
    return;
   }
   // Unknown tiers/boolean-only adapters retain the complete preexisting handler below.
   // Their numeric source-quality parity is unknown, not a reason for a new denial.
  }
  var original=BlockHarvestPolicy.source(event.getTargetBlock().getBlock());
  if(original.isPresent() && original.get().handHarvestable()){event.setCanHarvest(true);return;}
  var stack=event.getEntity().getMainHandItem();
  // Some port constructors lacked requiresCorrectToolForDrops. Source groups still require
  // their matching tool; a permissive native constructor must not bypass that contract.
  if(original.isPresent()) event.setCanHarvest(stack.isCorrectToolForDrops(event.getTargetBlock()));
  if(GTToolHelper.isSpecialHarvestTool(stack,event.getTargetBlock())) event.setCanHarvest(true);
  if(stack.getItem() instanceof gregtech.item.ElectricToolItem electric
      && gregtech.content.tool.ElectricWrenchHarvest.target(event.getTargetBlock()))
   event.setCanHarvest(gregtech.content.tool.ElectricWrenchHarvest.canHarvest(electric,stack,event.getTargetBlock()));
  if(stack.getItem() instanceof gregtech.item.ElectricToolItem electric
      && electric.toolName().equals("Chainsaw")
      && gregtech.content.tool.ElectricChainsawHarvest.target(event.getTargetBlock()))
   event.setCanHarvest(gregtech.content.tool.ElectricChainsawHarvest.canHarvest(electric,stack,event.getTargetBlock()));
  int level=BlockHarvestPolicy.level(event.getTargetBlock().getBlock());
  // Vanilla only supplies three tier tags; GT materials can require a higher tier.
  if(level>3 || original.isPresent()) {
   int actual=stack.getItem() instanceof gregtech.item.ElectricToolItem electric?electric.headMaterial(stack).getToolQuality()+(electric.toolName().equals("Chainsaw")?1:0):
     GTToolHelper.isTool(stack)?GTToolHelper.getHarvestLevel(stack):
//? if >=1.21.1 {
     /*stack.getItem() instanceof net.minecraft.world.item.TieredItem tool?vanillaLevel(tool.getTier()):-1;
*///?} else {
     stack.getItem() instanceof net.minecraft.world.item.TieredItem tool?tool.getTier().getLevel():-1;
//?}
   if(actual<level && (actual>=0 || level>3)) event.setCanHarvest(false);
  }
 }
//? if >=1.21.1 {
 /*private static int vanillaLevel(net.minecraft.world.item.Tier tier){return tier==net.minecraft.world.item.Tiers.NETHERITE?4:tier==net.minecraft.world.item.Tiers.DIAMOND?3:tier==net.minecraft.world.item.Tiers.IRON?2:tier==net.minecraft.world.item.Tiers.STONE?1:tier==net.minecraft.world.item.Tiers.WOOD||tier==net.minecraft.world.item.Tiers.GOLD?0:-1;}

*///?} else {
//?}
}
