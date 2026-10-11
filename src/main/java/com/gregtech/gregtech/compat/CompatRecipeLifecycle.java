package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.GregTech;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tag reload owns compat machine rows. The crafting pack reloads through the datapack, not this class. */
@Mod.EventBusSubscriber(modid = GregTech.MODID)
public final class CompatRecipeLifecycle {
    private CompatRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tags(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) CompatRecipes.rebuild();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CompatRecipes.clear(); }
}
