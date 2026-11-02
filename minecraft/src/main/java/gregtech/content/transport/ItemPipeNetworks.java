package gregtech.content.transport;

import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
//?}

/**
 * Per-level scheduler for item pipes, which do not tick themselves. At the end of each level tick it
 * handles, in order: pipes that just loaded (connection repair, snapshot invalidation around them),
 * pipes that carry covers, and source pipes holding items that are due to deliver.
 *
 * <p>An idle pipe costs nothing. A source delivers as soon as items arrive; when nothing could be
 * sent it backs off from {@link #MIN_RETRY} to {@link #MAX_RETRY} ticks, and it is woken early when
 * new items arrive, its network's layout changes, or an inventory beside the network reports room.
 * A source whose own bandwidth is spent waits for the next window.</p>
 */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class ItemPipeNetworks {
    private static final int MIN_RETRY = 10;
    private static final int MAX_RETRY = 40;
    private static final Map<Level, State> STATES = new WeakHashMap<>();

    private ItemPipeNetworks() {}

    private static final class Waiting {
        final ItemPipeBlockEntity pipe;
        long due = Long.MIN_VALUE;
        int backoff;
        long wakeVersion;

        Waiting(ItemPipeBlockEntity pipe) {
            this.pipe = pipe;
        }
    }

    private static final class State {
        final Level level;
        final Set<ItemPipeBlockEntity> joined = new LinkedHashSet<>();
        final Set<ItemPipeBlockEntity> covered = new LinkedHashSet<>();
        final Map<ItemPipeBlockEntity, Waiting> waiting = new LinkedHashMap<>();
        final TreeMap<Long, Set<Waiting>> buckets = new TreeMap<>();
        long drained = Long.MIN_VALUE;

        State(Level level) {
            this.level = level;
        }

        void wake(ItemPipeBlockEntity pipe) {
            Waiting entry = waiting.computeIfAbsent(pipe, Waiting::new);
            entry.backoff = 0;
            entry.wakeVersion++;
            long soonest = Math.max(level.getGameTime(), drained + 1);
            if (entry.due == Long.MIN_VALUE || entry.due > soonest) schedule(entry, soonest);
        }

        void unschedule(Waiting entry) {
            Set<Waiting> bucket = buckets.get(entry.due);
            if (bucket != null) {
                bucket.remove(entry);
                if (bucket.isEmpty()) buckets.remove(entry.due);
            }
            entry.due = Long.MIN_VALUE;
        }

        void schedule(Waiting entry, long tick) {
            // A callback during delivery can wake this or another source. Deferring it also keeps
            // the current bucket stable and prevents a same-tick wake/delivery loop.
            long due = Math.max(tick, drained + 1);
            if (entry.due == due) return;
            unschedule(entry);
            entry.due = due;
            buckets.computeIfAbsent(due, key -> new LinkedHashSet<>()).add(entry);
        }

        void forget(ItemPipeBlockEntity pipe) {
            Waiting entry = waiting.remove(pipe);
            if (entry != null) unschedule(entry);
        }
    }

    private static State state(Level level) {
        return level == null || level.isClientSide ? null : STATES.computeIfAbsent(level, State::new);
    }

    // ── Pipe lifecycle ───────────────────────────────────────────────────────

    /** Register placement or chunk load for connection repair at the end of the tick. */
    public static void joined(ItemPipeBlockEntity pipe) {
        State state = state(pipe.getLevel());
        if (state != null) state.joined.add(pipe);
    }

    /** A pipe left the level, by removal or chunk unload. */
    public static void left(ItemPipeBlockEntity pipe) {
        invalidate(pipe);
        State state = state(pipe.getLevel());
        if (state == null) return;
        state.joined.remove(pipe);
        state.covered.remove(pipe);
        state.forget(pipe);
    }

    /** The layout around {@code pipe} changed: its network snapshot is stale. */
    public static void invalidate(ItemPipeBlockEntity pipe) {
        ItemPipeNetwork network = pipe.network();
        if (network != null) network.invalidate();
        // Opening a previously one-sided connection joins snapshots which did not contain this pipe.
        Level level = pipe.getLevel();
        if (level == null || level.isClientSide) return;
        for (Direction face : Direction.values()) {
            BlockPos next = pipe.getBlockPos().relative(face);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof ItemPipeBlockEntity neighbor) {
                ItemPipeNetwork adjacent = neighbor.network();
                if (adjacent != null) adjacent.invalidate();
            }
        }
    }

    /** {@code pipe} holds new items. */
    public static void wake(ItemPipeBlockEntity pipe) {
        State state = state(pipe.getLevel());
        if (state != null) state.wake(pipe);
    }

    /** Only retrievers change the cached layout; all other cover rules are checked at delivery. */
    public static void coversChanged(ItemPipeBlockEntity pipe, boolean layoutChanged) {
        if (layoutChanged) invalidate(pipe);
        else inventoryUpdated(pipe);
        State state = state(pipe.getLevel());
        if (state == null) return;
        if (pipe.hasCovers()) state.covered.add(pipe);
        else state.covered.remove(pipe);
    }

    /** An inventory beside {@code pipe} reported room; sources that routed through its network retry. */
    public static void inventoryUpdated(ItemPipeBlockEntity pipe) {
        ItemPipeNetwork network = pipe.network();
        if (network != null && network.valid()) network.wakeSources();
    }

    /** The current snapshot containing {@code pipe}, built on demand. */
    public static ItemPipeNetwork networkOf(ItemPipeBlockEntity pipe) {
        State state = state(pipe.getLevel());
        if (state == null || !loaded(state, pipe)) return null;
        ItemPipeNetwork network = pipe.network();
        return network != null && network.valid() ? network : ItemPipeNetwork.build(state.level, pipe, state::wake);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

//? if >=1.21.1 {
    /*@SubscribeEvent public static void end(LevelTickEvent.Post event) { tick(event.getLevel()); }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { if (!event.getLevel().isClientSide()) STATES.remove(event.getLevel()); }
*///?} else {
    @SubscribeEvent public static void end(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) tick(event.level);
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) { if (!event.getLevel().isClientSide()) STATES.remove(event.getLevel()); }
//?}

    public static void tick(Level level) {
        if (level.isClientSide) return;
        State state = STATES.get(level);
        if (state == null) return;
        if (!state.joined.isEmpty()) {
            List<ItemPipeBlockEntity> joined = new ArrayList<>(state.joined);
            state.joined.clear();
            for (ItemPipeBlockEntity pipe : joined) settle(state, pipe);
        }
        if (!state.covered.isEmpty()) {
            for (ItemPipeBlockEntity pipe : new ArrayList<>(state.covered)) {
                if (!loaded(state, pipe) || !pipe.hasCovers()) state.covered.remove(pipe);
                else if (level.shouldTickBlocksAt(pipe.getBlockPos())) pipe.tickCovers();
            }
        }
        deliverDue(state);
    }

    /**
     * Repair half-connections the load order hid, then invalidate the snapshots this pipe may join or
     * split: its own and every adjacent pipe's.
     */
    private static void settle(State state, ItemPipeBlockEntity pipe) {
        if (!loaded(state, pipe)) return;
        pipe.autoConnectOnPlace(null);
        invalidate(pipe);
        if (pipe.hasCovers()) state.covered.add(pipe);
        if (pipe.hasStoredItems()) state.wake(pipe);
    }

    private static boolean loaded(State state, ItemPipeBlockEntity pipe) {
        return !pipe.isRemoved() && state.level.hasChunkAt(pipe.getBlockPos())
                && state.level.getBlockEntity(pipe.getBlockPos()) == pipe;
    }

    private static void deliverDue(State state) {
        long now = state.level.getGameTime();
        state.drained = now;
        while (!state.buckets.isEmpty() && state.buckets.firstKey() <= now) {
            var bucket = state.buckets.pollFirstEntry();
            for (Waiting waiting : bucket.getValue()) {
                if (state.waiting.get(waiting.pipe) != waiting || waiting.due != bucket.getKey()) continue;
                waiting.due = Long.MIN_VALUE;
                deliver(state, waiting, now);
            }
        }
    }

    private static void deliver(State state, Waiting waiting, long now) {
        ItemPipeBlockEntity pipe = waiting.pipe;
        if (!loaded(state, pipe) || !pipe.hasStoredItems()) {
            state.forget(pipe);
            return;
        }
        if (!state.level.shouldTickBlocksAt(pipe.getBlockPos())) {
            state.schedule(waiting, now + MIN_RETRY);
            return;
        }
        long wakeVersion = waiting.wakeVersion;
        ItemPipeNetwork network = networkOf(pipe);
        int moved = network == null ? 0 : pipe.deliverThrough(network);
        // A target callback can remove or re-register the source. Never alter its new schedule.
        if (state.waiting.get(pipe) != waiting) return;
        if (!pipe.hasStoredItems()) {
            state.forget(pipe);
            return;
        }
        if (waiting.wakeVersion != wakeVersion) return;
        if (!pipe.pipeCapacityCheck()) {
            waiting.backoff = 0;
            state.schedule(waiting, pipe.nextBandwidthWindow());
        } else {
            waiting.backoff = moved > 0 ? 0 : waiting.backoff == 0 ? MIN_RETRY : Math.min(MAX_RETRY, waiting.backoff * 2);
            state.schedule(waiting, now + Math.max(MIN_RETRY, waiting.backoff));
        }
    }
}
