package com.gregtech.gregtech.compat;

//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
*///?} else {

import com.gregtech.gregtech.GregTech;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * Server stop drops compat rows. {@code MachineRecipeLifecycle} rebuilds them on tag reload,
 * after the machine-recipe baseline is restored and before datapack removals.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}

public final class CompatRecipeLifecycle {
    private CompatRecipeLifecycle() {}

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CompatRecipes.clear(); }
}
