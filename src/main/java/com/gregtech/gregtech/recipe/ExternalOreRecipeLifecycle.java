package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.content.recipe.ExternalOreProcessing;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server reload and remote-client tag packets share the source machine rows and compositions. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class ExternalOreRecipeLifecycle {
    private ExternalOreRecipeLifecycle() {}
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tags(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) ExternalOreProcessing.rebuild();
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ExternalOreProcessing.clear(); }
}
