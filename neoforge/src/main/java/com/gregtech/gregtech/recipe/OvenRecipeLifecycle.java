package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.loaders.Loader_OvenRecipes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Original furnace/oven bridge over the actual loaded Native recipes, including datapack reload. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class OvenRecipeLifecycle {
    private OvenRecipeLifecycle() {}
    private static final Map<RecipeManager, Set<RecipeHolder<?>>> APPLIED = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }

    // Rail replacements run at NORMAL; the tooltip index runs at LOWEST.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) rebuild(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        Loader_OvenRecipes.clearMirrors(); APPLIED.clear();
    }

    private static void rebuild(MinecraftServer server) {
        var manager = server.getRecipeManager(); var current = manager.getRecipes(); var applied = APPLIED.get(manager);
        // The same RecipeManager may be reused on reload. Compare actual holder identities, not manager identity.
        if (applied != null && applied.size() == current.size() && applied.containsAll(current)) return;
        com.gregtech.gregtech.loaders.Loader_FormConversionCraftingRecipes.replacePlain(manager, server.registryAccess());
        Loader_OvenRecipes.apply(manager, server.registryAccess());
        Set<RecipeHolder<?>> snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
        snapshot.addAll(manager.getRecipes()); APPLIED.put(manager, snapshot);
    }
}
