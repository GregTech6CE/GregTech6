package gregtech.client;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class InventoryClientSetup {
 private InventoryClientSetup(){}
 @net.neoforged.bus.api.SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event){for(var type:gregtech.registry.GTMenuTypes.allHopperTypes())event.register(type.get(),gregtech.client.gui.HopperScreen::new);}
}
