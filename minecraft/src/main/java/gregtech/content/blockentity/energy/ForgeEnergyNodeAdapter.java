package gregtech.content.blockentity.energy;

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
        return !host.isRemoved() && host.behavior().exposesRfInput(side);
    }

//? if >=1.21.1 {
    /*IEnergyStorage capability(@Nullable Direction side) {
        return side == null ? unsided : new Port(side);
    }
*///?} else {
    LazyOptional<IEnergyStorage> capability(@Nullable Direction side) {
        if (side != null) {
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
            var device = host.behavior();
            return maxReceive > 0 && canReceive(device) ? device.receiveRf(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return exposes(side) ? (int) Math.min(Integer.MAX_VALUE, host.bufferedInput) : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return exposes(side) ? (int) Math.min(Integer.MAX_VALUE, host.capacity()) : 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return canReceive(host.behavior());
        }

        private boolean canReceive(EnergyDeviceBehavior device) {
            return !host.isRemoved() && device.exposesRfInput(side) && device.canReceiveRf(side);
        }
    }
}
