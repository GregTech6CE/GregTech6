package gregtech.content.blockentity.energy;

import gregtech.content.data.GregTechTags;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
*///?} else {
import java.util.EnumMap;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
//?}

/** Bridges GT packet transfers to the adjacent Forge Energy storage or an exposed RF input port. */
final class ForgeEnergyNodeAdapter {
    private final EnergyNodeBlockEntity host;
    private final Port unsided = new Port(null);
//? if <1.21.1 {
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedCapabilities = new EnumMap<>(Direction.class);
    private LazyOptional<IEnergyStorage> unsidedCapability;
//?}

    ForgeEnergyNodeAdapter(EnergyNodeBlockEntity host) {
        this.host = host;
    }

    boolean exposes(@Nullable Direction side) {
        if (host.spec() == null || host.spec().inType() != GregTechTags.Energy.RF) return false;
        if (side == null) return true;
        if (side == host.facing()) return false;
        boolean hasRearOutput = host.isMagnet() || host.isOriginalCooler();
        return !hasRearOutput || side != host.facing().getOpposite();
    }

//? if >=1.21.1 {
    /*IEnergyStorage capability(@Nullable Direction side) {
        return (host.isMagnet() || host.hasOriginalConverter()) && side != null ? new Port(side) : unsided;
    }
*///?} else {
    LazyOptional<IEnergyStorage> capability(@Nullable Direction side) {
        if ((host.isMagnet() || host.hasOriginalConverter()) && side != null) {
            return sidedCapabilities.computeIfAbsent(side, face -> LazyOptional.of(() -> new Port(face)));
        }
        if (unsidedCapability == null || !unsidedCapability.isPresent()) {
            unsidedCapability = LazyOptional.of(() -> unsided);
        }
        return unsidedCapability;
    }

    void invalidateCaps() {
        if (unsidedCapability != null) {
            unsidedCapability.invalidate();
            unsidedCapability = null;
        }
        sidedCapabilities.values().forEach(LazyOptional::invalidate);
        sidedCapabilities.clear();
    }
//?}

    void push() {
        var level = host.getLevel();
        var neighbor = level.getBlockEntity(host.getBlockPos().relative(host.facing()));
        if (neighbor == null) return;
//? if >=1.21.1 {
        /*var storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, neighbor.getBlockPos(), host.facing().getOpposite());
*///?} else {
        var storage = neighbor.getCapability(ForgeCapabilities.ENERGY, host.facing().getOpposite())
                .resolve()
                .orElse(null);
//?}
        if (storage == null || !storage.canReceive()) return;

        long inputCost = host.inputCostPerOutputPacket();
        int packetSize = (int) host.effectiveOutputRate();
        for (int packet = 0; packet < 64 && host.bufferedInput >= inputCost; packet++) {
            if (storage.receiveEnergy(packetSize, true) < packetSize) break;
            storage.receiveEnergy(packetSize, false);
            host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, 1);
            host.bufferedInput -= inputCost;
            host.setChanged();
        }
    }

    private final class Port implements IEnergyStorage {
        @Nullable
        private final Direction side;

        Port(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (side != null && !host.isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false)) return 0;
            if (host.hasOriginalConverter()) {
                return (int) host.doEnergyInjection(GregTechTags.Energy.RF, null, 1, maxReceive, !simulate);
            }
            long space = Math.max(0, host.capacity() - host.bufferedInput);
            int accepted = (int) Math.min(maxReceive, space);
            if (!simulate && accepted > 0) {
                host.bufferedInput += accepted;
                var level = host.getLevel();
                if (level != null) host.conversionMeter().input(level.getGameTime(), accepted);
                host.setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, host.bufferedInput);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, host.capacity());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            if (side != null) return host.isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false);
            return !host.hasOriginalConverter() || !host.converterStopped;
        }
    }
}
