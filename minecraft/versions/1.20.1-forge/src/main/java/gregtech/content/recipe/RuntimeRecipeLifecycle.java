package gregtech.content.recipe;

import gregtech.GregTech;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Runtime recipes are rebuilt after tags bind and before PlayerList sends the recipe packet. */
@Mod.EventBusSubscriber(modid = GregTech.MODID)
public final class RuntimeRecipeLifecycle {
    private static final Map<RecipeManager, Set<Recipe<?>>> APPLIED = new WeakHashMap<>();

    private RuntimeRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void started(ServerStartedEvent event) {
        rebuild(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void syncing(OnDatapackSyncEvent event) {
        rebuild(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        OvenRecipeBridge.clearMirrors();
        APPLIED.clear();
    }

    /** Compare actual recipe identities: reload can reuse the manager; unchanged login sync stays cheap. */
    public static void rebuild(MinecraftServer server) {
        var manager = server.getRecipeManager();
        var current = manager.getRecipes();
        var applied = APPLIED.get(manager);
        if (applied != null && applied.size() == current.size() && applied.containsAll(current)) return;
        var working = new RecipeManager();
        working.replaceRecipes(manager.getRecipes());
        var access = server.registryAccess();
        BottleFillingRecipeInjector.apply(working, access);
        FormConversionCraftingRecipeInjector.apply(working, access);
        HandToolCraftingRecipeInjector.apply(working, access);
        StoneCraftingRecipeInjector.apply(working, access);
        ToolCraftingRecipeInjector.apply(working, access);
        TrackRecipeInjector.apply(working, access);
        WoodCraftingRecipeInjector.apply(working, access);
        BedrockFlowerCraftingRecipeInjector.apply(working, access);
        OvenRecipeBridge.apply(working, access);
        manager.replaceRecipes(working.getRecipes());
        ShapelessRecipeTooltipIndex.rebuild(manager);
        Set<Recipe<?>> snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
        snapshot.addAll(manager.getRecipes());
        APPLIED.put(manager, snapshot);
        GregTech.LOGGER.info(
                "Rebuilt GT runtime recipes before synchronization: {} recipes",
                manager.getRecipes().size());
    }

    /** Input rows precede generated rows. Existing data-pack IDs win instead of throwing duplicates. */
    public static void replaceGenerated(RecipeManager manager, Collection<Recipe<?>> recipes) {
        Map<ResourceLocation, Recipe<?>> unique = new LinkedHashMap<>();
        for (var recipe : recipes) unique.putIfAbsent(recipe.getId(), recipe);
        manager.replaceRecipes(unique.values());
    }
}
