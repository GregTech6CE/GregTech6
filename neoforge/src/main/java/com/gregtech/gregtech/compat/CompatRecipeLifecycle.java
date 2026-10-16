package com.gregtech.gregtech.compat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Server stop drops compat rows. {@code MachineRecipeLifecycle} rebuilds them on tag reload,
 * after the machine-recipe baseline is restored and before datapack removals.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class CompatRecipeLifecycle {
    private CompatRecipeLifecycle() {}

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CompatRecipes.clear(); }
}
