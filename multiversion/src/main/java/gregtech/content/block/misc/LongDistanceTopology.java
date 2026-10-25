package gregtech.content.block.misc;

import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.ChunkEvent;
//?}

/** Routing changes only; active-state animation must not invalidate a cached connection. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
//?}
public final class LongDistanceTopology {
    private static final java.util.Map<Level, Long> VERSIONS = new java.util.WeakHashMap<>();

    private LongDistanceTopology() {}

    static long version(Level level) {
        return VERSIONS.getOrDefault(level, 0L);
    }

    static void changed(Level level) {
        if (!level.isClientSide) VERSIONS.put(level, version(level) + 1);
    }

    /** Notification only: no route scan, chunk access or endpoint iteration in the event. */
    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) changed(level);
    }

}
