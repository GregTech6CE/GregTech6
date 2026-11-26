package gregtech.compat.common;

import gregtech.content.worldgen.OreGenerationSummary;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/** Refreshes stale atlas pages and discards the previous server's data on disconnect. */
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, value = Dist.CLIENT)
public final class OreGenerationClient {
    private static long generation;
    private static Build built;
    private static OreGenerationSummary checked;

    private OreGenerationClient() {}

    /** The snapshot at the start of a native JEI or EMI atlas registration. */
    public record Build(long generation, OreGenerationSummary summary) {}

    public static synchronized Build beginBuild() {
        built = null;
        checked = null;
        return new Build(++generation, OreGenerationSummary.current());
    }

    public static synchronized void finishBuild(Build build) {
        // EMI may finish an asynchronous registration after disconnect or a newer registration.
        if (build.generation() != generation) return;
        built = build;
        checked = null;
    }

    private static synchronized boolean needsRefresh() {
        var latest = OreGenerationSummary.received();
        if (built == null || latest == null || latest == checked) return false;
        if (latest.equals(built.summary()) || latest.equals(checked)) {
            checked = latest;
            return false;
        }
        // EMI registers its pages before its asynchronous recipe index has finished baking.
        if (!RecipeViewerRefresh.ready()) return false;
        checked = latest;
        return true;
    }

    @SubscribeEvent
    //? if >=1.21.1 {
    /*public static void tick(ClientTickEvent.Post event) {
    *///?} else {
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    //?}
        if (Minecraft.getInstance().getConnection() != null && needsRefresh()) RecipeViewerRefresh.refresh();
    }

    @SubscribeEvent
    public static synchronized void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        generation++;
        built = null;
        checked = null;
        OreGenerationSummary.acceptClientSummary(null);
    }
}
