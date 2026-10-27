package gregtech.engine.multiblock.tracking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

//? if >=1.21.1 {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else {
import gregtech.GregTech;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
//?}

import java.util.ArrayList;
import java.util.List;

/** End-of-tick structure scans. Formed controllers that just changed are not subject to the cell budget. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}
public final class StructureScheduler {
    public static final int CELLS_PER_TICK = 20_000;
    public static final int SAFETY_PERIOD = 600;
    private static int testBudget = -1;
    private static int safetyVisited;

    private StructureScheduler() {}

    public static void flush(Level level) {
        boolean auditing = StructureScanProbe.auditing();
        StructureScanProbe.audit(true);
        try {
            end(level);
        } finally {
            StructureScanProbe.audit(auditing);
        }
    }

    /** One budget slice, without turning footprint audit on. */
    public static void tick(Level level) {
        end(level);
    }

    public static void runSafetyNow(Level level) {
        if (!(level instanceof ServerLevel server)) return;
        WorldStructureClaims.checkOrphans(server, true);
        for (StructureTracker tracker : StructureWatchIndex.all(server)) {
            if (tracker.unloading() || tracker.entity().isRemoved()) continue;
            if (WorldStructureClaims.waiting(tracker)) continue;
            tracker.requestFullEval();
            tracker.enqueue(false);
            StructureWatchIndex.enqueue(tracker);
        }
        end(level);
    }

    /** {@code cells < 0} restores {@link #CELLS_PER_TICK}. */
    public static void setCellBudgetForTests(int cells) {
        testBudget = cells;
    }

    public static int safetyVisitedForTests() {
        return safetyVisited;
    }

    public static void enqueueAll() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) StructureWatchIndex.enqueueAll(server);
    }

    /**
     * New epoch. A tracker whose verdict can change (tag cells, or an overlay machine the reload touched)
     * loses its formed bit now; every other tracker is only queued for a budgeted rescan.
     */
    public static void epochChanged(java.util.Set<String> machines) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (StructureTracker tracker : StructureWatchIndex.all(level)) {
                if (tracker.unloading() || tracker.entity().isRemoved()) continue;
                if (affected(tracker, machines)) {
                    boolean fell = tracker.invalidate();
                    StructureWatchIndex.enqueue(tracker);
                    if (fell) {
                        StructureTracking.formedChanged(tracker.machine(), false);
                        ClientFormedSync.send(tracker.entity());
                    }
                } else {
                    tracker.enqueue(false);
                    StructureWatchIndex.enqueue(tracker);
                }
            }
        }
    }

    private static boolean affected(StructureTracker tracker, java.util.Set<String> machines) {
        StructureDefinition definition = tracker.machine().structureDefinition();
        if (definition.usesTags()) return true;
        String name = definition.overlayMachine();
        return name != null && machines.contains(name);
    }

    public static void chunkLoadedForTests(ServerLevel level, ChunkPos pos) {
        StructureWatchIndex.chunkLoaded(level, pos);
    }

    public static void chunkUnloadedForTests(ServerLevel level, ChunkPos pos) {
        StructureWatchIndex.chunkUnloaded(level, pos);
    }

    public static boolean pendingForTests(ServerLevel level, long chunk) {
        return StructureWatchIndex.pending(level, chunk);
    }

    public static void deferChunkForTests(ServerLevel level, long chunk) {
        StructureWatchIndex.defer(level, chunk);
    }

    //? if >=1.21.1 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void end(LevelTickEvent.Post event) {
        end(event.getLevel());
    }
    @SubscribeEvent
    public static void load(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            StructureTrackingEvents.record("ChunkEvent.Load " + event.getChunk().getPos().toLong());
            StructureWatchIndex.chunkLoaded(level, event.getChunk().getPos());
        }
    }
    @SubscribeEvent
    public static void unloadChunk(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            StructureTrackingEvents.record("ChunkEvent.Unload " + event.getChunk().getPos().toLong());
            StructureWatchIndex.chunkUnloaded(level, event.getChunk().getPos());
        }
    }
    @SubscribeEvent
    public static void unloadLevel(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) StructureWatchIndex.unloadWorld(level);
    }
    *///?} else {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void end(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        end(event.level);
    }

    @SubscribeEvent
    public static void load(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            StructureTrackingEvents.record("ChunkEvent.Load " + event.getChunk().getPos().toLong());
            StructureWatchIndex.chunkLoaded(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void unloadChunk(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            StructureTrackingEvents.record("ChunkEvent.Unload " + event.getChunk().getPos().toLong());
            StructureWatchIndex.chunkUnloaded(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void unloadLevel(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) StructureWatchIndex.unloadWorld(level);
    }
    //?}

    private static void end(Level level) {
        if (!(level instanceof ServerLevel server) || level.isClientSide) return;
        WorldStructureClaims.checkOrphans(server, false);
        StructureWatchIndex.promotePending(server);
        safetyVisited = 0;
        enqueueSafety(server, level.getGameTime());
        List<StructureTracker> batch = StructureWatchIndex.drainDirty(server);
        if (batch.isEmpty()) return;
        List<StructureTracker> expedited = new ArrayList<>();
        List<StructureTracker> normal = new ArrayList<>();
        for (StructureTracker tracker : batch) {
            if (tracker.expedite()) expedited.add(tracker);
            else normal.add(tracker);
        }
        for (StructureTracker tracker : expedited) StructureTracking.evaluate(tracker);
        int budget = testBudget >= 0 ? testBudget : CELLS_PER_TICK;
        boolean evaluated = false;
        for (int index = 0; index < normal.size(); index++) {
            StructureTracker tracker = normal.get(index);
            int cost = tracker.cellCount();
            if (evaluated && cost > budget) {
                for (int rest = normal.size() - 1; rest >= index; rest--)
                    StructureWatchIndex.requeue(normal.get(rest));
                return;
            }
            budget -= cost;
            StructureTracking.evaluate(tracker);
            evaluated = true;
        }
    }

    private static void enqueueSafety(ServerLevel level, long gameTime) {
        int phase = (int) Math.floorMod(gameTime, SAFETY_PERIOD);
        for (StructureTracker tracker : StructureWatchIndex.safetyBucket(level, phase)) {
            safetyVisited++;
            if (tracker.unloading() || tracker.dirty() || tracker.entity().isRemoved()) continue;
            if (WorldStructureClaims.waiting(tracker)) continue;
            tracker.requestFullEval();
            tracker.enqueue(false);
            StructureWatchIndex.enqueue(tracker);
        }
    }
}
