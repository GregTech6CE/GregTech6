package gregtech.content.blockentity.machine;

import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.transport.fluid.FluidPipeNetwork;
import gregtech.content.transport.fluid.FluidPipeNetworks;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.stack.StackCompat;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}
import org.jetbrains.annotations.Nullable;

/** Shared endpoint IO over the snapshot's ordered members and their native tanks. */
final class SharedFluidPool {
    private static final Direction[] DIRECTIONS = Direction.values();

    private SharedFluidPool() {}

    static @Nullable FluidPipeNetwork networkFrom(FluidPipeBlockEntity source) {
        FluidPipeNetwork network = FluidPipeNetworks.networkOf(source);
        return network == null || hasPressureValve(source) ? null : network;
    }

    static int fill(FluidPipeNetwork network, FluidStack resource, FluidAction action) {
        network.beginPass();
        FluidStack remaining = resource.copy();
        for (int index = nextMember(network, 0); index >= 0; index = nextMember(network, index + 1)) {
            FluidPipeBlockEntity pipe = network.pipe(index);
            int channel = pipe.fillableChannel(remaining);
            if (channel < 0) continue;
            int filled = pipe.channelTanks()[channel].fill(remaining, action);
            if (filled == 0) continue;
            if (filled == remaining.getAmount()) return resource.getAmount();
            remaining.setAmount(remaining.getAmount() - filled);
        }
        return resource.getAmount() - remaining.getAmount();
    }

    /** An empty request means to choose the first drainable fluid and retain its full identity. */
    static FluidStack drain(FluidPipeNetwork network, FluidStack resource, int amount, FluidAction action) {
        if (amount <= 0) return FluidStack.EMPTY;
        network.beginPass();
        boolean hasRequestedFluid = !resource.isEmpty();
        FluidStack request = resource.copy();
        FluidStack result = FluidStack.EMPTY;
        int remaining = amount;
        for (int index = nextMember(network, 0); index >= 0; index = nextMember(network, index + 1)) {
            for (FluidTankGT tank : network.pipe(index).channelTanks()) {
                if (tank.isEmpty()) continue;
                FluidStack drained;
                if (hasRequestedFluid) {
                    if (!StackCompat.sameFluid(tank.getFluidLong(), request)) continue;
                    request.setAmount(remaining);
                    drained = tank.drain(request, action);
                } else {
                    if (!result.isEmpty() && !tank.contains(result)) continue;
                    drained = tank.drain(remaining, action);
                }
                if (drained.isEmpty()) continue;
                if (result.isEmpty()) result = drained;
                else result.grow(drained.getAmount());
                remaining -= drained.getAmount();
                if (remaining == 0) return result;
            }
        }
        return result;
    }

    /** The filter sees the first stored stack in the same member and channel order as the pool. */
    static FluidStack firstDrainCandidate(FluidPipeBlockEntity source, @Nullable FluidPipeNetwork network) {
        if (network != null) {
            network.beginPass();
            for (int index = nextMember(network, 0); index >= 0; index = nextMember(network, index + 1)) {
                FluidStack candidate = firstStoredFluid(network.pipe(index));
                if (!candidate.isEmpty()) return candidate;
            }
        }
        return firstStoredFluid(source);
    }

    private static FluidStack firstStoredFluid(FluidPipeBlockEntity pipe) {
        for (FluidTankGT tank : pipe.channelTanks()) {
            if (!tank.isEmpty()) return tank.getFluid();
        }
        return FluidStack.EMPTY;
    }

    private static int nextMember(FluidPipeNetwork network, int from) {
        for (int index = from; index < network.size() && network.live(index); index++) {
            if (!hasPressureValve(network.pipe(index))) return index;
        }
        return -1;
    }

    /** A pressure valve operates on its host's own tank. */
    private static boolean hasPressureValve(FluidPipeBlockEntity pipe) {
        if (!pipe.hasCovers()) return false;
        for (Direction side : DIRECTIONS) {
            if (CoverUtilityBehaviors.PRESSURE_VALVE.equals(CoverItems.behavior(pipe.getCover(side)))) return true;
        }
        return false;
    }
}
