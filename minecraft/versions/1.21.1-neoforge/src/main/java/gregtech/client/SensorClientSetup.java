package gregtech.client;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class SensorClientSetup {private SensorClientSetup(){}@net.neoforged.bus.api.SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(gregtech.content.registry.GTBlockEntities.SENSOR.get(),SensorRenderer::new);}}
