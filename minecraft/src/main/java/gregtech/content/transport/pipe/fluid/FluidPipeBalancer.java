package gregtech.content.transport.pipe.fluid;

import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.cover.CoverAttachmentBehaviors;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.engine.fluid.FluidPipeBalance;
import gregtech.engine.fluid.FluidTankGT;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.List;

/** Levels each fluid across connected pipe channels in balanced distribution mode. */
final class FluidPipeBalancer {
    private static final int BALANCE_PERIOD = 10;

    private FluidPipeBalancer() {}

    static void balanceComponent(Level level, FluidPipeBlockEntity start) {
        long nextBalance = level.getGameTime() + BALANCE_PERIOD;
        FluidPipeTopology topology = FluidPipeTopology.scan(level, start);
        for (int i = 0; i < topology.size(); i++) topology.pipe(i).deferBalanceUntil(nextBalance);
        for (FluidStack fluid : FluidPipeContents.variants(topology)) balanceFluid(level, topology, fluid);
    }

    private static void balanceFluid(Level level, FluidPipeTopology topology, FluidStack fluid) {
        int size = topology.size();
        FluidTankGT[] channels = new FluidTankGT[size];
        long[] capacity = new long[size];
        for (int i = 0; i < size; i++) {
            FluidPipeBlockEntity pipe = topology.pipe(i);
            int channel = isBalanceExcluded(pipe) ? -1 : pipe.fillableChannel(fluid);
            if (channel < 0) continue;
            channels[i] = pipe.channelTanks()[channel];
            capacity[i] = channels[i].capacity(fluid);
        }

        List<List<Integer>> adjacency = new ArrayList<>(size);
        for (int i = 0; i < size; i++) adjacency.add(new ArrayList<>());
        for (int i = 0; i < size; i++) {
            if (channels[i] == null) continue;
            FluidPipeBlockEntity pipe = topology.pipe(i);
            for (Direction side : Direction.values()) {
                FluidPipeBlockEntity neighbor = FluidPipeTopology.connectedNeighbor(level, pipe, side);
                if (neighbor == null) continue;
                int other = topology.indexOf(neighbor);
                if (other <= i || channels[other] == null) continue;
                if (!balanceEdgeOpen(pipe, side, neighbor, fluid)) continue;
                adjacency.get(i).add(other);
                adjacency.get(other).add(i);
            }
        }

        FluidPipeBalance.distribute(adjacency, capacity,
                node -> channels[node].isEmpty() ? 0 : channels[node].getAmount(),
                (from, to, requested) -> {
                    FluidTankGT source = channels[from];
                    long limit = Math.min(requested, source.capacity(fluid));
                    topology.pipe(from).transferToPipe(source, channels[to], limit);
                });
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
