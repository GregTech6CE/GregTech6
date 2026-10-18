package com.gregtech.gregtech.client;
import com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries;
import com.gregtech.gregtech.client.gui.BasicMachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BasicMachineClientSetup {
 private BasicMachineClientSetup(){}
 @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(BasicMachineRegistries.MENU.get(),BasicMachineScreen::new);}
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){for(var holder:BasicMachineRegistries.all()){var block=holder.get();event.register((state,level,pos,index)->index==0?block.basicSpec().material().getColor():0xFFFFFF,block);}}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){for(var holder:BasicMachineRegistries.all()){var block=holder.get();event.register(ItemColorARGB.opaque((stack,index)->index==0?block.basicSpec().material().getColor():0xFFFFFF),block.asItem());}}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){for(var type:BasicMachineRegistries.types())event.registerBlockEntityRenderer(type.get(),MachineCoverRenderer::new);event.registerBlockEntityRenderer(com.gregtech.gregtech.registry.GTBlockEntities.COKE_OVEN.get(),MachineCoverRenderer::new);}
}
