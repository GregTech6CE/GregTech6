package com.gregtech.gregtech.compat;

//? if >=1.21.1 {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
*///?} else {
import com.gregtech.gregtech.GregTech;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Tag reload owns compat machine rows. The crafting pack reloads through the datapack, not this class. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}
public final class CompatRecipeLifecycle {
    private CompatRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tags(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) CompatRecipes.rebuild();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CompatRecipes.clear(); }
}
