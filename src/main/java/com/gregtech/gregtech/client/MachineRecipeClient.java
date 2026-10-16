package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Dedicated clients apply the synced markers locally, then let JEI and EMI re-read the maps. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
public final class MachineRecipeClient {
    private MachineRecipeClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void recipes(RecipesUpdatedEvent event) {
        apply(event.getRecipeManager());
    }

    /** On login the server sends recipes before tags, so tag inputs only expand once the tags arrive. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) return;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) apply(connection.getRecipeManager());
    }

    private static void apply(RecipeManager recipes) {
        // Integrated server and client share the static maps. The server pass already applied them.
        if (ServerLifecycleHooks.getCurrentServer() != null) return;
        MachineRecipeOverlay.apply(recipes);
        refresh("jei", "com.gregtech.gregtech.jei.GregTechJEIPlugin");
        refresh("emi", "com.gregtech.gregtech.emi.GregTechEMIPlugin");
    }

    private static void refresh(String modId, String plugin) {
        if (!ModList.get().isLoaded(modId)) return;
        try {
            Class.forName(plugin).getMethod("overlayChanged").invoke(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not refresh " + modId + " machine recipes", failure);
        }
    }
}