package com.gregtech.gregtech.registry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class LegacyItemMappings {
 @SubscribeEvent public static void missing(MissingMappingsEvent event) {
  for(var mapping:event.<Item>getMappings(Registries.ITEM,"gregtech")) {
   String old=mapping.getKey().getPath();
   if(old.startsWith("compact__electric_conveyor_")) {
    var target=GTTechnological.get(old);
    if(target!=null)mapping.remap(target);
   }
  }
 }
}
