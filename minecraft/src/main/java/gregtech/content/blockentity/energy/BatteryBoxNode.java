package gregtech.content.blockentity.energy;

import gregtech.GregTechConfig;
import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.BatteryBoxBlock;
import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.MachineOverload;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Connects the battery box's slot and buffer state to the energy network and the world. */
final class BatteryBoxNode implements MachineControl {
    private final EnergyNodeBlockEntity host;
    private final EnergyNodeSpec spec;
    final BatteryBoxEnergy energy;

    BatteryBoxNode(EnergyNodeBlockEntity host, EnergyNodeSpec spec, BatteryBoxInventory inventory) {
        this.host = host;
        this.spec = spec;
        energy = new BatteryBoxEnergy(inventory.reset(spec.batterySlots()), spec.inputRate(), spec.inType());
    }

    void tick(Level level, BlockPos pos, BlockState state) {
        long time = level.getGameTime();
        host.panels().beforeTick();
        for (Direction side : Direction.values()) {
            ComponentCoverRuntime.tick(host, side, time);
        }
        energy.tick(time, level, pos);
        long offered = energy.offered();
        if (offered > 0) {
            long sent = EnergyTransfer.emitEnergyToSide(spec.outType(), host.facing(), host.outRate(), offered, host);
            energy.emitted(sent);
        }
        host.panels().afterTick();
        host.setChanged();
        if (time % 20 == 0) {
            int charge = energy.displayState();
            if (state.hasProperty(BatteryBoxBlock.CHARGE_STATE)
                    && state.getValue(BatteryBoxBlock.CHARGE_STATE) != charge) {
                level.setBlock(pos, state.setValue(BatteryBoxBlock.CHARGE_STATE, charge), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    boolean emitsTo(@Nullable Direction side, boolean theoretical) {
        return (theoretical || energy.enabled()) && (side == null || side == host.facing());
    }

    long demanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        if (size == Long.MIN_VALUE || !host.isEnergyAcceptingFrom(type, side, false)) return 0;
        return energy.inject(Math.abs(size), Long.MAX_VALUE, false);
    }

    long inject(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        if (size == Long.MIN_VALUE || size == 0 || amount <= 0
                || !host.isEnergyAcceptingFrom(type, side, false) || !energy.canReceive()) return 0;
        long packet = Math.abs(size);
        if (packet > host.getEnergySizeInputMax(type, side)) {
            if (execute) overload(packet);
            return amount;
        }
        long used = energy.inject(packet, amount, execute);
        if (execute && used > 0) {
            Level level = host.getLevel();
            if (level != null) host.conversionMeter().input(level.getGameTime(), packet, used);
            host.setChanged();
        }
        return used;
    }

    private void overload(long packet) {
        Level level = host.getLevel();
        if (level == null || level.isClientSide) return;
        if (GregTechConfig.breakByOverload()) host.dropContents();
        MachineOverload.apply(host, packet, spec.inType());
    }

    long stored(GregTechTags.Tag type) {
        return type == spec.inType() ? energy.stored() : 0;
    }

    long capacity(GregTechTags.Tag type) {
        return type == spec.inType() ? energy.capacity() : 0;
    }

    @Override
    public boolean supportsMode() {
        return true;
    }

    @Override
    public int mode() {
        return energy.mode();
    }

    @Override
    public int setMode(int mode) {
        energy.mode(mode);
        host.setChanged();
        return energy.mode();
    }

    @Override
    public boolean enabled() {
        return energy.enabled();
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        energy.enabled(enabled);
        host.setChanged();
        return enabled;
    }

    @Override
    public boolean running() {
        return energy.running();
    }

    @Override
    public boolean active() {
        return energy.emitted();
    }

    @Override
    public long progress() {
        return energy.buffer();
    }

    @Override
    public long progressMax() {
        return energy.bufferCapacity();
    }
}
