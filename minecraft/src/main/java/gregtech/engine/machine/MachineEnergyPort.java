package gregtech.engine.machine;

import gregtech.content.data.GregTechTags;
import gregtech.content.machine.MachineInputAverage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Per-tick energy buffers, auxiliary charge and the machine's energy contract. */
public final class MachineEnergyPort {
    public interface Host {
        @Nullable
        BasicMachineSpec spec();

        @Nullable
        GregTechTags.Tag auxiliaryTag();

        Level getLevel();

        long energyInputMaximum();

        boolean usesAuxiliaryInput(GregTechTags.Tag type, @Nullable Direction side);

        boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical);

        void markEnergyDataChanged();

        void overloadEnergy(long size, GregTechTags.Tag type);
    }

    private long stored;
    private long auxStored;
    private long startup;
    private final MachineInputAverage inputAverage = new MachineInputAverage();
    private long lastOverloadTick = Long.MIN_VALUE;
    private long lastInsufficientTick = Long.MIN_VALUE;

    public long stored() {
        return stored;
    }

    public void setStored(long value) {
        stored = value;
    }

    public void clearStored() {
        stored = 0;
    }

    public long addPackets(long capacity, long size, long packets) {
        stored = BasicMachineEnergy.add(stored, capacity, size, packets);
        return stored;
    }

    public long accepted(long size, long requested, long capacity) {
        return BasicMachineEnergy.accepted(stored, capacity, size, requested);
    }

    public long addReceived(long size, long packets, long capacity, @Nullable Level level) {
        long before = stored;
        stored = BasicMachineEnergy.add(stored, capacity, size, packets);
        if (level != null) inputAverage.record(level.getGameTime(), Math.max(0, stored - before));
        return stored;
    }

    public long drain(long ratedMaximum) {
        stored = BasicMachineEnergy.drain(stored, ratedMaximum);
        return stored;
    }

    public double average(@Nullable Level level) {
        return level == null ? 0 : inputAverage.average(level.getGameTime());
    }

    /** Rejected packets do not enter the input average, but must remain visible during inspection. */
    public void recordOverload(@Nullable Level level) {
        if (level != null) lastOverloadTick = level.getGameTime();
    }

    public boolean overloaded(@Nullable Level level) {
        if (level == null || lastOverloadTick == Long.MIN_VALUE) return false;
        long elapsed = level.getGameTime() - lastOverloadTick;
        return elapsed >= 0 && elapsed < 20;
    }

    public void recordInsufficient(@Nullable Level level) {
        if (level != null) lastInsufficientTick = level.getGameTime();
    }

    public boolean insufficient(@Nullable Level level) {
        if (level == null || lastInsufficientTick == Long.MIN_VALUE) return false;
        long elapsed = level.getGameTime() - lastInsufficientTick;
        return elapsed >= 0 && elapsed < 20;
    }

    public long auxStored() {
        return auxStored;
    }

    public void setAuxStored(long value) {
        auxStored = value;
    }

    public long auxAccepted(long size, long requested, long capacity) {
        return BasicMachineEnergy.accepted(auxStored, capacity, size, requested);
    }

    public long addAux(long size, long packets, long capacity) {
        auxStored = BasicMachineEnergy.add(auxStored, capacity, size, packets);
        return auxStored;
    }

    public void payStartup() {
        var paid = BasicMachineEnergy.payStartup(auxStored, startup);
        auxStored = paid.auxStored();
        startup = paid.startup();
    }

    public long startup() {
        return startup;
    }

    public void setStartup(long value) {
        startup = value;
    }

    public void clearStartup() {
        startup = 0;
    }

    public boolean isEnergyType(Host host, GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        BasicMachineSpec spec = host.spec();
        if (energyType == null || spec == null) return false;
        return spec.energyType().equalsIgnoreCase(energyType.getShortName())
                || (host.auxiliaryTag() != null && host.auxiliaryTag() == energyType);
    }

    public Collection<GregTechTags.Tag> getEnergyTypes(Host host, @Nullable Direction side) {
        BasicMachineSpec spec = host.spec();
        if (spec == null) return Collections.emptyList();
        var tags = new ArrayList<GregTechTags.Tag>(2);
        GregTechTags.Tag tag = spec.energyTag();
        if (tag != null) tags.add(tag);
        GregTechTags.Tag aux = host.auxiliaryTag();
        if (aux != null && !tags.contains(aux)) tags.add(aux);
        return tags;
    }

    public boolean isEnergyEmittingTo(
            Host host, GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }

    public long doInject(
            Host host, GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean execute) {
        if (size == 0 || size == Long.MIN_VALUE || amount <= 0 || !host.isEnergyAcceptingFrom(energyType, side, false))
            return 0;
        long sizeAbs = Math.abs(size);
        if (host.usesAuxiliaryInput(energyType, side)) {
            BasicMachineSpec spec = host.spec();
            long capacity = spec.auxiliaryEnergyCapacity();
            if (sizeAbs < spec.auxiliaryEnergyMin()) return 0;
            if (sizeAbs > spec.auxiliaryEnergyMax()) {
                if (execute) host.overloadEnergy(sizeAbs, energyType);
                return amount;
            }
            long consumed = BasicMachineEnergy.accepted(auxStored, capacity, size, amount);
            if (execute && consumed > 0) {
                auxStored = BasicMachineEnergy.add(auxStored, capacity, size, consumed);
                payStartup();
                host.markEnergyDataChanged();
            }
            return consumed;
        }
        if (sizeAbs > host.energyInputMaximum()) {
            // GT6 consumes oversized packets without crediting them; damage is opt-in.
            if (execute) host.overloadEnergy(sizeAbs, energyType);
            return amount;
        }
        long consumed = BasicMachineEnergy.accepted(stored, host.energyInputMaximum(), size, amount);
        if (execute && consumed > 0) {
            addReceived(size, consumed, host.energyInputMaximum(), host.getLevel());
            host.markEnergyDataChanged();
        }
        return consumed;
    }

    public long doExtract(
            Host host, GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean execute) {
        return 0;
    }

    public long getEnergyDemanded(Host host, GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (size == 0 || size == Long.MIN_VALUE || !host.isEnergyAcceptingFrom(energyType, side, false)) return 0;
        if (host.usesAuxiliaryInput(energyType, side))
            return BasicMachineEnergy.demanded(auxStored, host.spec().auxiliaryEnergyCapacity(), size);
        return BasicMachineEnergy.demanded(stored, host.energyInputMaximum(), size);
    }

    public long getEnergyOffered(Host host, GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    public long getEnergySizeInputRecommended(Host host, GregTechTags.Tag energyType, @Nullable Direction side) {
        if (host.usesAuxiliaryInput(energyType, side)) return host.spec().auxiliaryEnergyIn();
        return host.spec() != null ? host.spec().energyIn() : 1;
    }

    public long getEnergySizeInputMin(Host host, GregTechTags.Tag energyType, @Nullable Direction side) {
        if (host.usesAuxiliaryInput(energyType, side)) return host.spec().auxiliaryEnergyMin();
        return host.spec() != null ? host.spec().energyInMin() : 1;
    }

    public long getEnergySizeInputMax(Host host, GregTechTags.Tag energyType, @Nullable Direction side) {
        if (host.usesAuxiliaryInput(energyType, side)) return host.spec().auxiliaryEnergyMax();
        return host.spec() != null ? host.energyInputMaximum() : 1;
    }

    public long getEnergySizeOutputRecommended(Host host, GregTechTags.Tag energyType, @Nullable Direction side) {
        return 1;
    }

    public long getEnergySizeOutputMin(Host host, GregTechTags.Tag energyType, @Nullable Direction side) {
        return 1;
    }

    public long getEnergySizeOutputMax(Host host, GregTechTags.Tag energyType, @Nullable Direction side) {
        return 1;
    }
}
