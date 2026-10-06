package com.gregtech.gregtech.compat;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Tag reload owns compat machine rows. The crafting pack reloads through the datapack, not this class. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class CompatRecipeLifecycle {
    private CompatRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tags(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) CompatRecipes.rebuild();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CompatRecipes.clear(); }
}
