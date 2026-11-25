package gregtech.content.worldgen;

import gregtech.engine.mod.GregTechIdentity;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
*///?} else {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Advances pending worldgen exports and releases server-owned generation state on shutdown. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
//?}
public final class WorldgenLifecycle {
    private WorldgenLifecycle() {}

    @SubscribeEvent
    //? if >=1.21.1 {
    /*public static void tick(ServerTickEvent.Post event) {
    *///?} else {
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    //?}
        WorldgenExport.tick(event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        WorldgenExport.stop(event.getServer());
        VeinTerrain.clear();
    }
}
