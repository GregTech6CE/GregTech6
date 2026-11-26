package gregtech.event;

import gregtech.content.recipe.ShapelessRecipeTooltipIndex;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/** Rebuilds the shapeless tooltip index when server recipes become available. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class ShapelessRecipeTooltipIndexEvents {
    private ShapelessRecipeTooltipIndexEvents() {}

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
}
