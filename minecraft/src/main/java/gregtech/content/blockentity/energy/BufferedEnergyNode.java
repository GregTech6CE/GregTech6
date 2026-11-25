package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.GregTechTags.Tag;
import gregtech.engine.energy.EnergyPackets;
import gregtech.engine.energy.EnergyTransfer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Stores complete input packets and pays their input cost when a receiver accepts the output. */
abstract class BufferedEnergyNode extends EnergyDeviceBehavior implements MachineControl {
    BufferedEnergyNode(EnergyNodeBlockEntity host) {
        super(host);
    }

    @Override
    void tickCoversBeforeDevice(Level level) {
        host.tickComponentCovers(level);
    }

    @Override
    void tick(Level level, BlockPos pos, BlockState state) {
        if (host.getLevel() != null && !host.converterStopped) emitOutput();
        host.syncBufferedInput(level, pos, state);
    }

    long availableOutputPackets() {
        long cost = host.inputCostPerOutputPacket();
        return cost > 0 && outputRate() > 0 ? host.bufferedInput / cost : 0;
    }

    void emitOutput() {
        long offered = availableOutputPackets();
        if (offered <= 0) return;
        if (host.spec().outType() == GregTechTags.Energy.RF) {
            host.forgeEnergy.push();
            return;
        }
        long accepted =
                EnergyTransfer.emitEnergyToSide(host.spec().outType(), host.facing(), outputRate(), offered, host);
        if (accepted > 0) {
            host.conversionMeter().output(host.getLevel().getGameTime(), outputRate(), accepted);
            host.bufferedInput -= accepted * host.inputCostPerOutputPacket();
            host.setChanged();
        }
    }

    boolean acceptsPacket(Tag type, @Nullable Direction side) {
        return acceptsInputType(type);
    }

    boolean overloads(Tag type, @Nullable Direction side, long magnitude) {
        return false;
    }

    void recordInput(long size) {}

    @Override
    long demanded(Tag type, @Nullable Direction side, long size) {
        long magnitude = EnergyPackets.magnitude(size, type == GregTechTags.Energy.RU);
        if (magnitude == 0 || !acceptsPacket(type, side)) return 0;
        return Math.max(0, capacity() - host.bufferedInput) / magnitude;
    }

    @Override
    long inject(Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        long magnitude = EnergyPackets.magnitude(size, type == GregTechTags.Energy.RU);
        if (amount <= 0 || magnitude == 0 || !acceptsPacket(type, side)) return 0;
        if (overloads(type, side, magnitude)) {
            if (execute) {
                if (host.getLevel() != null)
                    host.conversionMeter().input(host.getLevel().getGameTime(), magnitude, amount);
                host.handleInputOverload(magnitude);
            }
            return amount;
        }
        long freeCapacity = Math.max(0, capacity() - host.bufferedInput);
        long accepted = EnergyPackets.fitting(amount, freeCapacity, magnitude);
        if (execute && accepted > 0) {
            recordInput(size);
            long energy = accepted * magnitude;
            host.bufferedInput += energy;
            if (host.getLevel() != null)
                host.conversionMeter().input(host.getLevel().getGameTime(), energy);
            host.setChanged();
        }
        return accepted;
    }

    @Override
    MachineControl control() {
        return this;
    }

    @Override
    public boolean available() {
        return isCurrent();
    }

    @Override
    public boolean supportsMode() {
        return false;
    }

    @Override
    public boolean enabled() {
        return !host.converterStopped;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        if (!available()) return false;
        host.converterStopped = !enabled;
        host.setChanged();
        return enabled;
    }

    @Override
    public boolean running() {
        return enabled() && host.stored() > 0;
    }

    @Override
    public boolean active() {
        return running();
    }

    @Override
    public long progress() {
        return host.stored();
    }

    @Override
    public long progressMax() {
        return host.capacity();
    }

    /** Generic converters and plain storage can also supply a pull request. */
    static final class Pullable extends BufferedEnergyNode {
        Pullable(EnergyNodeBlockEntity host) {
            super(host);
        }

        @Override
        long offered(Tag type, @Nullable Direction side, long size) {
            if (!acceptsOutputType(type) || size <= 0) return 0;
            long available = availableOutputPackets();
            return Math.min(available, available * outputRate() / size);
        }

        @Override
        long extract(Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
            if (!acceptsOutputType(type) || amount <= 0 || size <= 0) return 0;
            long available = availableOutputPackets();
            if (available <= 0) return 0;
            long requested = amount * size / outputRate();
            long outputPackets = Math.min(available, Math.max(0, requested));
            long extracted = outputPackets * outputRate() / size;
            if (execute && extracted > 0) {
                if (host.getLevel() != null)
                    host.conversionMeter().output(host.getLevel().getGameTime(), size, extracted);
                host.bufferedInput -= outputPackets * host.inputCostPerOutputPacket();
                host.setChanged();
            }
            return extracted;
        }
    }

    /** Older custom turbine specs can receive energy without the original steam conversion cycle. */
    static final class Turbine extends BufferedEnergyNode {
        private final SteamTurbineNode turbine;

        Turbine(EnergyNodeBlockEntity host, long inputRate) {
            super(host);
            turbine = new SteamTurbineNode(host, inputRate);
        }

        @Override
        SteamTurbineNode turbine() {
            return turbine;
        }

        @Override
        long capacity() {
            return host.spec().inputRate() * 2;
        }

        @Override
        boolean acceptsFrom(@Nullable Direction side, boolean theoretical) {
            return !host.converterStopped
                    && (side == null || side == host.facing().getOpposite());
        }

        @Override
        long inject(Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
            return turbine.inject(type, side, size, amount, execute);
        }

        @Override
        MachineControl control() {
            return turbine;
        }
    }

    /** Older custom magnets use the buffered transfer cycle with two output faces. */
    static final class Magnet extends BufferedEnergyNode {
        Magnet(EnergyNodeBlockEntity host) {
            super(host);
        }

        @Override
        void saveState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
            super.saveState(tag, lookup);
            MagnetNode.saveLegacyState(host, tag, mode());
        }

        @Override
        void loadState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
            super.loadState(tag, lookup);
            MagnetNode.loadLegacyState(host, tag);
        }

        @Override
        boolean acceptsPacket(Tag type, @Nullable Direction side) {
            return host.isEnergyAcceptingFrom(type, side, false);
        }

        @Override
        boolean overloads(Tag type, @Nullable Direction side, long magnitude) {
            return magnitude > host.getEnergySizeInputMax(type, side);
        }

        @Override
        boolean emitsTo(Tag type, @Nullable Direction side, boolean theoretical) {
            return side == null || side.getAxis() == host.facing().getAxis();
        }
    }
}
