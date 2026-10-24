package gregtech.client;

import gregtech.platform.neoforge.logistics.LogisticsRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Registers shared logistics face rendering for the core, bound ports and wire. */
@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class LogisticsClientSetup {

    private LogisticsClientSetup() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(gregtech.registry.GTBlockEntities.LOGISTICS_CORE.get(),
                LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(gregtech.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),
                LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(LogisticsRegistries.LOGISTICS_WIRE.get(), LogisticsCoverRenderer::new);
    }
}
