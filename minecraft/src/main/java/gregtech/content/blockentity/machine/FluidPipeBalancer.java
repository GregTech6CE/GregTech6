package gregtech.content.blockentity.machine;

import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.cover.CoverAttachmentBehaviors;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.engine.fluid.FluidPipeChannels;
import gregtech.engine.fluid.FluidTankGT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Levels each fluid across connected pipe channels in balanced distribution mode. */
final class FluidPipeBalancer {
    private static final int BALANCE_PERIOD = 10;
    private static final long BALANCE_DEADBAND = 1;

    private FluidPipeBalancer() {}

    static void balanceComponent(Level level, FluidPipeBlockEntity start) {
        long nextBalance = level.getGameTime() + BALANCE_PERIOD;
        List<FluidPipeBlockEntity> component = new ArrayList<>();
        Map<FluidPipeBlockEntity, Integer> index = new IdentityHashMap<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
        seen.add(start.getBlockPos());
        queue.add(start);
        while (!queue.isEmpty()) {
            FluidPipeBlockEntity pipe = queue.poll();
            index.put(pipe, component.size());
            component.add(pipe);
            for (Direction side : Direction.values()) {
                if (!pipe.connected(side)) continue;
                BlockPos next = pipe.getBlockPos().relative(side);
                if (!level.hasChunkAt(next) || !seen.add(next)) continue;
                if (level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbor
                        && neighbor.connected(side.getOpposite())) {
                    queue.add(neighbor);
                }
            }
        }
        for (FluidPipeBlockEntity pipe : component) pipe.deferBalanceUntil(nextBalance);

        Map<Fluid, FluidStack> fluids = new LinkedHashMap<>();
        for (FluidPipeBlockEntity pipe : component) {
            for (FluidTankGT tank : pipe.channelTanks()) {
                if (!tank.isEmpty()) fluids.putIfAbsent(tank.getFluid().getFluid(), tank.getFluid());
            }
        }
        for (FluidStack fluid : fluids.values()) balanceFluid(level, component, index, fluid);
    }

    private static void balanceFluid(Level level, List<FluidPipeBlockEntity> component,
                                     Map<FluidPipeBlockEntity, Integer> index, FluidStack fluid) {
        int size = component.size();
        int[] channel = new int[size];
        long[] amount = new long[size];
        long[] capacity = new long[size];
        for (int i = 0; i < size; i++) {
            FluidPipeBlockEntity pipe = component.get(i);
            channel[i] = isBalanceExcluded(pipe) ? -1 : pipe.fillableChannel(fluid);
            if (channel[i] < 0) continue;
            FluidTankGT tank = pipe.channelTanks()[channel[i]];
            amount[i] = tank.isEmpty() ? 0 : tank.getAmount();
            capacity[i] = tank.capacity(fluid);
        }

        List<List<Integer>> adjacency = new ArrayList<>(size);
        for (int i = 0; i < size; i++) adjacency.add(new ArrayList<>());
        for (int i = 0; i < size; i++) {
            if (channel[i] < 0) continue;
            FluidPipeBlockEntity pipe = component.get(i);
            for (Direction side : Direction.values()) {
                if (!pipe.connected(side)) continue;
                BlockPos next = pipe.getBlockPos().relative(side);
                if (!level.hasChunkAt(next) || !(level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbor)) continue;
                Integer other = index.get(neighbor);
                if (other == null || other <= i || channel[other] < 0 || !neighbor.connected(side.getOpposite())) continue;
                if (!balanceEdgeOpen(pipe, side, neighbor, fluid)) continue;
                adjacency.get(i).add(other);
                adjacency.get(other).add(i);
            }
        }

        boolean[] visited = new boolean[size];
        int[] parent = new int[size];
        long[] balance = new long[size];
        List<Integer> order = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int root = 0; root < size; root++) {
            if (channel[root] < 0 || visited[root]) continue;
            order.clear();
            visited[root] = true;
            parent[root] = -1;
            stack.push(root);
            while (!stack.isEmpty()) {
                int node = stack.pop();
                order.add(node);
                for (int next : adjacency.get(node)) {
                    if (visited[next]) continue;
                    visited[next] = true;
                    parent[next] = node;
                    stack.push(next);
                }
            }
            if (order.size() > 1) {
                balanceTree(component, channel, fluid, amount, capacity, order, parent, balance);
            }
        }
    }

    /**
     * Levels a connected group along a spanning tree. Leaves first push excess toward the root,
     * then each deficit subtree pulls fluid down. A pipe at its target can still forward fluid to
     * a deficit branch behind it. Each edge moves at most one source tank of fluid per pass.
     */
    private static void balanceTree(List<FluidPipeBlockEntity> component, int[] channel,
                                    FluidStack fluid, long[] amount, long[] capacity,
                                    List<Integer> order, int[] parent, long[] balance) {
        long total = 0;
        long totalCapacity = 0;
        for (int node : order) {
            total += amount[node];
            totalCapacity += capacity[node];
        }
        if (total <= 0 || total >= totalCapacity) return;

        long[] target = FluidPipeChannels.balancedTargets(order, capacity, total);

        // Leaves to root: each subtree sheds its excess into its parent.
        for (int k = order.size() - 1; k > 0; k--) {
            int node = order.get(k);
            long surplus = amountOf(component, channel, node) - target[node];
            if (surplus <= BALANCE_DEADBAND) continue;
            moveEdge(component, channel, fluid, node, parent[node], surplus);
        }
        // Subtree surplus or deficit after the upward pass is what the downward pass moves.
        for (int node : order) balance[node] = amountOf(component, channel, node) - target[node];
        for (int k = order.size() - 1; k > 0; k--) {
            int node = order.get(k);
            balance[parent[node]] += balance[node];
        }
        // Root to leaves: each deficit subtree pulls fluid down from its parent.
        for (int k = 1; k < order.size(); k++) {
            int node = order.get(k);
            if (balance[node] >= -BALANCE_DEADBAND) continue;
            moveEdge(component, channel, fluid, parent[node], node, -balance[node]);
        }
    }

    private static long amountOf(List<FluidPipeBlockEntity> component, int[] channel, int node) {
        FluidTankGT tank = component.get(node).channelTanks()[channel[node]];
        return tank.isEmpty() ? 0 : tank.getAmount();
    }

    /** One edge moves no more than the source tank's capacity for this fluid in a single pass. */
    private static void moveEdge(List<FluidPipeBlockEntity> component, int[] channel,
                                 FluidStack fluid, int from, int to, long requested) {
        FluidPipeBlockEntity source = component.get(from);
        FluidTankGT sourceTank = source.channelTanks()[channel[from]];
        FluidTankGT sinkTank = component.get(to).channelTanks()[channel[to]];
        long limit = Math.min(requested, sourceTank.capacity(fluid));
        source.transferToPipe(sourceTank, sinkTank, limit);
    }

    /** A balancing edge must pass in both directions, including both ends' filters for this fluid. */
    private static boolean balanceEdgeOpen(FluidPipeBlockEntity from, Direction side,
                                           FluidPipeBlockEntity to, FluidStack fluid) {
        Direction back = side.getOpposite();
        var fromCover = from.getCover(side);
        var toCover = to.getCover(back);
        return ComponentCoverRuntime.allowsFluid(fromCover, false)
                && ComponentCoverRuntime.allowsFluid(fromCover, true)
                && ComponentCoverRuntime.allowsFluid(toCover, false)
                && ComponentCoverRuntime.allowsFluid(toCover, true)
                && from.coverFluidFilterPermits(side, fluid)
                && to.coverFluidFilterPermits(back, fluid);
    }

    /** Covers that act on a pipe's own tank exclude it from levelling. */
    private static boolean isBalanceExcluded(FluidPipeBlockEntity pipe) {
        for (Direction side : Direction.values()) {
            String id = CoverItems.behavior(pipe.getCover(side));
            if (CoverUtilityBehaviors.PRESSURE_VALVE.equals(id)
                    || CoverAttachmentBehaviors.DRAIN.equals(id)
                    || CoverAttachmentBehaviors.AIR_VENT.equals(id)) return true;
        }
        return false;
    }
}
