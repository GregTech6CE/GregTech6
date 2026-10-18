package com.gregtech.gregtech.client;
import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.api.distmarker.Dist;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PoweredToolClient {
 private PoweredToolClient(){}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){for(var tool:com.gregtech.gregtech.registry.GTElectricItems.tools())event.register(ItemColorARGB.opaque((stack,layer)->tool.get().tint(stack,layer)),tool.get());for(var holder:com.gregtech.gregtech.registry.GTToolBlocks.explosives())event.register(ItemColorARGB.opaque((stack,layer)->holder.get().tintRgb()),holder.get().asItem());}
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){for(var holder:com.gregtech.gregtech.registry.GTToolBlocks.explosives())event.register((state,level,pos,layer)->holder.get().tintRgb(),holder.get());}
 @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(com.gregtech.gregtech.registry.GTBlockEntities.PORTABLE_CONTAINER.get(),CapsuleCellRenderer::new);}
}
