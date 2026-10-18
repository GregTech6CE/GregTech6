package com.gregtech.gregtech.client;
@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class EnergyNodeClientSetup {
 private EnergyNodeClientSetup(){}
 @net.neoforged.bus.api.SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(com.gregtech.gregtech.registry.GTEnergyNodes.ENERGY_NODE.get(),EnergyNodeRenderer::new);}
 @net.neoforged.bus.api.SubscribeEvent public static void blocks(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event){for(var entry:com.gregtech.gregtech.registry.GTEnergyNodes.all()){var block=entry.get();event.register((state,level,pos,layer)->layer==0?block.spec().material().getColor():0xFFFFFF,block);}}
 @net.neoforged.bus.api.SubscribeEvent public static void items(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){for(var entry:com.gregtech.gregtech.registry.GTEnergyNodes.all()){var block=entry.get();event.register(ItemColorARGB.opaque((stack,layer)->layer==0?block.spec().material().getColor():0xFFFFFF),block.asItem());}}
}
