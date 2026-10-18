package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.GTBumbleBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BumbleClientSetup {
 private BumbleClientSetup(){}
 @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(com.gregtech.gregtech.registry.GTMenuTypes.BUMBLIARY.get(),com.gregtech.gregtech.client.gui.BumbliaryScreen::new);event.register(com.gregtech.gregtech.registry.GTMenuTypes.ADVANCED_BUMBLIARY.get(),com.gregtech.gregtech.client.gui.BumbliaryScreen::new);}
 @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event){for(var holder:java.util.List.of(GTBumbleBlocks.BUMBLIARY,GTBumbleBlocks.ADVANCED_BUMBLIARY))event.register((state,level,pos,index)->index==0?holder.get().tintRgb():0xFFFFFF,holder.get());}
 @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item event){for(var holder:java.util.List.of(GTBumbleBlocks.BUMBLIARY,GTBumbleBlocks.ADVANCED_BUMBLIARY))event.register(ItemColorARGB.opaque((stack,index)->index==0?holder.get().tintRgb():0xFFFFFF),holder.get().asItem());}
}
