package com.gregtech.gregtech.platform.neoforge.logistics;
import com.gregtech.gregtech.registry.GTBlockEntities;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class BookShelfCapabilities {@SubscribeEvent public static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,GTBlockEntities.BOOKSHELF.get(),(be,side)->be.inventory());}}
