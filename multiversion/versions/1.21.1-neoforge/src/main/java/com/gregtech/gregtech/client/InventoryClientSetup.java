package com.gregtech.gregtech.client;
@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class InventoryClientSetup {
 private InventoryClientSetup(){}
 @net.neoforged.bus.api.SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event){for(var type:com.gregtech.gregtech.registry.GTMenuTypes.allHopperTypes())event.register(type.get(),com.gregtech.gregtech.client.gui.HopperScreen::new);}
}
