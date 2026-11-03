/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityPipeFluid.onTick2/onServerTickPre and WD.even. */
package gregtech.content.transport.fluid;

import gregtech.GregTechConfig;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class FluidPipeTickPhases {
    /** A sleeping pipe still runs this often so a missed wake-up cannot strand fluid forever. */
    private static final int HEARTBEAT_TICKS = 600;
    private static final Map<Level, State> STATES = new WeakHashMap<>();

    private FluidPipeTickPhases() {}

    private static final class State {
        final Level level;
        /** Pipes the block ticker enrolled for the next start of tick, in native tick order. */
        Set<FluidPipeBlockEntity> pending = new LinkedHashSet<>();
        /** Pipes that have work (or ran this tick) and must run again at the next start of tick. */
        final Set<FluidPipeBlockEntity> active = new LinkedHashSet<>();
        /** Idle pipes, keyed by the tick at which their heartbeat is due. */
        final Map<FluidPipeBlockEntity, Long> sleeping = new LinkedHashMap<>();
        /** Pipes that just loaded; shared-pool snapshots around them are stale. */
        final Set<FluidPipeBlockEntity> joined = new LinkedHashSet<>();
        /** Sleeping pipes woken during the first phase, still due in this tick's second phase. */
        final Set<FluidPipeBlockEntity> woken = new LinkedHashSet<>();
        /** The queue being iterated, so a wake does not add the running pipe again. */
        Set<FluidPipeBlockEntity> current;
        boolean ticking;
        int phase = 2;
        /** The block-entity pass currently collecting pending pipes. */
        long generation;
        /** True between start of tick and the first ticker call that opens the next pass. */
        boolean expectPass = true;
        long nextRank;
        long pendingMaxRank = -1;

        State(Level level) {
            this.level = level;
        }
    }

    private static State stateOf(Level level) {
        return level == null || level.isClientSide ? null : STATES.computeIfAbsent(level, State::new);
    }

    /**
     * Native ticking enrolls only pipes that need the next pipe pass. With idle scheduling disabled
     * every loaded pipe is enrolled, restoring the one-pass-per-tick behaviour for comparison.
     * Every call is stamped with its pass and rank, including the sleeping pipes it skips, so a
     * later wake can reinsert the pipe at the position the original tick loop would have used.
     */
    public static void enqueue(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity pipe) {
        if (level.isClientSide) return;
        State queue = stateOf(level);
        if (queue.expectPass) {
            queue.expectPass = false;
            queue.generation++;
            queue.nextRank = 0;
        }
        pipe.schedulerOffered(queue.generation, queue.nextRank++);
        if (!GregTechConfig.fluidIdleScheduling() || queue.active.contains(pipe)) {
            addPending(queue, pipe);
            return;
        }
        Long due = queue.sleeping.get(pipe);
        if (due == null || level.getGameTime() >= due) addPending(queue, pipe);
    }

    /** Append to the next queue, keeping it in ticker order when a wake inserts after the pass. */
    private static void addPending(State queue, FluidPipeBlockEntity pipe) {
        if (!queue.pending.add(pipe)) return;
        long rank = pipe.schedulerRank();
        if (rank >= queue.pendingMaxRank) {
            queue.pendingMaxRank = rank;
            return;
        }
        List<FluidPipeBlockEntity> ordered = new ArrayList<>(queue.pending);
        ordered.sort(Comparator.comparingLong(FluidPipeBlockEntity::schedulerRank));
        queue.pending.clear();
        queue.pending.addAll(ordered);
        queue.pendingMaxRank = ordered.get(ordered.size() - 1).schedulerRank();
    }

    /** A pipe has a reason to run: new fluid, a cover change, a neighbour change or a load. */
    public static void wake(FluidPipeBlockEntity pipe) {
        State queue = stateOf(pipe.getLevel());
        if (queue == null) return;
        queue.active.add(pipe);
        queue.sleeping.remove(pipe);
        if (queue.ticking) {
            // All neighbours have the opposite parity, so waking during the first phase still
            // leaves the pipe's own (second) phase ahead in this same tick.
            if (queue.phase == 0 && parity(pipe) == 0 && queue.current != null
                    && !queue.current.contains(pipe)
                    && pipe.schedulerGeneration() == queue.generation) {
                queue.woken.add(pipe);
            }
            return;
        }
        // The ticker call for this pass already ran and skipped the sleeping pipe; the next start
        // of tick would otherwise miss it. The upcoming call enrols a not-yet-visited pipe itself.
        if (queue.expectPass) return;
        if (pipe.schedulerGeneration() == queue.generation) addPending(queue, pipe);
    }

    /** Wake this pipe and every adjacent pipe; a change on one can unblock the other. */
    public static void wakeAround(FluidPipeBlockEntity pipe) {
        wake(pipe);
        Level level = pipe.getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos origin = pipe.getBlockPos();
        for (Direction side : Direction.values()) {
            BlockPos next = origin.relative(side);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbor) {
                wake(neighbor);
            }
        }
    }

    public static void joined(FluidPipeBlockEntity pipe) {
        wake(pipe);
        State queue = stateOf(pipe.getLevel());
        if (queue != null) queue.joined.add(pipe);
    }

    public static void left(FluidPipeBlockEntity pipe) {
        State queue = stateOf(pipe.getLevel());
        if (queue == null) return;
        queue.pending.remove(pipe);
        queue.active.remove(pipe);
        queue.sleeping.remove(pipe);
        queue.joined.remove(pipe);
        queue.woken.remove(pipe);
    }

//? if >=1.21.1 {
    /*@SubscribeEvent public static void start(LevelTickEvent.Pre event) { tick(event.getLevel()); }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { STATES.remove(event.getLevel()); }
*///?} else {
    @SubscribeEvent
    public static void start(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) tick(event.level);
    }

    @SubscribeEvent
    public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        STATES.remove(event.getLevel());
    }
//?}

    public static void tick(Level level) {
        if (level.isClientSide) return;
        State queue = STATES.get(level);
        if (queue == null) return;
        try {
            if (!queue.joined.isEmpty()) {
                for (FluidPipeBlockEntity pipe : new ArrayList<>(queue.joined)) {
                    BlockPos pos = pipe.getBlockPos();
                    if (pipe.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != pipe) continue;
                    FluidPipeNetworks.invalidate(pipe);
                }
                queue.joined.clear();
            }
            Set<FluidPipeBlockEntity> queued = queue.pending;
            queue.pending = new LinkedHashSet<>();
            queue.pendingMaxRank = -1;
            queue.current = queued;
            queue.ticking = true;
            queue.woken.clear();
            if (queued.isEmpty()) return;
            boolean scheduling = GregTechConfig.fluidIdleScheduling();
            long now = level.getGameTime();
            // Original WD.even counts even coordinates: its first phase is odd sum parity. Every
            // adjacent pair has opposite parity, and a rotation cannot reverse their ordering.
            for (queue.phase = 0; queue.phase < 2; queue.phase++) {
                List<FluidPipeBlockEntity> extra = queue.phase == 1
                        ? wokenInSecondPhase(queue) : Collections.emptyList();
                int extraIndex = 0;
                for (FluidPipeBlockEntity pipe : queued) {
                    if (parity(pipe) != 1 - queue.phase) continue;
                    if (queue.phase == 1) {
                        while (extraIndex < extra.size()
                                && extra.get(extraIndex).schedulerRank() < pipe.schedulerRank()) {
                            tickPipe(level, queue, extra.get(extraIndex++), scheduling, now);
                        }
                    }
                    tickPipe(level, queue, pipe, scheduling, now);
                }
                for (; extraIndex < extra.size(); extraIndex++) {
                    tickPipe(level, queue, extra.get(extraIndex), scheduling, now);
                }
            }
        } finally {
            queue.ticking = false;
            queue.phase = 2;
            queue.current = null;
            queue.woken.clear();
            queue.expectPass = true;
        }
    }

    /** Woken pipes that still have their second phase ahead in this tick, in ticker order. */
    private static List<FluidPipeBlockEntity> wokenInSecondPhase(State queue) {
        if (queue.woken.isEmpty()) return Collections.emptyList();
        List<FluidPipeBlockEntity> extra = new ArrayList<>(queue.woken.size());
        for (FluidPipeBlockEntity pipe : queue.woken) {
            if (parity(pipe) == 0) extra.add(pipe);
        }
        extra.sort(Comparator.comparingLong(FluidPipeBlockEntity::schedulerRank));
        return extra;
    }

    private static void tickPipe(Level level, State queue, FluidPipeBlockEntity pipe,
                                 boolean scheduling, long now) {
        BlockPos pos = pipe.getBlockPos();
        if (pipe.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != pipe) return;
        if (scheduling) {
            Long due = queue.sleeping.get(pipe);
            if (!queue.active.contains(pipe) && due != null && now < due) return;
        }
        FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
        if (!scheduling) return;
        if (pipe.isRemoved()) {
            queue.active.remove(pipe);
            queue.sleeping.remove(pipe);
            return;
        }
        // Keep a pipe that moved anything awake one more tick, so the tick that resets its
        // transfer counter still runs and the progress sensor sees the same value as before.
        if (pipe.isIdle() && pipe.getTransferredAmount() == 0) {
            queue.active.remove(pipe);
            queue.sleeping.put(pipe, now + HEARTBEAT_TICKS);
        } else {
            queue.active.add(pipe);
            queue.sleeping.remove(pipe);
        }
    }

    private static int parity(FluidPipeBlockEntity pipe) {
        BlockPos pos = pipe.getBlockPos();
        return (pos.getX() ^ pos.getY() ^ pos.getZ()) & 1;
    }
}
