package com.gregtech.gregtech.client;
@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class SensorClientSetup {private SensorClientSetup(){}@net.neoforged.bus.api.SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(com.gregtech.gregtech.registry.GTBlockEntities.SENSOR.get(),SensorRenderer::new);}}
