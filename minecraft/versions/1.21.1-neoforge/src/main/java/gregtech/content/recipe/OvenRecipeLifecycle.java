package gregtech.content.recipe;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** NeoForge lifecycle adapter; the publication sequence itself lives in {@link RecipePublication}. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class OvenRecipeLifecycle {
    static {
        RecipePublication.register((manager, access) -> {
            FormConversionCraftingRecipeInjector.replacePlain(manager, access);
            TrackRecipeReplacement.removeConflicts(manager, access);
        });
    }

    private OvenRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void started(ServerStartedEvent event) {
        RecipePublication.publish(event.getServer());
    }

    // Rail replacements run at NORMAL; the tooltip index runs at LOWEST.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null)
            RecipePublication.publish(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        OvenRecipeBridge.clearMirrors();
        RecipePublication.clear();
    }
}
