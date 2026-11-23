package gregtech.content.recipe;

import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/** Rebuilds shapeless tooltip index when recipes reload (client + dedicated server). */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class ShapelessRecipeTooltipIndexLoader {
    private ShapelessRecipeTooltipIndexLoader() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShapelessRecipeTooltipIndex.rebuild(event.getServer().getRecipeManager(), event.getServer().registryAccess());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        var server = event.getPlayerList().getServer();
        ShapelessRecipeTooltipIndex.rebuild(server.getRecipeManager(), server.registryAccess());
    }

    @EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() {}

        @SubscribeEvent
        public static void onRecipesUpdated(RecipesUpdatedEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                ShapelessRecipeTooltipIndex.rebuild(
                        mc.getConnection() != null ? event.getRecipeManager() : null,
                        mc.getConnection() != null ? mc.getConnection().registryAccess() : null);
            }
        }
    }
}
