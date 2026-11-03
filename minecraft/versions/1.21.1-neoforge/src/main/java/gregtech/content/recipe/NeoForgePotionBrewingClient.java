package gregtech.content.recipe;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Client-side probe after the connection has built its brewing table. */
@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        value = Dist.CLIENT)
public final class NeoForgePotionBrewingClient {
    private NeoForgePotionBrewingClient() {}

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) NeoForgePotionBrewing.probe(connection.potionBrewing());
    }
}
