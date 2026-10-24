package gregtech.client;

//? if >=1.21.1 {
/**///?} else {
import gregtech.GregTech;
//?}
import gregtech.recipe.MachineRecipeOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
//?}

/** Dedicated clients apply the synced markers locally, then let JEI and EMI re-read the maps. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
//?}
public final class MachineRecipeClient {
    private MachineRecipeClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void recipes(RecipesUpdatedEvent event) {
        apply(event.getRecipeManager());
    }

    /** On login the server sends recipes before tags, so tag inputs only expand once the tags arrive. */

/** Tag inputs only expand against the tags the server sent, which can arrive after the recipes. */
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
        refresh("jei", "gregtech.jei.GregTechJEIPlugin");
        refresh("emi", "gregtech.emi.GregTechEMIPlugin");
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
