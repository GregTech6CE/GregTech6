package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.GregTech;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server stop drops compat rows. {@code MachineRecipeLifecycle} rebuilds them on tag reload,
 * after the machine-recipe baseline is restored and before datapack removals.
 */
@Mod.EventBusSubscriber(modid = GregTech.MODID)
public final class CompatRecipeLifecycle {
    private CompatRecipeLifecycle() {}

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CompatRecipes.clear(); }
}
