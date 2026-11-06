package gregtech.content.recipe;

import gregtech.GregTech;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge lifecycle adapter; the publication sequence itself lives in {@link RecipePublication}. */
@Mod.EventBusSubscriber(modid = GregTech.MODID)
public final class RuntimeRecipeLifecycle {
    static {
        RecipePublication.register((manager, access) -> {
            BottleFillingRecipeInjector.apply(manager, access);
            FormConversionCraftingRecipeInjector.apply(manager, access);
            HandToolCraftingRecipeInjector.apply(manager, access);
            StoneCraftingRecipeInjector.apply(manager, access);
            ToolCraftingRecipeInjector.apply(manager, access);
            TrackRecipeInjector.apply(manager, access);
            WoodCraftingRecipeInjector.apply(manager, access);
            BedrockFlowerCraftingRecipeInjector.apply(manager, access);
        });
    }

    private RuntimeRecipeLifecycle() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void started(ServerStartedEvent event) {
        RecipePublication.publish(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void syncing(OnDatapackSyncEvent event) {
        RecipePublication.publish(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        OvenRecipeBridge.clearMirrors();
        RecipePublication.clear();
    }

    /** Input rows precede generated rows. Existing data-pack IDs win instead of throwing duplicates. */
    public static void replaceGenerated(RecipeManager manager, Collection<Recipe<?>> recipes) {
        Map<ResourceLocation, Recipe<?>> unique = new LinkedHashMap<>();
        for (var recipe : recipes) unique.putIfAbsent(recipe.getId(), recipe);
        manager.replaceRecipes(unique.values());
    }
}
