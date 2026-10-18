package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.content.recipe.ExternalOreProcessing;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Server reload and remote-client tag packets share the source machine rows and compositions. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
//?}
public final class ExternalOreRecipeLifecycle {
    private ExternalOreRecipeLifecycle() {}
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tags(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) {
            ExternalOreProcessing.rebuild();
            com.gregtech.gregtech.api.material.MaterialDisplayBinding.invalidate();
        }
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        ExternalOreProcessing.clear();
        com.gregtech.gregtech.api.material.MaterialDisplayBinding.invalidate();
    }
}
