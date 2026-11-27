package gregtech.compat.common;

import gregtech.content.multiblock.ControllerStructureLayouts;
import gregtech.engine.multiblock.tracking.StructurePlan;
import java.util.List;
import net.minecraft.world.level.block.Block;
import gregtech.content.worldgen.OreGenerationSummary;
import gregtech.engine.mod.GregTechIdentity;
import java.util.Map;
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
    private static Build built;
    private static OreGenerationSummary checked;
    private static Runnable refresh;

    private RecipeViewerRefresh() {}

    /** The inputs used by one viewer registration. An older registration cannot replace a newer one. */
    public record Build(long generation, OreGenerationSummary summary, Map<Block, List<StructurePlan.Cell>> layouts) {}

    public static synchronized Build beginBuild() {
        return new Build(++generation, OreGenerationSummary.current(), ControllerStructureLayouts.plans());
    }

    public static synchronized void finishBuild(Build build, Runnable refreshPages) {
        if (build.generation() != generation) return;
        built = build;
        checked = build.summary();
        refresh = refreshPages;
    }

    private static synchronized Runnable pendingRefresh() {
        var latest = OreGenerationSummary.received();
        if (built == null) return latest == null ? null : refresh;
        if (latest != null && latest != checked) {
            if (!latest.equals(built.summary())) return refresh;
            checked = latest;
        }
        return built.layouts() == ControllerStructureLayouts.plans() ? null : refresh;
    }

    @SubscribeEvent
    //? if >=1.21.1 {
    /*public static void tick(ClientTickEvent.Post event) {
    *///?} else {
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    //?}
        if (Minecraft.getInstance().getConnection() == null) return;
        var refreshPages = pendingRefresh();
        if (refreshPages != null) refreshPages.run();
    }

    @SubscribeEvent
    public static synchronized void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        generation++;
        built = null;
        checked = null;
        OreGenerationSummary.acceptClientSummary(null);
    }
}
