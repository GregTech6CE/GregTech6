package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.GregTech;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Applies the machine-recipe overlay after tags bind on the logical server. */
@Mod.EventBusSubscriber(modid = GregTech.MODID)
public final class MachineRecipeLifecycle {
    private MachineRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void tags(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        MachineRecipeOverlay.apply(server.getRecipeManager());
    }

    /**
     * Integrated startup binds tags before {@code getCurrentServer()} is set, so the handler above
     * skips that pass. A saved world's markers still have to be on the maps before the first player
     * syncs. {@code /reload} does not fire this event; the tag handler covers it.
     * <p>Runs before the HIGH oven mirror: the first apply captures the baseline and its ore rebuild
     * clears oven mirrors, so running later would freeze mirrored rows into the baseline and drop them
     * from the live furnace map until the next reload.</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void started(ServerStartedEvent event) {
        MachineRecipeOverlay.apply(event.getServer().getRecipeManager());
    }
}
