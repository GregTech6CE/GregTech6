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
     */
    public static void enqueue(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity pipe) {
        if (level.isClientSide) return;
        State queue = stateOf(level);
        if (!GregTechConfig.fluidIdleScheduling() || queue.active.contains(pipe)) {
            queue.pending.add(pipe);
            return;
        }
        Long due = queue.sleeping.get(pipe);
        if (due == null || level.getGameTime() >= due) queue.pending.add(pipe);
    }

    /** A pipe has a reason to run: new fluid, a cover change, a neighbour change or a load. */
    public static void wake(FluidPipeBlockEntity pipe) {
        State queue = stateOf(pipe.getLevel());
        if (queue == null) return;
        queue.active.add(pipe);
        queue.sleeping.remove(pipe);
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
    }

//? if >=1.21.1 {
    /*@SubscribeEvent public static void start(LevelTickEvent.Pre event) { tick(event.getLevel()); }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { STATES.remove(event.getLevel()); }
*///?} else {
    @SubscribeEvent public static void start(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) tick(event.level);
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) { STATES.remove(event.getLevel()); }
//?}

    public static void tick(Level level) {
        if (level.isClientSide) return;
        State queue = STATES.get(level);
        if (queue == null) return;
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
        if (queued.isEmpty()) return;
        boolean scheduling = GregTechConfig.fluidIdleScheduling();
        long now = level.getGameTime();
        // Every adjacent pair has opposite parity. A rotation cannot reverse their tick ordering.
        for (int phase = 0; phase < 2; phase++) for (FluidPipeBlockEntity pipe : queued) {
            BlockPos pos = pipe.getBlockPos();
            int parity = (pos.getX() ^ pos.getY() ^ pos.getZ()) & 1;
            // Original WD.even counts even coordinates: its first phase is odd sum parity.
            if (parity != 1 - phase || pipe.isRemoved() || !level.hasChunkAt(pos)
                    || level.getBlockEntity(pos) != pipe) continue;
            if (scheduling) {
                Long due = queue.sleeping.get(pipe);
                if (!queue.active.contains(pipe) && due != null && now < due) continue;
            }
            FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
            if (!scheduling) continue;
            if (pipe.isRemoved()) {
                queue.active.remove(pipe);
                queue.sleeping.remove(pipe);
                continue;
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
    }
}
