package gregtech.client;

import gregtech.registry.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoinClientSetup {
    @SubscribeEvent
    public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GTBlockEntities.COIN_PILE.get(), CoinPileRenderer::new);
    }

    @SubscribeEvent
    public static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        var coins = GTItems.allEntries().stream()
                .map(item -> item.get())
                .filter(item -> item instanceof gregtech.item.CoinItem)
                .toArray(net.minecraft.world.item.Item[]::new);
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return CoinItemRenderer.instance();
            }
        }, coins);
    }
}
