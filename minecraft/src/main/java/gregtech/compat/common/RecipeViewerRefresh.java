package gregtech.compat.common;

import gregtech.content.worldgen.OreGenerationSummary;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.platform.LoaderHooks;
import gregtech.compat.emi.GregTechEMIPlugin;
import gregtech.compat.jei.GregTechJEIPlugin;
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
public final class RecipeViewerRefresh {
    private static long generation;
    private static boolean structuresChanged;
    private static Build built;
    private static OreGenerationSummary checked;

    private RecipeViewerRefresh() {}

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

    public static void invalidateStructures() {
        structuresChanged = true;
    }

    private static synchronized boolean staleAtlas() {
        var latest = OreGenerationSummary.received();
        if (built == null || latest == null || latest == checked) return false;
        if (latest.equals(built.summary())) {
            checked = latest;
            return false;
        }
        return true;
    }

    @SubscribeEvent
    //? if >=1.21.1 {
    /*public static void tick(ClientTickEvent.Post event) {
    *///?} else {
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    //?}
        if (Minecraft.getInstance().getConnection() == null) return;
        boolean atlas = staleAtlas();
        if (!atlas && !structuresChanged) return;
        if (LoaderHooks.isModLoaded("emi")) {
            if (!GregTechEMIPlugin.readyToReload()) return;
            GregTechEMIPlugin.reload();
        } else if (LoaderHooks.isModLoaded("jei")) {
            if (!GregTechJEIPlugin.refreshPages(atlas, structuresChanged)) return;
        }
        synchronized (RecipeViewerRefresh.class) {
            checked = OreGenerationSummary.received();
        }
        structuresChanged = false;
    }

    @SubscribeEvent
    public static synchronized void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        generation++;
        built = null;
        checked = null;
        structuresChanged = false;
        OreGenerationSummary.acceptClientSummary(null);
    }
}
