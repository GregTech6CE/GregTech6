package gregtech.client;

import gregtech.content.worldgen.OreGenerationSummary;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Refreshes stale atlas pages and discards the previous server's data on disconnect. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, value = Dist.CLIENT)
//?}
public final class OreGenerationClient {
    private static long generation;
    private static Build built;
    private static OreGenerationSummary requested;
    private static boolean dirty;

    private OreGenerationClient() {}

    /** The snapshot at the start of a native JEI or EMI atlas registration. */
    public record Build(long generation, OreGenerationSummary summary) {}

    public static synchronized Build beginBuild() {
        built = null;
        requested = null;
        return new Build(++generation, OreGenerationSummary.current());
    }

    public static synchronized void finishBuild(Build build) {
        // EMI may finish an asynchronous registration after disconnect or a newer registration.
        if (build.generation() != generation) return;
        built = build;
        dirty = true;
    }

    public static synchronized void accept(OreGenerationSummary summary) {
        OreGenerationSummary.acceptClientSummary(summary);
        dirty = true;
    }

    private static synchronized boolean needsRefresh() {
        if (!dirty || built == null) return false;
        var current = OreGenerationSummary.current();
        if (current.equals(built.summary()) || current.equals(requested)) {
            dirty = false;
            return false;
        }
        // EMI registers these pages before its asynchronous recipe index has finished baking.
        if (!RecipeViewerRefresh.ready()) return false;
        dirty = false;
        requested = current;
        return true;
    }

    @SubscribeEvent
    //? if >=1.21.1 {
    /*public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
    *///?} else {
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
    //?}
        if (Minecraft.getInstance().getConnection() != null && needsRefresh()) RecipeViewerRefresh.refresh();
    }

    @SubscribeEvent
    public static synchronized void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        generation++;
        built = null;
        requested = null;
        dirty = false;
        OreGenerationSummary.acceptClientSummary(null);
    }
}
