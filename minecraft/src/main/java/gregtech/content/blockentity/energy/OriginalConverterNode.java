package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.block.energy.OriginalMotorBlock;
import gregtech.content.block.energy.RotaryConverterBlock;
import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.MagnetMachineDefinitions;
import gregtech.content.energy.OriginalAdjacentEnergyRules;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.content.energy.OriginalSteamTurbines;
import gregtech.content.energy.OriginalThermalConverter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.machine.AdjacentEnergyControl;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

/** World-facing behavior for devices that share the original GT6 conversion cycle. */
final class OriginalConverterNode implements MachineControl {
    private final EnergyNodeBlockEntity host;

    @Nullable
    private OriginalRotaryConverter.State state;

    OriginalConverterNode(EnergyNodeBlockEntity host) {
        this.host = host;
    }

    void setSpec(EnergyNodeSpec spec) {
        boolean usesConverterState = OriginalRotaryConverter.handles(spec)
                || OriginalThermalConverter.handles(spec)
                || MagnetMachineDefinitions.handles(spec)
                || OriginalSteamTurbines.handles(spec);
        state = usesConverterState ? new OriginalRotaryConverter.State(spec) : null;
    }

    boolean hasState() {
        return state != null;
    }

    boolean isRotary() {
        return host.hasOriginalConverter() && OriginalRotaryConverter.handles(host.spec());
    }

    boolean isThermal() {
        return host.hasOriginalConverter() && OriginalThermalConverter.handles(host.spec());
    }

    boolean isMotor() {
        return host.hasOriginalConverter() && OriginalRotaryConverter.motorBehavior(host.spec());
    }

    boolean isCooler() {
        return host.isOriginalThermalConverter() && OriginalThermalConverter.cooler(host.spec());
    }

    boolean reverseMotor() {
        host.bufferedInput = 0;
        boolean counterClockwise = state.reverse();
        host.setChanged();
        return counterClockwise;
    }

    boolean counterClockwise() {
        return host.isOriginalMotor() && state.counterClockwise();
    }

    boolean emitted() {
        return state != null && state.emitted();
    }

    boolean fast() {
        return state != null && state.fast();
    }

    boolean acceptsInputFrom(@Nullable Direction side, boolean theoretical) {
        if (!theoretical && host.converterStopped) return false;
        if (side == null) return true;
        Direction front = host.facing();
        if (host.isMagnet()) return side != front && side != front.getOpposite();
        if (host.isOriginalThermalConverter()) {
            return side != front && (!OriginalThermalConverter.cooler(host.spec()) || side != front.getOpposite());
        }
        if (host.isOriginalMotor() && !host.isTurbine()) return side != front;
        return side == front.getOpposite();
    }

    boolean emitsCoolerTo(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (energyType == host.spec().outType()) return side == null || side == host.facing();
        return energyType == GregTechTags.Energy.HU
                && (side == null || side == host.facing().getOpposite());
    }

    long inject(
            GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount, boolean execute) {
        if (!host.isEnergyAcceptingFrom(energyType, side, false)) return 0;
        var injection = state.inject(host.bufferedInput, packetSize, packetCount, execute);
        if (execute) {
            if (host.getLevel() != null) {
                host.conversionMeter().input(host.getLevel().getGameTime(), packetSize, injection.consumed());
            }
            host.bufferedInput = injection.energy();
            if (injection.overloaded()) host.handleConverterOverload(Math.abs(packetSize), energyType);
            if (injection.consumed() > 0) host.setChanged();
        }
        return injection.consumed();
    }

    void tick() {
        host.panels().beforeTick();
        for (Direction side : Direction.values()) {
            ComponentCoverRuntime.tick(host, side, host.getLevel().getGameTime());
        }
        if (host.isTurbine()) host.turbine.tick();

        long bufferBeforeConversion = host.bufferedInput;
        var conversion = state.tick(host.bufferedInput, host.converterStopped, this::emit);
        host.bufferedInput = conversion.energy();
        if (conversion.overloaded()) {
            host.handleConverterOverload(bufferBeforeConversion, host.spec().outType());
        }
        var level = host.getLevel();
        var pos = host.getBlockPos();
        if (host.isRemoved() || level.getBlockEntity(pos) != host) return;
        host.coolDownOverload(conversion.possible());

        var blockState = host.getBlockState();
        // Magnet activity levels 1 and 2 share the same overlay.
        var nextState = host.isMagnet()
                ? blockState.setValue(MagnetMachineBlock.ACTIVE, conversion.visual() > 0)
                : blockState.setValue(RotaryConverterBlock.ACTIVITY, conversion.visual());
        if (host.isOriginalMotor()) {
            nextState = nextState
                    .setValue(OriginalMotorBlock.COUNTER_CLOCKWISE, state.counterClockwise())
                    .setValue(OriginalMotorBlock.FAST, conversion.fast());
        }
        if (nextState != blockState) {
            level.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
        } else {
            host.syncBufferedInput(level, pos, blockState);
        }
        host.panels().afterTick();
        host.setChanged();
    }

    private long emit(long packetSize, long offeredPackets) {
        if (host.isMagnet()) {
            long positivePackets = EnergyTransfer.emitEnergyToSide(
                    host.spec().outType(), host.facing(), packetSize, offeredPackets, host);
            long negativePackets = EnergyTransfer.emitEnergyToSide(
                    host.spec().outType(), host.facing().getOpposite(), -packetSize, offeredPackets, host);
            long acceptedPackets = positivePackets + negativePackets;
            host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
            return acceptedPackets;
        }
        if (isCooler()) {
            long coldPackets = EnergyTransfer.emitEnergyToSide(
                    host.spec().outType(), host.facing(), packetSize, offeredPackets, host);
            long heatPackets = EnergyTransfer.emitEnergyToSide(
                    GregTechTags.Energy.HU, host.facing().getOpposite(), packetSize, offeredPackets, host);
            host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, coldPackets);
            host.conversionMeter().auxiliaryOutput(host.getLevel().getGameTime(), packetSize, heatPackets);
            return coldPackets > 0 || heatPackets > 0 ? 1 : 0;
        }

        long acceptedPackets =
                EnergyTransfer.emitEnergyToNetwork(host.spec().outType(), packetSize, offeredPackets, host);
        host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
        return acceptedPackets;
    }

    @Nullable
    AdjacentEnergyControl adjacentEnergyControl() {
        if (!host.hasOriginalConverter() || !OriginalAdjacentEnergyRules.respondsToAdjacent(host.spec())) return null;
        return enabled -> {
            // Manual and adjacent requests share the stopped state. Stored energy still drains normally.
            if (!host.isRemoved()
                    && (host.getLevel() == null || !host.getLevel().isClientSide)
                    && host.converterStopped == enabled) {
                host.converterStopped = !enabled;
                host.setChanged();
            }
            return !host.converterStopped;
        };
    }

    @Override
    public boolean available() {
        return !host.isRemoved();
    }

    @Override
    public boolean supportsProgress() {
        return false;
    }

    @Override
    public boolean supportsMode() {
        return host.isOriginalMotor() || host.isMagnet() || OriginalThermalConverter.modeSelectable(host.spec());
    }

    @Override
    public int mode() {
        return state.mode();
    }

    @Override
    public int setMode(int value) {
        if (supportsMode()) state.mode(value);
        host.setChanged();
        return mode();
    }

    @Override
    public boolean enabled() {
        return !host.converterStopped;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        host.converterStopped = !enabled;
        host.setChanged();
        return enabled;
    }

    @Override
    public boolean running() {
        return state.possible();
    }

    @Override
    public boolean active() {
        return state.emitted();
    }

    @Override
    public long progress() {
        return 0;
    }

    @Override
    public long progressMax() {
        return 0;
    }

    void save(CompoundTag tag) {
        if (!host.hasOriginalConverter()) return;
        var snapshot = state.snapshot();
        tag.putByte("gt.mode", (byte) snapshot.mode());
        if (host.isOriginalMotor()) tag.putBoolean("gt.reversed", snapshot.counterClockwise());
        tag.putBoolean("gt.active.energy", snapshot.emitted());
        tag.putBoolean("gt.can.energy", snapshot.possible());
        if (host.isOriginalMotor()) tag.putBoolean("gt.visual", snapshot.fast());
    }

    void load(CompoundTag tag, boolean usesLegacyMagnetKeys) {
        if (!host.hasOriginalConverter()) return;
        state.restore(new OriginalRotaryConverter.Snapshot(
                tag.getByte(usesLegacyMagnetKeys ? "gt.magnet_mode" : "gt.mode"),
                host.isOriginalMotor() && tag.getBoolean("gt.reversed"),
                false,
                tag.getBoolean("gt.can.energy"),
                tag.getBoolean("gt.active.energy"),
                host.isOriginalMotor() && tag.getBoolean("gt.visual")));
    }
}
