package gregtech.client;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class InventoryClientSetup {
 private InventoryClientSetup(){}
 @net.neoforged.bus.api.SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event){event.register(gregtech.content.registry.GTMenuTypes.METAL_CHEST.get(),gregtech.client.gui.HopperScreen::new);event.register(gregtech.content.registry.GTMenuTypes.STORAGE_15.get(),gregtech.client.gui.HopperScreen::new);event.register(gregtech.content.registry.GTMenuTypes.STORAGE_36.get(),gregtech.client.gui.HopperScreen::new);for(var type:gregtech.content.registry.GTMenuTypes.allHopperTypes())event.register(type.get(),gregtech.client.gui.HopperScreen::new);}
}
