package com.gregtech.gregtech.loaders;
import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.event.OnDatapackSyncEvent;import net.minecraft.world.item.Items;import net.minecraft.world.item.crafting.ShapedRecipe;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class TrackRecipeReplacement {private TrackRecipeReplacement(){}
 @SubscribeEvent public static void sync(OnDatapackSyncEvent e){var server=e.getPlayerList().getServer();var manager=server.getRecipeManager();var replaced=java.util.Set.of(Items.RAIL,Items.POWERED_RAIL,Items.DETECTOR_RAIL,Items.ACTIVATOR_RAIL);
  var rows=manager.getRecipes();var kept=rows.stream().filter(holder->holder.id().getNamespace().equals("gregtech")||!(holder.value() instanceof ShapedRecipe)||!replaced.contains(holder.value().getResultItem(server.registryAccess()).getItem())).toList();
  if(kept.size()!=rows.size()){manager.replaceRecipes(kept);com.mojang.logging.LogUtils.getLogger().info("[gregtech] Replaced {} conflicting shaped rail recipes",rows.size()-kept.size());}
 }
}
