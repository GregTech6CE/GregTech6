package gregtech.content.recipe;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Rebuilds shapeless tooltip index when recipes reload (client + dedicated server). */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}
public final class ShapelessRecipeTooltipIndexLoader {
    private ShapelessRecipeTooltipIndexLoader() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
//? if >=1.21.1 {
        /*ShapelessRecipeTooltipIndex.rebuild(event.getServer().getRecipeManager(),event.getServer().registryAccess());
*///?} else {
        ShapelessRecipeTooltipIndex.rebuild(event.getServer().getRecipeManager());
//?}
    }

//? if >=1.21.1 {
    /*@SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        var server = event.getPlayerList().getServer();
        ShapelessRecipeTooltipIndex.rebuild(server.getRecipeManager(), server.registryAccess());
    }

    @EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
    @Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
//?}
    public static final class Client {
        private Client() {}

        @SubscribeEvent
        public static void onRecipesUpdated(RecipesUpdatedEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
//? if >=1.21.1 {
                /*ShapelessRecipeTooltipIndex.rebuild(mc.getConnection()!=null?event.getRecipeManager():null,
                        mc.getConnection()!=null?mc.getConnection().registryAccess():null);
*///?} else {
                ShapelessRecipeTooltipIndex.rebuild(mc.getConnection() != null
                        ? mc.getConnection().getRecipeManager()
                        : null);
//?}
            }
        }
    }
}
