package com.gregtech.gregtech.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.*;

/**
 * Per-dimension wire connectivity graph (GT6 {@code EnergyNet}).
 * <p>
 * Tracks which wire segments are connected so energy transfer can
 * traverse the full network efficiently.
 *
 * <p>The map is per dimension and outlives chunks, so it is the one structure here that has to be
 * released explicitly: wire block entities register from {@code onLoad} and release from
 * {@code setRemoved} (the hook a chunk unload actually calls). Neither {@link #getConnectedWires} nor
 * {@link #getReceivers} has a caller in the port today, so every position this map retains is state
 * nobody reads - which is why leaving it unpruned cost memory for no result at all.</p>
 */
public final class EnergyNet {
    private EnergyNet() {}

    /** Adjacency per dimension: wire pos → set of connected wire positions. */
    private static final Map<Level, Map<BlockPos, Set<BlockPos>>> GRAPHS = new WeakHashMap<>();

    private static final BooleanProperty[] DUMMY_PROPS = {};

    /* ---- registration ---- */

    public static void onPlace(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        GRAPHS.computeIfAbsent(level, k -> new HashMap<>())
                .putIfAbsent(pos, new HashSet<>());
    }

    /**
     * Drops a wire position and every edge that pointed at it.
     *
     * <p>The map is a per-dimension structure that outlives chunks, so this method is the only thing
     * that keeps it proportional to the wires that actually exist. A neighbour whose edge set became
     * empty is dropped with it: an entry without edges carries no connectivity, and traversal is
     * unchanged by its removal because {@link #getConnectedWires} reports its start position whether
     * or not the map has an entry for it. Without that second step the map would keep one entry per
     * position that was ever a wire or a conductor next to one, i.e. it would grow with every block a
     * player ever placed and never shrink again.</p>
     */
    public static void onRemove(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        Map<BlockPos, Set<BlockPos>> graph = GRAPHS.get(level);
        if (graph == null) return;
        Set<BlockPos> neighbors = graph.remove(pos);
        if (neighbors != null) {
            for (BlockPos nb : neighbors) {
                Set<BlockPos> nbSet = graph.get(nb);
                if (nbSet != null) {
                    nbSet.remove(pos);
                    if (nbSet.isEmpty()) graph.remove(nb);
                }
            }
        }
    }

    /**
     * Number of wire positions this class currently retains for {@code level} - the size of the map
     * that {@link #onPlace} grows.
     *
     * <p>Exposed so {@code MemoryGrowthTests} can measure that a removed block entity hands its
     * position back instead of keeping it for the lifetime of the level. This is the accessor for the
     * unbounded-growth invariant of this class, not a gameplay API.</p>
     */
    public static int trackedPositions(Level level) {
        Map<BlockPos, Set<BlockPos>> graph = GRAPHS.get(level);
        return graph == null ? 0 : graph.size();
    }

    /**
     * Whether {@code pos} still has an entry in the map for {@code level}.
     *
     * <p>The exact form of the invariant {@link #trackedPositions} measures: after a wire's block entity
     * has been removed, neither the wire's own position nor a neighbour position that only pointed at it
     * may still be tracked. Also a test accessor, not a gameplay API.</p>
     */
    public static boolean tracks(Level level, BlockPos pos) {
        Map<BlockPos, Set<BlockPos>> graph = GRAPHS.get(level);
        return graph != null && graph.containsKey(pos);
    }

    /** Rebuild adjacency from block state after a connection toggle. */
    public static void onConnectionChange(Level level, BlockPos pos, BlockState state,
                                          BooleanProperty[] connectionProps) {
        if (level.isClientSide) return;
        Map<BlockPos, Set<BlockPos>> graph = GRAPHS.get(level);
        if (graph == null) return;
        Set<BlockPos> edges = graph.computeIfAbsent(pos, k -> new HashSet<>());
        edges.clear();

        for (int i = 0; i < connectionProps.length; i++) {
            if (!state.getValue(connectionProps[i])) continue;
            Direction dir = Direction.from3DDataValue(i);
            BlockPos nbPos = pos.relative(dir);
            BlockEntity nbBe = level.getBlockEntity(nbPos);
            if (nbBe instanceof IEnergyConductor) {
                edges.add(nbPos.immutable());
                // make symmetric
                Set<BlockPos> nbEdges = graph.computeIfAbsent(nbPos.immutable(), k -> new HashSet<>());
                nbEdges.add(pos.immutable());
            }
        }
    }

    /* ---- traversal ---- */

    /** All wire positions in the same connected component as {@code start}. */
    public static Set<BlockPos> getConnectedWires(Level level, BlockPos start) {
        if (level.isClientSide) return Set.of();
        Map<BlockPos, Set<BlockPos>> graph = GRAPHS.get(level);
        if (graph == null) return Set.of();

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            Set<BlockPos> neighbors = graph.get(cur);
            if (neighbors == null) continue;
            for (BlockPos nb : neighbors) {
                if (visited.add(nb)) queue.add(nb);
            }
        }
        return visited;
    }

    /** Non-wire neighbors of every wire in the connected component. */
    public static Set<BlockPos> getReceivers(Level level, Set<BlockPos> wirePositions) {
        Set<BlockPos> receivers = new LinkedHashSet<>();
        for (BlockPos wp : wirePositions) {
            for (Direction dir : Direction.values()) {
                BlockPos nb = wp.relative(dir);
                if (wirePositions.contains(nb)) continue;
                BlockEntity be = level.getBlockEntity(nb);
                if (be instanceof IEnergyBlock) {
                    receivers.add(nb.immutable());
                }
            }
        }
        return receivers;
    }
}
