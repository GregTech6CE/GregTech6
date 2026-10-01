package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.GTBars;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.api.distmarker.Dist;import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BarsClientSetup {@SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){for(var b:GTBars.all())e.register((s,l,p,t)->t==0?b.get().tintRgb():0xFFFFFF,b.get());}@SubscribeEvent public static void items(RegisterColorHandlersEvent.Item e){for(var b:GTBars.all())e.register((s,t)->t==0?b.get().tintRgb():0xFFFFFF,b.get().asItem());}}
