package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.content.recipe.ExternalOreProcessing;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Server stop drops tag-owned ore rows. {@code MachineRecipeLifecycle} rebuilds them after the
 * machine-recipe baseline is restored, so a datapack reload cannot freeze the previous tag expansion.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class ExternalOreRecipeLifecycle {
    private ExternalOreRecipeLifecycle() {}
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        ExternalOreProcessing.clear();
        com.gregtech.gregtech.api.material.MaterialDisplayBinding.invalidate();
    }
}
