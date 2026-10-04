package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.content.recipe.ExternalOreProcessing;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Server reload and remote-client tag packets share the source machine rows and compositions. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class ExternalOreRecipeLifecycle {
    private ExternalOreRecipeLifecycle() {}
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tags(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) ExternalOreProcessing.rebuild();
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ExternalOreProcessing.clear(); }
}
