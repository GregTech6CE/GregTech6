package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.GregTech;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rebuilds shapeless tooltip index when recipes reload (client + dedicated server). */
@Mod.EventBusSubscriber(modid = GregTech.MODID)
public final class ShapelessRecipeTooltipIndexLoader {
    private ShapelessRecipeTooltipIndexLoader() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShapelessRecipeTooltipIndex.rebuild(event.getServer().getRecipeManager());
    }

    @Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
    public static final class Client {
        private Client() {}

        @SubscribeEvent
        public static void onRecipesUpdated(RecipesUpdatedEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                ShapelessRecipeTooltipIndex.rebuild(mc.getConnection() != null
                        ? mc.getConnection().getRecipeManager()
                        : null);
            }
        }
    }
}
