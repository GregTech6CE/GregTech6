package gregtech.content.transport.energy;

import gregtech.content.blockentity.energy.ElectricWireBlockEntity;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
//?}

/**
 * Per-level scheduler for electric wires, which do not tick themselves. At the start of each level
 * tick it repairs the snapshots around newly loaded wires and turns saturating overloads into fire.
 */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class ElectricNetworks {
    private static final Map<Level, State> STATES = new WeakHashMap<>();

    private ElectricNetworks() {}

    private static final class State {
        final Level level;
        final Set<ElectricWireBlockEntity> joined = new LinkedHashSet<>();
        final Set<ElectricWireBlockEntity> burns = new LinkedHashSet<>();

        State(Level level) {
            this.level = level;
        }
    }

    private static State state(Level level) {
        return level == null || level.isClientSide ? null : STATES.computeIfAbsent(level, State::new);
    }

    // ── Wire lifecycle ───────────────────────────────────────────────────────

    /** Register a placement or chunk load; adjacent snapshots are rebuilt at the start of the tick. */
    public static void joined(ElectricWireBlockEntity wire) {
        State state = state(wire.getLevel());
        if (state != null) state.joined.add(wire);
    }

    /** A wire left the level, by removal or chunk unload. */
    public static void left(ElectricWireBlockEntity wire) {
        invalidate(wire);
        State state = state(wire.getLevel());
        if (state == null) return;
        state.joined.remove(wire);
        state.burns.remove(wire);
    }

    /** The layout around {@code wire} changed: its network snapshot and adjacent ones are stale. */
    public static void invalidate(ElectricWireBlockEntity wire) {
        ElectricNetwork network = wire.network();
        if (network != null) network.invalidate();
        Level level = wire.getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos origin = wire.getBlockPos();
        for (Direction face : Direction.values()) {
            BlockPos next = origin.relative(face);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof ElectricWireBlockEntity neighbor) {
                ElectricNetwork adjacent = neighbor.network();
                if (adjacent != null) adjacent.invalidate();
            }
        }
    }

    /** A wire reached its burn limit; it turns to fire at the start of the next tick. */
    public static void scheduleBurn(ElectricWireBlockEntity wire) {
        State state = state(wire.getLevel());
        if (state != null) state.burns.add(wire);
    }

    /** The current snapshot containing {@code wire}, built on demand. */
    public static ElectricNetwork networkOf(ElectricWireBlockEntity wire) {
        State state = state(wire.getLevel());
        if (state == null || !loaded(state, wire)) return null;
        ElectricNetwork network = wire.network();
        if (network != null && network.valid() && network.contains(wire)) return network;
        return ElectricNetwork.build(state.level, wire);
    }

    /** Inject one packet into the network containing {@code wire}. */
    public static long inject(ElectricWireBlockEntity wire, Direction fromSide, long packetVoltage, long packetAmperage) {
        ElectricNetwork network = networkOf(wire);
        return network == null ? 0 : network.inject(wire, fromSide, packetVoltage, packetAmperage);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

//? if >=1.21.1 {
    /*@SubscribeEvent public static void start(LevelTickEvent.Pre event) { tick(event.getLevel()); }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { if (!event.getLevel().isClientSide()) STATES.remove(event.getLevel()); }
*///?} else {
    @SubscribeEvent public static void start(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) tick(event.level);
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) { if (!event.getLevel().isClientSide()) STATES.remove(event.getLevel()); }
//?}

    public static void tick(Level level) {
        if (level.isClientSide) return;
        State state = STATES.get(level);
        if (state == null) return;
        if (!state.joined.isEmpty()) {
            for (ElectricWireBlockEntity wire : new java.util.ArrayList<>(state.joined)) {
                if (!loaded(state, wire)) continue;
                invalidate(wire);
            }
            state.joined.clear();
        }
        if (!state.burns.isEmpty()) {
            for (ElectricWireBlockEntity wire : new java.util.ArrayList<>(state.burns)) {
                if (wire.isRemoved()) {
                    state.burns.remove(wire);
                    continue;
                }
                BlockPos pos = wire.getBlockPos();
                if (!level.hasChunkAt(pos) || !level.shouldTickBlocksAt(pos)) continue;
                if (level.getBlockEntity(pos) != wire) {
                    state.burns.remove(wire);
                    continue;
                }
                BlockState fire = Blocks.FIRE.defaultBlockState();
                level.setBlockAndUpdate(pos, fire);
                state.burns.remove(wire);
            }
        }
    }

    private static boolean loaded(State state, ElectricWireBlockEntity wire) {
        return !wire.isRemoved() && state.level.hasChunkAt(wire.getBlockPos())
                && state.level.getBlockEntity(wire.getBlockPos()) == wire;
    }
}
