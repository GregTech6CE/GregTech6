package com.gregtech.gregtech.client;
import com.gregtech.gregtech.platform.neoforge.logistics.LogisticsRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
/** Real face-cover renderer on the actual six-way connector. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class LogisticsClientSetup {
    private LogisticsClientSetup() {}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.gregtech.gregtech.registry.GTBlockEntities.LOGISTICS_CORE.get(),LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(com.gregtech.gregtech.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(LogisticsRegistries.LOGISTICS_WIRE.get(),LogisticsCoverRenderer::new);
    }
}
