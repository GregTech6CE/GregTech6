package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.loaders.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Runtime recipes are rebuilt after tags bind and before PlayerList sends the recipe packet. */
@Mod.EventBusSubscriber(modid=GregTech.MODID)
public final class RuntimeRecipeLifecycle {
    private static final Set<RecipeManager> APPLIED=Collections.newSetFromMap(new WeakHashMap<>());
    private RuntimeRecipeLifecycle() {}

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void syncing(OnDatapackSyncEvent event) {
        rebuild(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        Loader_OvenRecipes.clearMirrors();
        APPLIED.clear();
    }

    /** A fresh manager on /reload is populated once; login sync does not rerun generators. */
    public static void rebuild(MinecraftServer server) {
        var manager=server.getRecipeManager();
        if(APPLIED.contains(manager)) return;
        var working=new RecipeManager();
        working.replaceRecipes(manager.getRecipes());
        var access=server.registryAccess();
        Loader_BottleFillingRecipes.apply(working,access);
        Loader_FormConversionCraftingRecipes.apply(working,access);
        Loader_HandToolCraftingRecipes.apply(working,access);
        Loader_StoneCraftingRecipes.apply(working,access);
        Loader_ToolCraftingRecipes.apply(working,access);
        Loader_TrackRecipes.apply(working,access);
        Loader_WoodCraftingRecipes.apply(working,access);
        Loader_BedrockFlowerCraftingRecipes.apply(working,access);
        Loader_OvenRecipes.apply(working,access);
        manager.replaceRecipes(working.getRecipes());
        ShapelessRecipeTooltipIndex.rebuild(manager);
        APPLIED.add(manager);
        GregTech.LOGGER.info("Rebuilt GT runtime recipes before synchronization: {} recipes",manager.getRecipes().size());
    }

    /** Input rows precede generated rows. Existing data-pack IDs win instead of throwing duplicates. */
    public static void replaceGenerated(RecipeManager manager,Collection<Recipe<?>> recipes) {
        Map<ResourceLocation,Recipe<?>> unique=new LinkedHashMap<>();
        for(var recipe:recipes) unique.putIfAbsent(recipe.getId(),recipe);
        manager.replaceRecipes(unique.values());
    }
}
