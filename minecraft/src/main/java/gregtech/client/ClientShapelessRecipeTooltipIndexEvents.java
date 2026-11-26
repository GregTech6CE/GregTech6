package gregtech.client;

import gregtech.content.recipe.ShapelessRecipeTooltipIndex;
import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/** Rebuilds the shapeless tooltip index from the client's synced recipes. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
public final class ClientShapelessRecipeTooltipIndexEvents {
    private ClientShapelessRecipeTooltipIndexEvents() {}

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            ShapelessRecipeTooltipIndex.rebuild(
                    mc.getConnection() != null ? event.getRecipeManager() : null,
                    mc.getConnection() != null ? mc.getConnection().registryAccess() : null);
        }
    }
}
